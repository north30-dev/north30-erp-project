package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.role.dto.RoleAssignMenuDTO;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeItemDTO;
import me.north30.erp.system.role.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.role.mapper.SysRoleDataScopeMapper;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.mapper.SysRoleMenuMapper;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.role.service.RoleManageService;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.role.vo.RoleCreatedVO;
import me.north30.erp.system.role.vo.RoleDataScopeItemVO;
import me.north30.erp.system.role.vo.RoleDataScopeVO;
import me.north30.erp.system.role.vo.RoleMenuAssignedVO;
import me.north30.erp.system.role.vo.RoleUpdatedVO;
import me.north30.erp.system.role.vo.RoleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现（接口文档 5.2）。
 * <p>分页 userCount 派生：按页角色 ID 集合一次 IN 查询 sys_user_role 后内存计数（禁止 N+1）；
 * 分配菜单/数据范围均为全量覆盖（先逻辑删除旧关联再插入）；写操作显式事务。</p>
 */
@Service
@RequiredArgsConstructor
public class RoleManageServiceImpl implements RoleManageService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 数据范围合法档位：1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义 */
    private static final Set<Integer> VALID_DATA_SCOPES = Set.of(1, 2, 3, 4, 5, 6, 9);

    /** 排序字段白名单（接口文档 1.4：orderBy 必须命中白名单，否则抛 10001） */
    private static final Set<String> ORDER_BY_WHITELIST = Set.of("create_time", "role_code", "role_sort", "role_name");

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleDataScopeMapper sysRoleDataScopeMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleVO> page(RolePageQueryDTO query) {
        int pageNum = query.pageNum() == null ? PageConstants.DEFAULT_PAGE_NUM : query.pageNum();
        int pageSize = query.pageSize() == null ? PageConstants.DEFAULT_PAGE_SIZE : query.pageSize();
        if (pageNum < 1 || pageSize < 1 || pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "分页参数非法：pageNum ≥ 1 且 1 ≤ pageSize ≤ " + PageConstants.MAX_PAGE_SIZE);
        }
        String orderBy = query.orderBy() == null || query.orderBy().isBlank()
            ? "create_time"
            : query.orderBy();
        if (!ORDER_BY_WHITELIST.contains(orderBy)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "排序字段非法：" + orderBy);
        }
        boolean asc;
        if (query.orderDirection() == null || query.orderDirection().isBlank()) {
            asc = false;
        } else if ("ASC".equalsIgnoreCase(query.orderDirection())) {
            asc = true;
        } else if ("DESC".equalsIgnoreCase(query.orderDirection())) {
            asc = false;
        } else {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "排序方向仅支持 ASC/DESC");
        }

        Page<SysRole> page = new Page<>(pageNum, pageSize);
        page.addOrder(asc ? OrderItem.asc(orderBy) : OrderItem.desc(orderBy));
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
            .like(hasText(query.roleCode()), SysRole::getRoleCode, query.roleCode())
            .like(hasText(query.roleName()), SysRole::getRoleName, query.roleName())
            .eq(query.status() != null, SysRole::getStatus, query.status());
        sysRoleMapper.selectPage(page, wrapper);

        // 派生 userCount：单次 IN 查询后内存分组计数
        List<Long> roleIds = page.getRecords().stream().map(SysRole::getId).toList();
        Map<Long, Long> userCounts = roleIds.isEmpty() ? Map.of()
            : sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds)).stream()
                .collect(Collectors.groupingBy(SysUserRole::getRoleId, Collectors.counting()));
        List<RoleVO> vos = page.getRecords().stream()
            .map(role -> new RoleVO(role.getId(), role.getRoleCode(), role.getRoleName(),
                role.getRoleSort(), role.getDataScope(), role.getIsBuiltin(), role.getStatus(),
                userCounts.getOrDefault(role.getId(), 0L).intValue()))
            .toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
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
        Integer dataScope = dto.dataScope() == null ? 1 : dto.dataScope();
        validateDataScope(dataScope);
        SysRole role = new SysRole();
        role.setRoleCode(dto.roleCode());
        role.setRoleName(dto.roleName());
        role.setRoleSort(dto.roleSort() == null ? 0 : dto.roleSort());
        role.setDataScope(dataScope);
        role.setIsBuiltin(0);
        role.setStatus(dto.status() == null ? 1 : dto.status());
        role.setRemark(dto.remark());
        sysRoleMapper.insert(role);
        return new RoleCreatedVO(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleUpdatedVO update(Long id, RoleUpdateDTO dto) {
        SysRole role = requireRole(id);
        if (dto.dataScope() != null) {
            validateDataScope(dto.dataScope());
        }
        if (dto.roleName() != null) {
            role.setRoleName(dto.roleName());
        }
        if (dto.roleSort() != null) {
            role.setRoleSort(dto.roleSort());
        }
        if (dto.dataScope() != null) {
            role.setDataScope(dto.dataScope());
        }
        if (dto.status() != null) {
            role.setStatus(dto.status());
        }
        if (dto.remark() != null) {
            role.setRemark(dto.remark());
        }
        // role_code 不可修改：不映射该字段；乐观锁按客户端传入 version 校验
        role.setVersion(dto.version());
        int rows = sysRoleMapper.updateById(role);
        if (rows == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        SysRole latest = sysRoleMapper.selectById(id);
        return new RoleUpdatedVO(latest != null && latest.getUpdateTime() != null
            ? latest.getUpdateTime().format(DATETIME_FORMATTER)
            : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysRole role = requireRole(id);
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_BUILTIN);
        }
        Long userCount = sysUserRoleMapper.selectCount(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_REFERENCED_BY_USER,
                "角色 " + role.getRoleName() + " 已分配给 " + userCount + " 个用户，不可删除");
        }
        sysRoleMapper.deleteById(id);
        // 级联逻辑删除授权与数据范围配置，避免已删角色残留引用阻塞菜单删除/范围查询
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        sysRoleDataScopeMapper.delete(
            new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, id));
        return new DeleteResultVO(id, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleMenuAssignedVO assignMenus(Long id, RoleAssignMenuDTO dto) {
        requireRole(id);
        List<Long> menuIds = dto.menuIds().stream().distinct().toList();
        List<SysMenu> menus = List.of();
        if (!menuIds.isEmpty()) {
            // 单次 IN 查询完成存在性校验与权限点计数，避免循环查库
            menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds));
            if (menus.size() != menuIds.size()) {
                throw new BusinessException(RoleMenuErrorCode.MENU_NOT_FOUND, "菜单不存在或已删除");
            }
        }
        // 全删全插：先逻辑删除旧关联，再批量插入新授权
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(id);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
        int permCount = (int) menus.stream()
            .filter(menu -> menu.getPerms() != null && !menu.getPerms().isBlank())
            .count();
        return new RoleMenuAssignedVO(menuIds, permCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDataScopeVO saveDataScopes(Long id, RoleDataScopeSaveDTO dto) {
        requireRole(id);
        for (RoleDataScopeItemDTO item : dto.scopes()) {
            if (item.scopeType() == null || !VALID_DATA_SCOPES.contains(item.scopeType())) {
                throw new BusinessException(RoleMenuErrorCode.ROLE_DATA_SCOPE_INVALID,
                    "数据范围配置非法：scope_type=" + item.scopeType());
            }
            if (item.scopeType() == 9 && isEmpty(item.deptIds()) && isEmpty(item.userIds())) {
                throw new BusinessException(RoleMenuErrorCode.ROLE_DATA_SCOPE_INVALID,
                    "数据范围配置非法：自定义范围必须至少勾选组织或人员");
            }
        }
        // 全删全插：先逻辑删除旧配置，再按明细逐条写入
        sysRoleDataScopeMapper.delete(
            new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, id));
        for (RoleDataScopeItemDTO item : dto.scopes()) {
            SysRoleDataScope scope = new SysRoleDataScope();
            scope.setRoleId(id);
            scope.setBizObject(item.bizObject());
            scope.setFilterDimension(item.filterDimension());
            scope.setScopeType(item.scopeType());
            scope.setDeptIds(joinIds(item.deptIds()));
            scope.setUserIds(joinIds(item.userIds()));
            scope.setFieldMask(item.fieldMask() == null ? 0 : item.fieldMask());
            sysRoleDataScopeMapper.insert(scope);
        }
        return listDataScopes(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDataScopeVO listDataScopes(Long id) {
        SysRole role = requireRole(id);
        List<RoleDataScopeItemVO> scopes = sysRoleDataScopeMapper.selectList(
                new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, id)).stream()
            .map(scope -> new RoleDataScopeItemVO(scope.getBizObject(), scope.getFilterDimension(),
                scope.getScopeType(), parseIds(scope.getDeptIds()), parseIds(scope.getUserIds()),
                scope.getFieldMask()))
            .toList();
        return new RoleDataScopeVO(role.getDataScope(), scopes);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    private SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 " + id + " 不存在");
        }
        return role;
    }

    private void validateDataScope(Integer dataScope) {
        if (!VALID_DATA_SCOPES.contains(dataScope)) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_DATA_SCOPE_INVALID,
                "数据范围配置非法：data_scope=" + dataScope);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isEmpty(List<Long> ids) {
        return ids == null || ids.isEmpty();
    }

    private static String joinIds(List<Long> ids) {
        if (isEmpty(ids)) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String id : ids.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                result.add(Long.valueOf(trimmed));
            }
        }
        return result;
    }
}
