package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.service.SysMenuService;
import me.north30.erp.system.role.converter.RoleConverter;
import me.north30.erp.system.role.dto.RoleAssignMenuDTO;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeItemDTO;
import me.north30.erp.system.role.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysRoleDataScopeMapper;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.service.RoleManageService;
import me.north30.erp.system.role.service.SysRoleService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.role.strategy.RoleAssembleStrategy;
import me.north30.erp.system.role.strategy.RoleGrantStrategy;
import me.north30.erp.system.role.strategy.RoleQueryStrategy;
import me.north30.erp.system.role.vo.RoleCreatedVO;
import me.north30.erp.system.role.vo.RoleDataScopeItemVO;
import me.north30.erp.system.role.vo.RoleDataScopeVO;
import me.north30.erp.system.role.vo.RoleMenuAssignedVO;
import me.north30.erp.system.role.vo.RoleUpdatedVO;
import me.north30.erp.system.role.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现（接口文档 5.2）。
 * <p>分页条件/白名单由 {@link RoleQueryStrategy} 承载，userCount 派生按页角色 ID 集合一次
 * IN 查询后内存计数（禁止 N+1）；菜单授权与数据范围全删全插由 {@link RoleGrantStrategy} 承载；
 * 数据范围档位校验由 {@link SysRole#changeDataScope(Integer)} 承载。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleManageServiceImpl implements RoleManageService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleService sysRoleService;
    private final SysUserRoleService sysUserRoleService;
    private final SysMenuService sysMenuService;
    private final SysRoleDataScopeMapper sysRoleDataScopeMapper;
    private final RoleQueryStrategy roleQueryStrategy;
    private final RoleGrantStrategy roleGrantStrategy;
    private final RoleAssembleStrategy roleAssembleStrategy;
    private final RoleConverter roleConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleVO> page(RolePageQueryDTO query) {
        Page<SysRole> page = roleQueryStrategy.buildPage(query);
        sysRoleMapper.selectPage(page, roleQueryStrategy.buildWrapper(query));

        // 派生 userCount：单次 IN 查询后内存分组计数
        List<Long> roleIds = page.getRecords().stream().map(SysRole::getId).toList();
        Map<Long, Long> userCounts = roleIds.isEmpty() ? Map.of()
            : sysUserRoleService.listByRoleIds(roleIds).stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleId, Collectors.counting()));
        List<RoleVO> vos = page.getRecords().stream()
            .map(role -> new RoleVO(role.getId(), role.getRoleCode(), role.getRoleName(),
                role.getRoleSort(), role.getDataScope(), role.getIsBuiltin(), role.getStatus(),
                userCounts.getOrDefault(role.getId(), 0L).intValue()))
            .toList();
        return PageResult.of(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleCreatedVO create(RoleCreateDTO dto) {
        Long exists = sysRoleMapper.selectCount(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.roleCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_CODE_EXISTS,
                "角色编码 " + dto.roleCode() + " 已存在");
        }
        SysRole role = roleConverter.toEntity(dto);
        role.changeDataScope(dto.dataScope() == null ? 1 : dto.dataScope());
        if (role.getRoleSort() == null) {
            role.setRoleSort(0);
        }
        role.setIsBuiltin(0);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleMapper.insert(role);
        return new RoleCreatedVO(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleUpdatedVO update(Long id, RoleUpdateDTO dto) {
        SysRole role = sysRoleService.requireRole(id);
        if (dto.dataScope() != null) {
            role.changeDataScope(dto.dataScope());
        }
        // role_code 不可修改：转换器不映射该字段；null 字段跳过（部分更新语义），
        // 乐观锁按客户端传入 version 校验
        roleConverter.updateEntity(dto, role);
        int rows = sysRoleMapper.updateById(role);
        if (rows == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        SysRole latest = sysRoleMapper.selectById(id);
        return new RoleUpdatedVO(latest != null && latest.getUpdateTime() != null
            ? DateTimeFormatUtil.format(latest.getUpdateTime())
            : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysRole role = sysRoleService.requireRole(id);
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_BUILTIN);
        }
        long userCount = sysUserRoleService.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_REFERENCED_BY_USER,
                "角色 " + role.getRoleName() + " 已分配给 " + userCount + " 个用户，不可删除");
        }
        sysRoleMapper.deleteById(id);
        // 级联逻辑删除授权与数据范围配置，避免已删角色残留引用阻塞菜单删除/范围查询
        roleGrantStrategy.cleanupOnRoleDelete(id);
        return new DeleteResultVO(id, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleMenuAssignedVO assignMenus(Long id, RoleAssignMenuDTO dto) {
        sysRoleService.requireRole(id);
        List<Long> menuIds = dto.menuIds().stream().distinct().toList();
        List<SysMenu> menus = List.of();
        if (!menuIds.isEmpty()) {
            // 单次 IN 查询完成存在性校验与权限点计数，避免循环查库
            menus = sysMenuService.listByIds(menuIds);
            if (menus.size() != menuIds.size()) {
                throw new BusinessException(RoleMenuErrorCode.MENU_NOT_FOUND, "菜单不存在或已删除");
            }
        }
        roleGrantStrategy.replaceRoleMenus(id, menuIds);
        int permCount = (int) menus.stream()
            .filter(menu -> menu.getPerms() != null && !menu.getPerms().isBlank())
            .count();
        return new RoleMenuAssignedVO(menuIds, permCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDataScopeVO saveDataScopes(Long id, RoleDataScopeSaveDTO dto) {
        sysRoleService.requireRole(id);
        for (RoleDataScopeItemDTO item : dto.scopes()) {
            requireScopeValid(item);
        }
        List<SysRoleDataScope> scopes = dto.scopes().stream()
            .map(item -> roleGrantStrategy.buildDataScope(id, item,
                roleAssembleStrategy.joinIds(item.deptIds()), roleAssembleStrategy.joinIds(item.userIds())))
            .toList();
        roleGrantStrategy.replaceDataScopes(id, scopes);
        return listDataScopes(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDataScopeVO listDataScopes(Long id) {
        SysRole role = sysRoleService.requireRole(id);
        List<RoleDataScopeItemVO> scopes = sysRoleDataScopeMapper.selectList(
                new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, id)).stream()
            .map(scope -> new RoleDataScopeItemVO(scope.getBizObject(), scope.getFilterDimension(),
                scope.getScopeType(), roleAssembleStrategy.parseIds(scope.getDeptIds()),
                roleAssembleStrategy.parseIds(scope.getUserIds()), scope.getFieldMask()))
            .toList();
        return new RoleDataScopeVO(role.getDataScope(), scopes);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    /**
     * 校验数据范围明细：档位合法且自定义范围至少勾选组织或人员。
     */
    private void requireScopeValid(RoleDataScopeItemDTO item) {
        if (item.scopeType() == null || !SysRole.VALID_DATA_SCOPES.contains(item.scopeType())) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_DATA_SCOPE_INVALID,
                "数据范围配置非法：scope_type=" + item.scopeType());
        }
        if (item.scopeType() == 9 && roleAssembleStrategy.isEmpty(item.deptIds())
            && roleAssembleStrategy.isEmpty(item.userIds())) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_DATA_SCOPE_INVALID,
                "数据范围配置非法：自定义范围必须至少勾选组织或人员");
        }
    }
}
