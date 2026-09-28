package me.north30.erp.system.menu.service.impl;

import me.north30.erp.system.menu.MenuTestFactory;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.support.MpTableInfoInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysMenuServiceImpl} 纯单元测试：仅 mock Mapper，不连接数据库。
 */
@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    @InjectMocks
    private SysMenuServiceImpl service;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysMenu.class);
    }

    @Nested
    class GetByMenuNameTest {

        @Test
        void shouldReturnMenu_whenMenuNameExists() {
            // Given
            SysMenu menu = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1);
            given(sysMenuMapper.selectOne(any())).willReturn(menu);

            // When
            SysMenu actual = service.getByMenuName("系统管理");

            // Then
            assertThat(actual).isSameAs(menu);
            verify(sysMenuMapper).selectOne(any());
        }

        @Test
        void shouldReturnNull_whenMenuNameMissing() {
            // Given
            given(sysMenuMapper.selectOne(any())).willReturn(null);

            // When
            SysMenu actual = service.getByMenuName("不存在的菜单");

            // Then
            assertThat(actual).isNull();
        }
    }

    @Nested
    @DisplayName("查询所有启用菜单")
    class ListEnabledTest {

        @Test   
        @DisplayName("查询所有启用菜单时，返回启用菜单列表")
        void shouldReturnEnabledMenus_whenMapperReturnsList() {
            // Given
            List<SysMenu> menus = List.of(
                MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1),
                MenuTestFactory.pageMenu(2L, "用户管理", 1L, "system/user/index", "system:user:list", 1));
            given(sysMenuMapper.selectList(any())).willReturn(menus);

            // When
            List<SysMenu> actual = service.listEnabled();

            // Then
            assertThat(actual).isSameAs(menus);
        }
    }

    @Nested
    @DisplayName("查询启用菜单列表")
    class ListEnabledByIdsTest {

        @Test   
        @DisplayName("查询启用菜单列表时，返回空列表")
        void shouldReturnEmpty_whenIdsEmpty() {
            // Given
            Collection<Long> ids = List.of();

            // When
            List<SysMenu> actual = service.listEnabledByIds(ids);

            // Then
            assertThat(actual).isEmpty();
            verifyNoInteractions(sysMenuMapper);
        }

        @Test   
        @DisplayName("查询启用菜单列表时，返回空列表")
        void shouldReturnEmpty_whenIdsNull() {
            // When
            List<SysMenu> actual = service.listEnabledByIds(null);

            // Then
            assertThat(actual).isEmpty();
            verifyNoInteractions(sysMenuMapper);
        }

        @Test   
        @DisplayName("查询启用菜单列表时，返回启用菜单列表")
        void shouldReturnMenus_whenIdsPresent() {
            // Given
            List<SysMenu> menus = List.of(
                MenuTestFactory.pageMenu(1L, "用户管理", 2L, "system/user/index", "system:user:list", 1));
            given(sysMenuMapper.selectList(any())).willReturn(menus);

            // When
            List<SysMenu> actual = service.listEnabledByIds(List.of(1L, 2L));

            // Then
            assertThat(actual).containsExactlyElementsOf(menus);
            verify(sysMenuMapper).selectList(any());
        }
    }

    @Nested
    @DisplayName("查询所有启用菜单权限点")
    class ListAllPermsTest {

        @Test   
        @DisplayName("查询所有启用菜单权限点时，返回去重非空权限点列表")
        void shouldReturnDistinctNonBlankPerms_whenEnabledMenusExist() {
            // Given：包含空权限点、空白权限点与重复权限点
            SysMenu permA = MenuTestFactory.pageMenu(1L, "用户管理", 0L, "system/user/index", "system:user:list", 1);
            SysMenu permADuplicated = MenuTestFactory.pageMenu(2L, "用户管理2", 0L, "system/user/index", "system:user:list", 2);
            SysMenu noPerms = MenuTestFactory.dirMenu(3L, "系统管理", 0L, 3);
            SysMenu blankPerms = MenuTestFactory.pageMenu(4L, "待配置", 0L, "a/index", "  ", 4);
            SysMenu permB = MenuTestFactory.pageMenu(5L, "角色管理", 0L, "system/role/index", "system:role:list", 5);
            given(sysMenuMapper.selectList(any())).willReturn(List.of(permA, permADuplicated, noPerms, blankPerms, permB));

            // When
            List<String> perms = service.listAllPerms();

            // Then：仅保留非空权限点并去重
            assertThat(perms).containsExactly("system:user:list", "system:role:list");
        }

        @Test   
        @DisplayName("查询所有启用菜单权限点时，返回空列表")
        void shouldReturnEmpty_whenNoEnabledMenus() {
            // Given
            given(sysMenuMapper.selectList(any())).willReturn(List.of());

            // When
            List<String> perms = service.listAllPerms();

            // Then
            assertThat(perms).isEmpty();
        }
    }

    @Nested
    @DisplayName("创建菜单")
    class CreateMenuTest {

        @Test   
        @DisplayName("创建菜单时，返回 true")
        void shouldReturnTrue_whenInsertAffected() {
            // Given
            SysMenu menu = MenuTestFactory.buttonMenu(null, "新增用户", 1L, "system:user:create", 1);
            given(sysMenuMapper.insert(menu)).willReturn(1);

            // When
            boolean created = service.createMenu(menu);

            // Then
            assertThat(created).isTrue();
            verify(sysMenuMapper).insert(menu);
        }

        @Test   
        @DisplayName("创建菜单时，返回false")
        void shouldReturnFalse_whenInsertNotAffected() {
            // Given
            SysMenu menu = MenuTestFactory.buttonMenu(null, "新增用户", 1L, "system:user:create", 1);
            given(sysMenuMapper.insert(menu)).willReturn(0);

            // When
            boolean created = service.createMenu(menu);

            // Then
            assertThat(created).isFalse();
        }
    }
}
