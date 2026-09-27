package me.north30.erp.system.core.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysDept;
import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.entity.SysRole;
import me.north30.erp.system.core.entity.SysRoleMenu;
import me.north30.erp.system.core.entity.SysUser;
import me.north30.erp.system.core.entity.SysUserRole;
import me.north30.erp.system.core.service.SysDeptService;
import me.north30.erp.system.core.service.SysMenuService;
import me.north30.erp.system.core.service.SysRoleMenuService;
import me.north30.erp.system.core.service.SysRoleService;
import me.north30.erp.system.core.service.SysUserRoleService;
import me.north30.erp.system.core.service.SysUserService;
import me.north30.erp.system.core.service.UserAccessService;
import me.north30.erp.system.core.service.dto.UserSecurityData;
import me.north30.erp.system.core.util.MenuTreeUtil;
import me.north30.erp.system.core.vo.MenuTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户访问聚合服务实现：统一收敛认证链的账号、部门、角色、菜单、权限点与数据范围取数编排。
 * <p>admin（is_admin=1）返回全量权限点与全量菜单；普通用户按启用角色并集取数，
 * 全部一次批量查出后内存组装（父级链补齐、组树），杜绝循环查库（N+1）。</p>
 */
@Service
@RequiredArgsConstructor
public class UserAccessServiceImpl implements UserAccessService {

    /** 数据范围档位按"最宽"排序：1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义 */
    private static final List<Integer> DATA_SCOPE_ORDER = List.of(1, 2, 3, 4, 5, 6, 9);

    private final SysUserService sysUserService;
    private final SysDeptService sysDeptService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysMenuService sysMenuService;

    @Override
    @Transactional(readOnly = true)
    public UserSecurityData loadByUserId(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        // 用户关联的角色（一次查出），仅保留启用角色
        List<SysRole> roles = loadEnabledRoles(userId);
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        // 权限点：admin 全量；普通用户按角色并集（role_menu → menu）
        List<String> perms = List.copyOf(loadPerms(isAdmin(user), roles));
        return new UserSecurityData(userId, user.getStatus(), user.getIsAdmin(), roleCodes,
            perms, resolveWidestDataScope(roles));
    }

    @Override
    @Transactional(readOnly = true)
    public SysUser getByUsername(String username) {
        return sysUserService.getByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public SysUser getUserById(Long userId) {
        return sysUserService.getById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime) {
        sysUserService.updateLastLogin(userId, loginIp, loginTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        sysUserService.updateUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = sysDeptService.getById(deptId);
        return dept != null ? dept.getDeptName() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listRoleCodes(Long userId) {
        return loadEnabledRoles(userId).stream()
            .map(SysRole::getRoleCode)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeVO> listMenuTree(Long userId, boolean isAdmin) {
        List<SysMenu> menus;
        if (isAdmin) {
            // admin：全量启用菜单（过滤按钮），内存组树
            menus = sysMenuService.listEnabled().stream()
                .filter(menu -> menu.getMenuType() != null && menu.getMenuType() != 3)
                .toList();
        } else {
            // 与既有菜单取数语义一致：按用户全部角色（不筛角色启用状态）收集可见菜单
            List<Long> roleIds = sysUserRoleService.listByUserId(userId).stream()
                .map(SysUserRole::getRoleId)
                .toList();
            Set<Long> menuIds = new LinkedHashSet<>(sysRoleMenuService.listByRoleIds(roleIds).stream()
                .map(SysRoleMenu::getMenuId)
                .toList());
            // 一次查出全量启用菜单，内存补齐父级链（角色可能只勾选叶子菜单），避免循环查库 N+1
            Map<Long, SysMenu> enabledById = sysMenuService.listEnabled().stream()
                .collect(Collectors.toMap(SysMenu::getId, Function.identity()));
            Set<Long> resultIds = expandWithAncestors(menuIds, enabledById);
            menus = enabledById.values().stream()
                .filter(menu -> resultIds.contains(menu.getId()))
                .filter(menu -> menu.getMenuType() != null && menu.getMenuType() != 3)
                .toList();
        }
        return MenuTreeUtil.buildTree(menus.stream().map(MenuTreeUtil::toVO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> listPerms(Long userId, boolean isAdmin) {
        return loadPerms(isAdmin, isAdmin ? List.of() : loadEnabledRoles(userId));
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    private boolean isAdmin(SysUser user) {
        return user.getIsAdmin() != null && user.getIsAdmin() == 1;
    }

    /**
     * 查询用户启用角色集合（两次批量查询，无循环查库）。
     */
    private List<SysRole> loadEnabledRoles(Long userId) {
        List<Long> roleIds = sysUserRoleService.listByUserId(userId).stream()
            .map(SysUserRole::getRoleId)
            .toList();
        return sysRoleService.listByIds(roleIds).stream()
            .filter(role -> role.getStatus() != null && role.getStatus() == 1)
            .toList();
    }

    /**
     * 加载权限点集合：admin 全量；普通用户按角色并集（角色-菜单关联与菜单权限标识各一次批量查询）。
     */
    private Set<String> loadPerms(boolean isAdmin, List<SysRole> roles) {
        if (isAdmin) {
            return new LinkedHashSet<>(sysMenuService.listAllPerms());
        }
        if (CollectionUtils.isEmpty(roles)) {
            return new LinkedHashSet<>();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = sysRoleMenuService.listByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(roleMenus)) {
            return new LinkedHashSet<>();
        }
        Set<Long> menuIds = new HashSet<>(roleMenus.stream().map(SysRoleMenu::getMenuId).toList());
        return sysMenuService.listEnabledByIds(menuIds).stream()
            .map(SysMenu::getPerms)
            .filter(perms -> perms != null && !perms.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 多角色取最宽数据范围档位（1-全部 > 2 > 3 > 4 > 5 > 6 > 9-自定义；无角色按"仅本人"）。
     */
    private Integer resolveWidestDataScope(List<SysRole> roles) {
        if (roles.isEmpty()) {
            return 6;
        }
        Set<Integer> scopes = roles.stream()
            .map(SysRole::getDataScope)
            .filter(scope -> scope != null)
            .collect(Collectors.toSet());
        return DATA_SCOPE_ORDER.stream()
            .filter(scopes::contains)
            .findFirst()
            .orElse(9);
    }

    /**
     * 内存补齐菜单父级链：从角色勾选菜单出发，沿启用菜单向上收集祖先 ID。
     * <p>父级不在启用集合（停用/不存在）时停止向上追溯，与逐层查库版本语义一致。</p>
     */
    private Set<Long> expandWithAncestors(Set<Long> menuIds, Map<Long, SysMenu> enabledById) {
        Set<Long> result = new HashSet<>(menuIds);
        for (Long menuId : menuIds) {
            SysMenu menu = enabledById.get(menuId);
            while (menu != null) {
                Long parentId = menu.getParentId();
                if (parentId == null || parentId == 0L || !result.add(parentId)) {
                    break;
                }
                menu = enabledById.get(parentId);
            }
        }
        return result;
    }
}
