package me.north30.erp.system.core.service.impl;

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
import me.north30.erp.system.core.service.dto.UserSecurityData;
import me.north30.erp.system.core.vo.MenuTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserAccessServiceImpl 聚合取数单元测试：
 * 覆盖 loadByUserId（普通用户/admin/无角色/用户不存在/数据范围档位）、
 * listMenuTree（admin 全量、非 admin 过滤与父链补齐一次查库断言）、
 * listPerms（admin 全量、非 admin 角色并集去重去空白）与账号/部门委托。
 * 各 Sys* 服务均为 Mock，验证批量查询各一次、无循环查库（N+1）。
 */
@ExtendWith(MockitoExtension.class)
class UserAccessServiceImplTest {

    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysDeptService sysDeptService;
    @Mock
    private SysUserRoleService sysUserRoleService;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysRoleMenuService sysRoleMenuService;
    @Mock
    private SysMenuService sysMenuService;

    private UserAccessServiceImpl userAccessService;

    @BeforeEach
    void setUp() {
        userAccessService = new UserAccessServiceImpl(sysUserService, sysDeptService,
            sysUserRoleService, sysRoleService, sysRoleMenuService, sysMenuService);
    }

    private SysUser user(Long id, Integer isAdmin) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("admin");
        user.setStatus(1);
        user.setIsAdmin(isAdmin);
        return user;
    }

    private SysUserRole userRole(Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(roleId);
        return userRole;
    }

    private SysRole role(Long id, String roleCode, Integer status, Integer dataScope) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setStatus(status);
        role.setDataScope(dataScope);
        return role;
    }

    private SysRoleMenu roleMenu(Long roleId, Long menuId) {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private SysMenu menu(Long id, Long parentId, Integer menuType, String menuName, Integer menuSort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setMenuType(menuType);
        menu.setMenuName(menuName);
        menu.setMenuSort(menuSort);
        menu.setStatus(1);
        return menu;
    }

    private SysMenu menuWithPerms(Long id, String perms) {
        SysMenu menu = menu(id, 0L, 2, "菜单" + id, 1);
        menu.setPerms(perms);
        return menu;
    }

    @Test
    @DisplayName("loadByUserId 普通用户：启用角色编码、权限点并集去重去空白、数据范围取最宽，批量查询各一次")
    void loadByUserIdNormalUser() {
        when(sysUserService.getById(1L)).thenReturn(user(1L, 0));
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L), userRole(20L)));
        when(sysRoleService.listByIds(anyCollection()))
            .thenReturn(List.of(role(10L, "ops", 1, 5), role(20L, "viewer", 1, 2)));
        when(sysRoleMenuService.listByRoleIds(anyCollection()))
            .thenReturn(List.of(roleMenu(10L, 100L), roleMenu(10L, 101L), roleMenu(10L, 102L)));
        when(sysMenuService.listEnabledByIds(anyCollection()))
            .thenReturn(List.of(menuWithPerms(100L, "system:user:create"),
                menuWithPerms(101L, "system:user:create"), menuWithPerms(102L, " ")));

        UserSecurityData data = userAccessService.loadByUserId(1L);

        assertEquals(List.of("ops", "viewer"), data.roleCodes());
        assertEquals(List.of("system:user:create"), data.perms());
        assertEquals(2, data.widestDataScope());
        // 每类数据一次批量查询，无循环查库
        verify(sysUserRoleService, times(1)).listByUserId(1L);
        verify(sysRoleService, times(1)).listByIds(anyCollection());
        verify(sysRoleMenuService, times(1)).listByRoleIds(anyCollection());
        verify(sysMenuService, times(1)).listEnabledByIds(anyCollection());
    }

    @Test
    @DisplayName("loadByUserId admin：权限点取全量，不走角色-菜单关联查询")
    void loadByUserIdAdmin() {
        when(sysUserService.getById(1L)).thenReturn(user(1L, 1));
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L)));
        when(sysRoleService.listByIds(anyCollection())).thenReturn(List.of(role(10L, "admin", 1, 1)));
        when(sysMenuService.listAllPerms()).thenReturn(List.of("system:user:create", "system:menu:list"));

        UserSecurityData data = userAccessService.loadByUserId(1L);

        assertEquals(List.of("admin"), data.roleCodes());
        assertEquals(List.of("system:user:create", "system:menu:list"), data.perms());
        assertEquals(1, data.widestDataScope());
        verify(sysRoleMenuService, never()).listByRoleIds(any());
        verify(sysMenuService, never()).listEnabledByIds(any());
    }

    @Test
    @DisplayName("loadByUserId 用户不存在：返回 null 且不查角色")
    void loadByUserIdUserNotFound() {
        when(sysUserService.getById(99L)).thenReturn(null);

        assertNull(userAccessService.loadByUserId(99L));
        verify(sysUserRoleService, never()).listByUserId(any());
    }

    @Test
    @DisplayName("loadByUserId 无启用角色：角色编码与权限点为空，数据范围兜底仅本人（6）")
    void loadByUserIdNoRoles() {
        when(sysUserService.getById(1L)).thenReturn(user(1L, 0));
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of());
        when(sysRoleService.listByIds(anyCollection())).thenReturn(List.of());

        UserSecurityData data = userAccessService.loadByUserId(1L);

        assertEquals(List.of(), data.roleCodes());
        assertEquals(List.of(), data.perms());
        assertEquals(6, data.widestDataScope());
        verify(sysRoleMenuService, never()).listByRoleIds(any());
    }

    @Test
    @DisplayName("loadByUserId 仅自定义数据范围（9）：档位取 9")
    void loadByUserIdCustomScopeOnly() {
        when(sysUserService.getById(1L)).thenReturn(user(1L, 0));
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L)));
        when(sysRoleService.listByIds(anyCollection())).thenReturn(List.of(role(10L, "custom", 1, 9)));
        when(sysRoleMenuService.listByRoleIds(anyCollection())).thenReturn(List.of());

        UserSecurityData data = userAccessService.loadByUserId(1L);

        assertEquals(9, data.widestDataScope());
        assertEquals(List.of(), data.perms());
    }

    @Test
    @DisplayName("listMenuTree admin：全量启用菜单组树并过滤按钮，仅一次 listEnabled 查询")
    void listMenuTreeAdmin() {
        when(sysMenuService.listEnabled()).thenReturn(List.of(
            menu(1L, 0L, 1, "系统管理", 1),
            menu(2L, 1L, 2, "用户管理", 1),
            menu(3L, 2L, 3, "用户新增", 1),
            menu(4L, 0L, 2, "工作台", 2)));

        List<MenuTreeVO> tree = userAccessService.listMenuTree(1L, true);

        assertEquals(2, tree.size());
        assertEquals(1L, tree.get(0).getMenuId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(2L, tree.get(0).getChildren().get(0).getMenuId());
        assertEquals(4L, tree.get(1).getMenuId());
        verify(sysMenuService, times(1)).listEnabled();
        verify(sysMenuService, never()).listEnabledByIds(any());
        verify(sysUserRoleService, never()).listByUserId(any());
    }

    @Test
    @DisplayName("listMenuTree 非 admin：角色只勾叶子菜单，内存补齐父级链且仅一次全量查库")
    void listMenuTreeNormalUserExpandsAncestorsWithSingleQuery() {
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L)));
        when(sysRoleMenuService.listByRoleIds(anyCollection())).thenReturn(List.of(roleMenu(10L, 2L)));
        when(sysMenuService.listEnabled()).thenReturn(List.of(
            menu(1L, 0L, 1, "系统管理", 1),
            menu(2L, 1L, 2, "用户管理", 2)));

        List<MenuTreeVO> tree = userAccessService.listMenuTree(1L, false);

        assertEquals(1, tree.size());
        assertEquals(1L, tree.get(0).getMenuId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(2L, tree.get(0).getChildren().get(0).getMenuId());
        // 关键断言：父级链补齐仅依赖一次全量启用菜单查询，无循环查库（N+1）
        verify(sysMenuService, times(1)).listEnabled();
        verify(sysMenuService, never()).listEnabledByIds(any());
    }

    @Test
    @DisplayName("listMenuTree 非 admin：父级停用时停止向上追溯，叶子无父链不挂树")
    void listMenuTreeNormalUserStopsAtDisabledAncestor() {
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L)));
        when(sysRoleMenuService.listByRoleIds(anyCollection())).thenReturn(List.of(roleMenu(10L, 2L)));
        // 父菜单 1 已停用（不在启用集合中）
        when(sysMenuService.listEnabled()).thenReturn(List.of(
            menu(2L, 1L, 2, "用户管理", 2)));

        List<MenuTreeVO> tree = userAccessService.listMenuTree(1L, false);

        assertTrue(tree.isEmpty());
        verify(sysMenuService, times(1)).listEnabled();
        verify(sysMenuService, never()).listEnabledByIds(any());
    }

    @Test
    @DisplayName("listPerms admin：返回全量权限点集合，不查角色")
    void listPermsAdmin() {
        when(sysMenuService.listAllPerms()).thenReturn(List.of("a:b:c", "d:e:f"));

        Set<String> perms = userAccessService.listPerms(1L, true);

        assertEquals(Set.of("a:b:c", "d:e:f"), perms);
        verify(sysUserRoleService, never()).listByUserId(any());
    }

    @Test
    @DisplayName("listPerms 非 admin：按启用角色并集取权限点，去重并过滤空白")
    void listPermsNormalUser() {
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L)));
        when(sysRoleService.listByIds(anyCollection())).thenReturn(List.of(role(10L, "ops", 1, 5)));
        when(sysRoleMenuService.listByRoleIds(anyCollection()))
            .thenReturn(List.of(roleMenu(10L, 100L), roleMenu(10L, 101L)));
        when(sysMenuService.listEnabledByIds(anyCollection()))
            .thenReturn(List.of(menuWithPerms(100L, "a:b:c"), menuWithPerms(101L, null)));

        Set<String> perms = userAccessService.listPerms(1L, false);

        assertEquals(Set.of("a:b:c"), perms);
        verify(sysMenuService, times(1)).listEnabledByIds(anyCollection());
    }

    @Test
    @DisplayName("listRoleCodes：仅返回启用角色编码")
    void listRoleCodesOnlyEnabledRoles() {
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of(userRole(10L), userRole(20L)));
        when(sysRoleService.listByIds(anyCollection()))
            .thenReturn(List.of(role(10L, "ops", 1, 5), role(20L, "off", 0, 1)));

        assertEquals(List.of("ops"), userAccessService.listRoleCodes(1L));
    }

    @Test
    @DisplayName("getDeptName：ID 为空/部门不存在返回 null，存在返回名称")
    void getDeptNameVariants() {
        assertNull(userAccessService.getDeptName(null));

        SysDept dept = new SysDept();
        dept.setId(5L);
        dept.setDeptName("研发部");
        when(sysDeptService.getById(5L)).thenReturn(dept);
        assertEquals("研发部", userAccessService.getDeptName(5L));

        when(sysDeptService.getById(6L)).thenReturn(null);
        assertNull(userAccessService.getDeptName(6L));
    }

    @Test
    @DisplayName("账号读取与更新：委托用户服务")
    void accountAccessDelegatesToUserService() {
        SysUser user = user(1L, 1);
        when(sysUserService.getByUsername("admin")).thenReturn(user);
        when(sysUserService.getById(1L)).thenReturn(user);

        assertSame(user, userAccessService.getByUsername("admin"));
        assertSame(user, userAccessService.getUserById(1L));

        userAccessService.updateLastLogin(1L, "127.0.0.1", LocalDateTime.now());
        userAccessService.updateUser(user);

        verify(sysUserService).updateLastLogin(eq(1L), eq("127.0.0.1"), any(LocalDateTime.class));
        verify(sysUserService).updateUser(user);
    }
}
