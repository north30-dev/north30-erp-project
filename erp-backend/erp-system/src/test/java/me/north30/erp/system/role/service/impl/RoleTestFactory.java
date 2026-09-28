package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.entity.SysUserRole;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;

/**
 * 角色域单元测试数据工厂：集中构建实体、DTO 与 MP 表信息缓存，供同域测试类共用。
 */
public final class RoleTestFactory {

    /** 通用角色更新时间 */
    public static final LocalDateTime TIME_UPDATE = LocalDateTime.of(2026, 9, 1, 8, 30, 0);

    private RoleTestFactory() {
    }

    /**
     * 初始化测试涉及实体的 MP TableInfo 缓存：Lambda 列解析依赖该缓存，
     * 纯 Mockito 单测环境无 MyBatis 启动流程，必须手动初始化。
     */
    public static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleDataScope.class);
    }

    /** 默认角色：id=5，编码 R001，名称 操作员，数据范围 2，非内置 */
    public static SysRole sysRole() {
        SysRole role = new SysRole();
        role.setId(5L);
        role.setRoleCode("R001");
        role.setRoleName("操作员");
        role.setRoleSort(2);
        role.setDataScope(2);
        role.setIsBuiltin(0);
        role.setStatus(1);
        return role;
    }

    /** 指定 ID/编码/名称的角色（分页列表用） */
    public static SysRole sysRole(Long id, String roleCode, String roleName) {
        SysRole role = sysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        return role;
    }

    /** 内置角色（is_builtin=1，不可删除） */
    public static SysRole builtinRole() {
        SysRole role = sysRole();
        role.setIsBuiltin(1);
        return role;
    }

    /** 更新后回查的最新角色快照（仅含更新时间） */
    public static SysRole latestRoleWithUpdateTime() {
        SysRole role = new SysRole();
        role.setId(5L);
        role.setUpdateTime(TIME_UPDATE);
        return role;
    }

    /** 用户-角色关联 */
    public static SysUserRole sysUserRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    /** 角色-菜单关联 */
    public static SysRoleMenu sysRoleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    /** 菜单/按钮权限点（perms 可为 null 或空白，用于权限点计数边界） */
    public static SysMenu sysMenu(Long id, String perms) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setMenuName("菜单" + id);
        menu.setParentId(0L);
        menu.setMenuType(2);
        menu.setPerms(perms);
        menu.setVisible(1);
        menu.setStatus(1);
        return menu;
    }

    /** 角色数据范围配置（roleId=5） */
    public static SysRoleDataScope sysRoleDataScope(String bizObject, String filterDimension,
                                                    Integer scopeType, String deptIds, String userIds,
                                                    Integer fieldMask) {
        SysRoleDataScope scope = new SysRoleDataScope();
        scope.setRoleId(5L);
        scope.setBizObject(bizObject);
        scope.setFilterDimension(filterDimension);
        scope.setScopeType(scopeType);
        scope.setDeptIds(deptIds);
        scope.setUserIds(userIds);
        scope.setFieldMask(fieldMask);
        return scope;
    }

    /** 合法新增角色请求：roleSort/dataScope/status 留空走默认值 */
    public static RoleCreateDTO validRoleCreateDTO() {
        return new RoleCreateDTO("R001", "操作员", null, null, null, "备注");
    }
}
