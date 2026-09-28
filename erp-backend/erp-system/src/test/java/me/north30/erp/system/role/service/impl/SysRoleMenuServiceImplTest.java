package me.north30.erp.system.role.service.impl;

import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link SysRoleMenuServiceImpl} 纯 Mockito 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysRoleMenuServiceImplTest {

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @InjectMocks
    private SysRoleMenuServiceImpl service;

    @BeforeAll
    static void setUpTableInfo() {
        // 初始化 MP 表信息缓存，保证 Lambda 条件在纯单测环境可解析
        RoleTestFactory.initTableInfo();
    }

    @Nested
    @DisplayName("根据角色 ID查询角色菜单")
    class ListByRoleIdsTest {

        @Test   
        @DisplayName("查询无角色菜单时，返回空列表")
        void shouldReturnEmpty_whenRoleIdsNull() {
            // When
            List<SysRoleMenu> result = service.listByRoleIds(null);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(sysRoleMenuMapper);
        }

        @Test   
        @DisplayName("查询无角色菜单时，返回空列表")
        void shouldReturnEmpty_whenRoleIdsEmpty() {
            // When
            List<SysRoleMenu> result = service.listByRoleIds(List.of());

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(sysRoleMenuMapper);
        }

        @Test   
        @DisplayName("查询角色菜单时，返回角色菜单列表")
        void shouldReturnRoleMenus_whenRoleIdsGiven() {
            // Given
            List<SysRoleMenu> roleMenus = List.of(
                RoleTestFactory.sysRoleMenu(5L, 1L),
                RoleTestFactory.sysRoleMenu(5L, 2L));
            when(sysRoleMenuMapper.selectList(any())).thenReturn(roleMenus);

            // When
            List<SysRoleMenu> result = service.listByRoleIds(List.of(5L));

            // Then
            assertThat(result).isSameAs(roleMenus);
        }
    }

    @Nested
    @DisplayName("创建角色菜单")
    class CreateRoleMenuTest {

        @Test   
        @DisplayName("创建角色菜单时，返回true")
        void shouldReturnTrue_whenInsertAffected() {
            // Given
            SysRoleMenu roleMenu = RoleTestFactory.sysRoleMenu(5L, 1L);
            when(sysRoleMenuMapper.insert(roleMenu)).thenReturn(1);

            // When
            boolean result = service.createRoleMenu(roleMenu);

            // Then
            assertThat(result).isTrue();
        }

        @Test   
        @DisplayName("创建角色菜单时，返回false")
        void shouldReturnFalse_whenInsertNotAffected() {
            // Given
            SysRoleMenu roleMenu = RoleTestFactory.sysRoleMenu(5L, 1L);
            when(sysRoleMenuMapper.insert(roleMenu)).thenReturn(0);

            // When
            boolean result = service.createRoleMenu(roleMenu);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("批量创建角色菜单")
    class CreateBatchTest {

        @Test   
        @DisplayName("批量创建角色菜单时，不执行任何操作")
        void shouldDoNothing_whenListNull() {
            // When
            service.createBatch(null);

            // Then
            verifyNoInteractions(sysRoleMenuMapper);
        }

        @Test   
        @DisplayName("批量创建角色菜单时，不执行任何操作")
        void shouldDoNothing_whenListEmpty() {
            // When
            service.createBatch(List.of());

            // Then
            verifyNoInteractions(sysRoleMenuMapper);
        }

        @Test   
        @DisplayName("批量创建角色菜单时，批量插入角色菜单")
        void shouldInsertEachRoleMenu_whenListGiven() {
            // Given
            List<SysRoleMenu> roleMenus = List.of(
                RoleTestFactory.sysRoleMenu(5L, 1L),
                RoleTestFactory.sysRoleMenu(5L, 2L));

            // When
            service.createBatch(roleMenus);

            // Then
            verify(sysRoleMenuMapper).insert(argThat((SysRoleMenu roleMenu) ->
                Long.valueOf(5L).equals(roleMenu.getRoleId()) && Long.valueOf(1L).equals(roleMenu.getMenuId())));
            verify(sysRoleMenuMapper).insert(argThat((SysRoleMenu roleMenu) ->
                Long.valueOf(5L).equals(roleMenu.getRoleId()) && Long.valueOf(2L).equals(roleMenu.getMenuId())));
        }
    }
}
