package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.QueryGuardUtil;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.service.SysMenuDomainService;
import me.north30.erp.system.role.converter.RoleConverter;
import me.north30.erp.system.role.dto.RoleAssignMenuDTO;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.dto.RoleStatusDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysRoleDataScopeMapper;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.service.SysRoleApplicationService;
import me.north30.erp.system.role.service.SysRoleDomainService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.role.strategy.RoleAssembleStrategy;
import me.north30.erp.system.role.strategy.RoleGrantStrategy;
import me.north30.erp.system.role.strategy.RoleQueryStrategy;
import me.north30.erp.system.role.vo.RoleCreatedVO;
import me.north30.erp.system.role.vo.RoleDataScopeItemVO;
import me.north30.erp.system.role.vo.RoleDataScopeVO;
import me.north30.erp.system.role.vo.RoleMenuAssignedVO;
import me.north30.erp.system.role.vo.RoleMenuIdsVO;
import me.north30.erp.system.role.vo.RoleStatusVO;
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
public class SysRoleApplicationServiceImpl implements SysRoleApplicationService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleDomainService sysRoleDomainService;
    private final SysUserRoleService sysUserRoleService;
    private final SysMenuDomainService sysMenuDomainService;
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
                role.getDataScope(), role.getIsBuiltin(), role.getStatus(),
                role.getRemark(), role.getVersion(), userCounts.getOrDefault(role.getId(), 0L).intValue()))
            .toList();
        return PageResult.of(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleCreatedVO create(RoleCreateDTO dto) {
        // 角色编码全局唯一校验（18015）收敛进领域服务
        sysRoleDomainService.requireRoleCodeAvailable(dto.roleCode());

        SysRole role = roleConverter.createSysRole(dto);
        role.changeDataScope(dto.dataScope() == null ? 1 : dto.dataScope());
        role.applyCreateDefault();
        sysRoleMapper.insert(role);
        return new RoleCreatedVO(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleUpdatedVO update(Long id, RoleUpdateDTO dto) {
        SysRole role = sysRoleDomainService.requireRole(id);
        if (dto.dataScope() != null) {
            role.changeDataScope(dto.dataScope());
        }
        // role_code 不可修改：转换器不映射该字段；null 字段跳过（部分更新语义），
        // 乐观锁按客户端传入 version 校验
        roleConverter.updateSysRole(dto, role);
        UpdateGuardUtil.checkOptimisticLockConflict(sysRoleMapper.updateById(role));
        SysRole latest = sysRoleMapper.selectById(id);
        return new RoleUpdatedVO(latest != null && latest.getUpdateTime() != null
            ? DateTimeFormatUtil.format(latest.getUpdateTime())
            : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleStatusVO changeStatus(Long id, RoleStatusDTO dto) {
        SysRole role = sysRoleDomainService.requireRole(id);
        // 状态值域守卫（10001）与内置角色不可停用（18017）收敛于实体 changeStatus
        role.changeStatus(dto.status());
        SysRole patch = new SysRole();
        patch.setId(id);
        patch.setStatus(role.getStatus());
        patch.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysRoleMapper.updateById(patch));
        return new RoleStatusVO(role.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysRole role = sysRoleDomainService.requireRole(id);
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BusinessException(SystemErrorCode.ROLE_BUILTIN);
        }
        long userCount = sysUserRoleService.countUsersByRoleId(id);
        QueryGuardUtil.requireCountZero(userCount, SystemErrorCode.ROLE_REFERENCED_BY_USER, 
            "角色 " + role.getRoleName() + " 已分配给 " + userCount + " 个用户，不可删除");
        sysRoleMapper.deleteById(id);
        // 级联逻辑删除授权与数据范围配置，避免已删角色残留引用阻塞菜单删除/范围查询
        roleGrantStrategy.cleanupOnRoleDelete(id);
        return new DeleteResultVO(id, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleMenuAssignedVO assignMenus(Long id, RoleAssignMenuDTO dto) {
        SysRole role = sysRoleDomainService.requireRole(id);
        // ADMIN 超管角色不走菜单勾选（权限=代码层全量），权限集修改一律拒绝（08 文档 2.1）
        if (SysRole.ROLE_CODE_ADMIN.equals(role.getRoleCode())) {
            throw new BusinessException(SystemErrorCode.ROLE_BUILTIN,
                "超级管理员角色权限为代码内置全量，不可通过菜单勾选修改");
        }
        List<Long> menuIds = dto.menuIds().stream().distinct().toList();
        List<SysMenu> menus = List.of();
        if (!menuIds.isEmpty()) {
            // 单次 IN 查询完成存在性校验与权限点计数，避免循环查库
            menus = sysMenuDomainService.listByIds(menuIds);
            if (menus.size() != menuIds.size()) {
                throw new BusinessException(SystemErrorCode.MENU_NOT_FOUND, "菜单不存在或已删除");
            }
        }
        roleGrantStrategy.replaceRoleMenus(id, menuIds);
        int permCount = (int) menus.stream()
            .filter(menu -> menu.getPerms() != null && !menu.getPerms().isBlank())
            .count();
        return new RoleMenuAssignedVO(menuIds, permCount);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleMenuIdsVO listRoleMenuIds(Long id) {
        sysRoleDomainService.requireRole(id);
        return new RoleMenuIdsVO(roleGrantStrategy.listMenuIds(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDataScopeVO saveDataScopes(Long id, RoleDataScopeSaveDTO dto) {
        sysRoleDomainService.requireRole(id);
        // 档位值域与自定义范围跨字段校验由 RoleGrantStrategy.buildDataScope 装配点承载
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
        SysRole role = sysRoleDomainService.requireRole(id);
        List<RoleDataScopeItemVO> scopes = sysRoleDataScopeMapper.selectList(
                new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, id)).stream()
            .map(scope -> new RoleDataScopeItemVO(scope.getBizObject(), scope.getFilterDimension(),
                scope.getScopeType(), roleAssembleStrategy.parseIds(scope.getDeptIds()),
                roleAssembleStrategy.parseIds(scope.getUserIds()), scope.getFieldMask()))
            .toList();
        return new RoleDataScopeVO(role.getDataScope(), scopes);
    }
}
