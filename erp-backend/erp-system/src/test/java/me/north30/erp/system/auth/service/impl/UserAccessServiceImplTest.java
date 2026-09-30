package me.north30.erp.system.auth.service.impl;

import me.north30.erp.system.common.util.MenuTreeUtil;
import me.north30.erp.system.auth.dto.UserSecurityData;
import me.north30.erp.system.dept.service.SysDeptService;
import me.north30.erp.system.menu.converter.MenuConverterImpl;
import me.north30.erp.system.menu.service.SysMenuService;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import me.north30.erp.system.role.service.SysRoleMenuService;
import me.north30.erp.system.role.service.SysRoleService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.ADMIN_USER_ID;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.ROLE_ID_A;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.ROLE_ID_B;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.USER_ID;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.adminUser;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.dept;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.menu;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.role;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.roleMenu;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.user;
import static me.north30.erp.system.auth.service.impl.UserAccessServiceImplTestFactory.userRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * UserAccessServiceImpl 单元测试：权限装配（安全数据/角色/部门/菜单树/权限点）的正常与边界路径。
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

    @Spy
    private final MenuTreeUtil menuTreeUtil = new MenuTreeUtil(new MenuConverterImpl());

    @InjectMocks
    private UserAccessServiceImpl userAccessService;

    @Nested
    @DisplayName("按用户 ID 加载安全数据")
    class LoadByUserIdTest {

        @Test
        @DisplayName("用户不存在：返回 null 且不触发角色/菜单取数")
        void shouldReturnNull_whenUserNotExists() {
            // Given
            when(sysUserService.getById(USER_ID)).thenReturn(null);
            // When
            UserSecurityData data = userAccessService.loadByUserId(USER_ID);
            // Then
            assertThat(data).isNull();
            verifyNoInteractions(sysUserRoleService, sysRoleService, sysRoleMenuService, sysMenuService);
        }

        @Test
        @DisplayName("admin 用户：返回全量权限点，且不查角色-菜单关联")
        void shouldReturnAllPermsAndWidestScope_whenAdmin() {
            // Given
            when(sysUserService.getById(ADMIN_USER_ID)).thenReturn(adminUser());
            when(sysUserRoleService.listByUserId(ADMIN_USER_ID))
                .thenReturn(List.of(userRole(ADMIN_USER_ID, ROLE_ID_A)));
            when(sysRoleService.listByIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(role(ROLE_ID_A, "admin", 1, 1)));
            when(sysMenuService.listAllPerms()).thenReturn(List.of("system:user:list", "system:role:list"));
            // When
            UserSecurityData data = userAccessService.loadByUserId(ADMIN_USER_ID);
            // Then
            assertThat(data.userId()).isEqualTo(ADMIN_USER_ID);
            assertThat(data.status()).isEqualTo(1);
            assertThat(data.isAdmin()).isEqualTo(1);
            assertThat(data.roleCodes()).containsExactly("admin");
            assertThat(data.perms()).containsExactly("system:user:list", "system:role:list");
            assertThat(data.widestDataScope()).isEqualTo(1);
            verify(sysRoleMenuService, never()).listByRoleIds(anyCollection());
            verify(sysMenuService, never()).listEnabledByIds(anyCollection());
        }

        @Test
        @DisplayName("普通用户多角色：权限点按角色并集，数据范围取最宽档位（2 优于 6）")
        void shouldUnionPermsAndWidestScope_whenNormalUser() {
            // Given
            when(sysUserService.getById(USER_ID)).thenReturn(user());
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A), userRole(USER_ID, ROLE_ID_B)));
            when(sysRoleService.listByIds(List.of(ROLE_ID_A, ROLE_ID_B)))
                .thenReturn(List.of(role(ROLE_ID_A, "sales", 1, 2), role(ROLE_ID_B, "stock", 1, 6)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A, ROLE_ID_B)))
                .thenReturn(List.of(roleMenu(ROLE_ID_A, 100L), roleMenu(ROLE_ID_B, 101L)));
            when(sysMenuService.listEnabledByIds(anyCollection())).thenReturn(List.of(
                menu(100L, 0L, 2, "sales:order:list"), menu(101L, 0L, 2, "stock:check:list")));
            // When
            UserSecurityData data = userAccessService.loadByUserId(USER_ID);
            // Then
            assertThat(data.roleCodes()).containsExactly("sales", "stock");
            assertThat(data.perms()).containsExactlyInAnyOrder("sales:order:list", "stock:check:list");
            assertThat(data.widestDataScope()).isEqualTo(2);
            verify(sysMenuService).listEnabledByIds(argThat(ids ->
                ids != null && ids.size() == 2 && ids.containsAll(Set.of(100L, 101L))));
            verify(sysMenuService, never()).listAllPerms();
        }

        @Test
        @DisplayName("角色含停用状态：仅保留启用角色，权限点与角色编码均过滤")
        void shouldExcludeDisabledRoles_whenLoad() {
            // Given
            when(sysUserService.getById(USER_ID)).thenReturn(user());
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A), userRole(USER_ID, ROLE_ID_B)));
            when(sysRoleService.listByIds(List.of(ROLE_ID_A, ROLE_ID_B)))
                .thenReturn(List.of(role(ROLE_ID_A, "sales", 1, 2), role(ROLE_ID_B, "stock", 0, 2)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(roleMenu(ROLE_ID_A, 100L)));
            when(sysMenuService.listEnabledByIds(anyCollection()))
                .thenReturn(List.of(menu(100L, 0L, 2, "sales:order:list")));
            // When
            UserSecurityData data = userAccessService.loadByUserId(USER_ID);
            // Then
            assertThat(data.roleCodes()).containsExactly("sales");
            assertThat(data.perms()).containsExactly("sales:order:list");
            verify(sysRoleMenuService).listByRoleIds(argThat(ids ->
                ids != null && ids.size() == 1 && ids.contains(ROLE_ID_A)));
        }

        @Test
        @DisplayName("用户无角色：角色为空、权限点为空、数据范围按仅本人（6）")
        void shouldReturnSelfScope_whenNoRoles() {
            // Given
            when(sysUserService.getById(USER_ID)).thenReturn(user());
            when(sysUserRoleService.listByUserId(USER_ID)).thenReturn(List.of());
            when(sysRoleService.listByIds(List.of())).thenReturn(List.of());
            // When
            UserSecurityData data = userAccessService.loadByUserId(USER_ID);
            // Then
            assertThat(data.roleCodes()).isEmpty();
            assertThat(data.perms()).isEmpty();
            assertThat(data.widestDataScope()).isEqualTo(6);
            verifyNoInteractions(sysRoleMenuService, sysMenuService);
        }

        @Test
        @DisplayName("角色数据范围未配置（null）：数据范围回退为 9-自定义")
        void shouldFallbackCustomScope_whenRoleDataScopeMissing() {
            // Given
            when(sysUserService.getById(USER_ID)).thenReturn(user());
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A)));
            when(sysRoleService.listByIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(role(ROLE_ID_A, "custom", 1, null)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A))).thenReturn(List.of());
            // When
            UserSecurityData data = userAccessService.loadByUserId(USER_ID);
            // Then
            assertThat(data.roleCodes()).containsExactly("custom");
            assertThat(data.perms()).isEmpty();
            assertThat(data.widestDataScope()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("按用户名查询账号")
    class GetByUsernameTest {

        @Test
        @DisplayName("委托用户服务查询并透传结果")
        void shouldDelegateToUserService_whenGetByUsername() {
            // Given
            SysUser stubUser = user();
            when(sysUserService.getByUsername("zhangsan")).thenReturn(stubUser);
            // When
            SysUser result = userAccessService.getByUsername("zhangsan");
            // Then
            assertThat(result).isSameAs(stubUser);
            verify(sysUserService).getByUsername("zhangsan");
        }
    }

    @Nested
    @DisplayName("按用户 ID 查询账号")
    class GetUserByIdTest {

        @Test
        @DisplayName("委托用户服务查询并透传结果")
        void shouldDelegateToUserService_whenGetUserById() {
            // Given
            SysUser stubUser = user();
            when(sysUserService.getById(USER_ID)).thenReturn(stubUser);
            // When
            SysUser result = userAccessService.getUserById(USER_ID);
            // Then
            assertThat(result).isSameAs(stubUser);
            verify(sysUserService).getById(USER_ID);
        }
    }

    @Nested
    @DisplayName("更新最后登录信息")
    class UpdateLastLoginTest {

        @Test
        @DisplayName("委托用户服务按原参数写入")
        void shouldDelegateToUserService_whenUpdateLastLogin() {
            // Given
            LocalDateTime loginTime = LocalDateTime.now();
            // When
            userAccessService.updateLastLogin(USER_ID, "192.168.1.10", loginTime);
            // Then
            verify(sysUserService).updateLastLogin(USER_ID, "192.168.1.10", loginTime);
        }
    }

    @Nested
    @DisplayName("更新用户")
    class UpdateUserTest {

        @Test
        @DisplayName("委托用户服务更新同一实体")
        void shouldDelegateToUserService_whenUpdateUser() {
            // Given
            SysUser stubUser = user();
            // When
            userAccessService.updateUser(stubUser);
            // Then
            verify(sysUserService).updateUser(stubUser);
        }
    }

    @Nested
    @DisplayName("查询部门名称")
    class GetDeptNameTest {

        @Test
        @DisplayName("部门 ID 为空：直接返回 null 且不查库")
        void shouldReturnNull_whenDeptIdNull() {
            // When
            String deptName = userAccessService.getDeptName(null);
            // Then
            assertThat(deptName).isNull();
            verifyNoInteractions(sysDeptService);
        }

        @Test
        @DisplayName("部门存在：返回部门名称")
        void shouldReturnDeptName_whenDeptExists() {
            // Given
            when(sysDeptService.getById(10L)).thenReturn(dept(10L, "研发部"));
            // When
            String deptName = userAccessService.getDeptName(10L);
            // Then
            assertThat(deptName).isEqualTo("研发部");
        }

        @Test
        @DisplayName("部门不存在：返回 null")
        void shouldReturnNull_whenDeptNotExists() {
            // Given
            when(sysDeptService.getById(99L)).thenReturn(null);
            // When
            String deptName = userAccessService.getDeptName(99L);
            // Then
            assertThat(deptName).isNull();
        }
    }

    @Nested
    @DisplayName("查询启用角色编码")
    class ListRoleCodesTest {

        @Test
        @DisplayName("多角色含停用角色：仅返回启用角色编码")
        void shouldReturnOnlyEnabledRoleCodes_whenRolesContainDisabled() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A), userRole(USER_ID, ROLE_ID_B)));
            when(sysRoleService.listByIds(List.of(ROLE_ID_A, ROLE_ID_B)))
                .thenReturn(List.of(role(ROLE_ID_A, "sales", 1, 2), role(ROLE_ID_B, "stock", 0, 2)));
            // When
            List<String> roleCodes = userAccessService.listRoleCodes(USER_ID);
            // Then
            assertThat(roleCodes).containsExactly("sales");
        }
    }

    @Nested
    @DisplayName("查询用户菜单树")
    class ListMenuTreeTest {

        @Test
        @DisplayName("admin 用户：全量启用菜单过滤按钮后组树，不查角色关联")
        void shouldBuildFullTree_whenAdmin() {
            // Given
            when(sysMenuService.listEnabled()).thenReturn(List.of(
                menu(1L, 0L, 1, null), menu(2L, 1L, 2, null), menu(3L, 1L, 3, null)));
            // When
            List<MenuTreeVO> tree = userAccessService.listMenuTree(USER_ID, true);
            // Then
            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getMenuId()).isEqualTo(1L);
            assertThat(tree.get(0).getChildren()).hasSize(1);
            assertThat(tree.get(0).getChildren().get(0).getMenuId()).isEqualTo(2L);
            verifyNoInteractions(sysUserRoleService, sysRoleMenuService);
        }

        @Test
        @DisplayName("普通用户仅勾选叶子菜单：内存补齐父级链后组成两级树")
        void shouldExpandAncestors_whenRoleOnlyAssignedLeaf() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(roleMenu(ROLE_ID_A, 2L)));
            when(sysMenuService.listEnabled())
                .thenReturn(List.of(menu(1L, 0L, 1, null), menu(2L, 1L, 2, null)));
            // When
            List<MenuTreeVO> tree = userAccessService.listMenuTree(USER_ID, false);
            // Then
            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getMenuId()).isEqualTo(1L);
            assertThat(tree.get(0).getChildren())
                .extracting(MenuTreeVO::getMenuId)
                .containsExactly(2L);
            verify(sysRoleService, never()).listByIds(anyCollection());
        }

        @Test
        @DisplayName("角色勾选按钮菜单：按钮（类型 3）不出现在树中")
        void shouldExcludeButtonMenus_whenNormalUser() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(roleMenu(ROLE_ID_A, 1L), roleMenu(ROLE_ID_A, 3L)));
            when(sysMenuService.listEnabled())
                .thenReturn(List.of(menu(1L, 0L, 1, null), menu(3L, 1L, 3, null)));
            // When
            List<MenuTreeVO> tree = userAccessService.listMenuTree(USER_ID, false);
            // Then
            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getMenuId()).isEqualTo(1L);
            assertThat(tree.get(0).getChildren()).isNull();
        }

        @Test
        @DisplayName("父级菜单不在启用集合（停用/不存在）：停止向上追溯且不作为根节点输出")
        void shouldStopExpansion_whenParentNotEnabled() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(roleMenu(ROLE_ID_A, 5L)));
            when(sysMenuService.listEnabled()).thenReturn(List.of(menu(5L, 7L, 2, null)));
            // When
            List<MenuTreeVO> tree = userAccessService.listMenuTree(USER_ID, false);
            // Then
            assertThat(tree).isEmpty();
        }

        @Test
        @DisplayName("用户无任何角色：仍执行一次全量启用菜单查询并返回空树")
        void shouldReturnEmptyTree_whenNoRoleAssigned() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID)).thenReturn(List.of());
            when(sysRoleMenuService.listByRoleIds(List.of())).thenReturn(List.of());
            when(sysMenuService.listEnabled()).thenReturn(List.of());
            // When
            List<MenuTreeVO> tree = userAccessService.listMenuTree(USER_ID, false);
            // Then
            assertThat(tree).isEmpty();
            verify(sysMenuService).listEnabled();
        }
    }

    @Nested
    @DisplayName("查询权限点集合")
    class ListPermsTest {

        @Test
        @DisplayName("admin 用户：返回全量权限点且不查角色-菜单关联")
        void shouldReturnAllPerms_whenAdmin() {
            // Given
            when(sysMenuService.listAllPerms()).thenReturn(List.of("system:user:list", "system:role:list"));
            // When
            Set<String> perms = userAccessService.listPerms(ADMIN_USER_ID, true);
            // Then
            assertThat(perms).containsExactlyInAnyOrder("system:user:list", "system:role:list");
            verifyNoInteractions(sysUserRoleService, sysRoleMenuService);
            verify(sysMenuService, never()).listEnabledByIds(anyCollection());
        }

        @Test
        @DisplayName("普通用户无角色：返回空权限点集合")
        void shouldReturnEmpty_whenNoRoles() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID)).thenReturn(List.of());
            when(sysRoleService.listByIds(List.of())).thenReturn(List.of());
            // When
            Set<String> perms = userAccessService.listPerms(USER_ID, false);
            // Then
            assertThat(perms).isEmpty();
            verifyNoInteractions(sysRoleMenuService, sysMenuService);
        }

        @Test
        @DisplayName("普通用户按角色并集取权限点：空白/缺失权限标识与重复项被过滤")
        void shouldUnionPermsFromRoleMenus_whenNormalUser() {
            // Given
            when(sysUserRoleService.listByUserId(USER_ID))
                .thenReturn(List.of(userRole(USER_ID, ROLE_ID_A)));
            when(sysRoleService.listByIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(role(ROLE_ID_A, "sales", 1, 2)));
            when(sysRoleMenuService.listByRoleIds(List.of(ROLE_ID_A)))
                .thenReturn(List.of(roleMenu(ROLE_ID_A, 100L), roleMenu(ROLE_ID_A, 101L)));
            when(sysMenuService.listEnabledByIds(anyCollection())).thenReturn(List.of(
                menu(100L, 0L, 2, "sales:order:list"), menu(101L, 0L, 2, null)));
            // When
            Set<String> perms = userAccessService.listPerms(USER_ID, false);
            // Then
            assertThat(perms).containsExactly("sales:order:list");
            verify(sysMenuService).listEnabledByIds(argThat(ids ->
                ids != null && ids.size() == 2 && ids.containsAll(Set.of(100L, 101L))));
            verify(sysMenuService, never()).listAllPerms();
        }
    }
}
