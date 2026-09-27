package me.north30.erp.system.core.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.entity.SysRole;
import me.north30.erp.system.core.entity.SysRoleMenu;
import me.north30.erp.system.core.entity.SysUser;
import me.north30.erp.system.core.entity.SysUserRole;
import me.north30.erp.system.core.service.ISysMenuService;
import me.north30.erp.system.core.service.ISysRoleMenuService;
import me.north30.erp.system.core.service.ISysRoleService;
import me.north30.erp.system.core.service.ISysUserRoleService;
import me.north30.erp.system.core.service.ISysUserService;
import me.north30.erp.system.core.service.UserSecurityQueryService;
import me.north30.erp.system.core.service.dto.UserSecurityData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户安全数据查询实现。
 * <p>admin（is_admin=1）返回全量权限点；普通用户按角色并集取权限点，全部一次批量查出，无循环查库。</p>
 */
@Service
@RequiredArgsConstructor
public class UserSecurityQueryServiceImpl implements UserSecurityQueryService {

    private final ISysUserService sysUserService;
    private final ISysUserRoleService sysUserRoleService;
    private final ISysRoleService sysRoleService;
    private final ISysRoleMenuService sysRoleMenuService;
    private final ISysMenuService sysMenuService;

    @Override
    @Transactional(readOnly = true)
    public UserSecurityData loadByUserId(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        // 用户关联的角色（一次查出），取启用角色编码
        List<Long> roleIds = sysUserRoleService.listByUserId(userId).stream()
            .map(SysUserRole::getRoleId)
            .toList();
        List<SysRole> roles = sysRoleService.listByIds(roleIds).stream()
            .filter(role -> role.getStatus() != null && role.getStatus() == 1)
            .toList();
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        // 权限点：admin 全量；普通用户按角色并集（role_menu → menu）
        List<String> perms = isAdmin(user) ? sysMenuService.listAllPerms() : loadPermsByRoles(roles);
        return new UserSecurityData(userId, user.getStatus(), user.getIsAdmin(), roleCodes, perms);
    }

    private boolean isAdmin(SysUser user) {
        return user.getIsAdmin() != null && user.getIsAdmin() == 1;
    }

    /**
     * 按角色并集加载权限点：角色-菜单关联与菜单权限标识各一次批量查询。
     */
    private List<String> loadPermsByRoles(List<SysRole> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = sysRoleMenuService.listByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(roleMenus)) {
            return List.of();
        }
        Set<Long> menuIds = new HashSet<>(roleMenus.stream().map(SysRoleMenu::getMenuId).toList());
        return sysMenuService.listEnabledByIds(menuIds).stream()
            .map(SysMenu::getPerms)
            .filter(perms -> perms != null && !perms.isBlank())
            .distinct()
            .toList();
    }
}
