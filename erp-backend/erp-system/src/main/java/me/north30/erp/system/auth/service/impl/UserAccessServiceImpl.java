package me.north30.erp.system.auth.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.dept.service.SysDeptService;
import me.north30.erp.system.menu.service.SysMenuService;
import me.north30.erp.system.role.service.SysRoleMenuService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.user.service.SysUserService;
import me.north30.erp.system.auth.service.UserAccessService;
import me.north30.erp.system.auth.dto.UserSecurityData;
import me.north30.erp.system.auth.strategy.AccessAssembleStrategy;
import me.north30.erp.system.common.util.MenuTreeUtil;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户访问聚合服务实现：统一收敛认证链的账号、部门、角色、菜单、权限点与数据范围取数编排。
 * <p>admin（is_admin=1）返回全量权限点与全量菜单；普通用户按启用角色并集取数，
 * 全部一次批量查出后内存组装（父级链补齐、组树），杜绝循环查库（N+1）。
 * 角色/权限点/数据范围/祖先链装配由 {@link AccessAssembleStrategy} 承载。</p>
 */
@Service
@RequiredArgsConstructor
public class UserAccessServiceImpl implements UserAccessService {

    private final SysUserService sysUserService;
    private final SysDeptService sysDeptService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysMenuService sysMenuService;
    private final MenuTreeUtil menuTreeUtil;
    private final AccessAssembleStrategy accessAssembleStrategy;

    @Override
    @Transactional(readOnly = true)
    public UserSecurityData loadByUserId(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        // 用户关联的角色（一次查出），仅保留启用角色
        List<SysRole> roles = accessAssembleStrategy.loadEnabledRoles(userId);
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        // 权限点：admin 全量；普通用户按角色并集（role_menu → menu）
        List<String> perms = List.copyOf(accessAssembleStrategy.loadPerms(user.isAdmin(), roles));
        return new UserSecurityData(userId, user.getStatus(), user.getIsAdmin(), roleCodes,
            perms, accessAssembleStrategy.resolveWidestDataScope(roles));
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
        return accessAssembleStrategy.loadEnabledRoles(userId).stream()
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
            Set<Long> resultIds = accessAssembleStrategy.expandWithAncestors(menuIds, enabledById);
            menus = enabledById.values().stream()
                .filter(menu -> resultIds.contains(menu.getId()))
                .filter(menu -> menu.getMenuType() != null && menu.getMenuType() != 3)
                .toList();
        }
        return menuTreeUtil.buildTree(menus.stream().map(menuTreeUtil::toVO).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> listPerms(Long userId, boolean isAdmin) {
        return accessAssembleStrategy.loadPerms(isAdmin,
            isAdmin ? List.of() : accessAssembleStrategy.loadEnabledRoles(userId));
    }
}
