package me.north30.erp.system.auth.service.impl;

import me.north30.erp.system.auth.dto.UserSecurityData;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.user.entity.SysUser;

import java.util.List;

/**
 * UserAccessServiceImpl 测试数据静态工厂：统一构造用户、角色、关联关系、菜单与部门等测试数据。
 */
public final class UserAccessServiceImplTestFactory {

    /** 被测普通用户 ID */
    public static final Long USER_ID = 2L;

    /** admin 用户 ID */
    public static final Long ADMIN_USER_ID = 1L;

    /** 角色一 ID */
    public static final Long ROLE_ID_A = 10L;

    /** 角色二 ID */
    public static final Long ROLE_ID_B = 11L;

    private UserAccessServiceImplTestFactory() {
    }

    /**
     * 启用状态的普通用户（is_admin=0）。
     */
    public static SysUser user() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("zhangsan");
        user.setStatus(1);
        user.setIsAdmin(0);
        return user;
    }

    /**
     * 启用状态的超级管理员（is_admin=1）。
     */
    public static SysUser adminUser() {
        SysUser user = new SysUser();
        user.setId(ADMIN_USER_ID);
        user.setUsername("admin");
        user.setStatus(1);
        user.setIsAdmin(1);
        return user;
    }

    /**
     * 角色：指定编码、状态与数据范围档位。
     */
    public static SysRole role(Long id, String roleCode, Integer status, Integer dataScope) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName("角色" + id);
        role.setStatus(status);
        role.setDataScope(dataScope);
        return role;
    }

    /**
     * 用户-角色关联。
     */
    public static SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    /**
     * 角色-菜单关联。
     */
    public static SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    /**
     * 菜单：指定 ID、父 ID、类型与权限点（1-目录 2-菜单 3-按钮）。
     */
    public static SysMenu menu(Long id, Long parentId, Integer menuType, String perms) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuType(menuType);
        menu.setPerms(perms);
        menu.setMenuName("菜单" + id);
        menu.setMenuSort(id.intValue());
        menu.setVisible(1);
        return menu;
    }

    /**
     * 部门。
     */
    public static SysDept dept(Long id, String deptName) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptName(deptName);
        return dept;
    }

    /**
     * 用户安全数据（启用、普通用户、单角色、数据范围 2-本组织及下级）。
     */
    public static UserSecurityData securityData() {
        return new UserSecurityData(USER_ID, 1, 0, List.of("sales"),
            List.of("sales:order:list"), 2);
    }
}
