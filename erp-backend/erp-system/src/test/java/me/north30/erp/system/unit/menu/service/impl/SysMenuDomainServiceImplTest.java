package me.north30.erp.system.unit.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.unit.menu.MenuTestFactory;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.menu.service.impl.SysMenuDomainServiceImpl;
import me.north30.erp.system.unit.security.support.MpTableInfoInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysMenuDomainServiceImpl} 纯单元测试：仅 mock Mapper，不连接数据库。
 */
@ExtendWith(MockitoExtension.class)
class SysMenuDomainServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    @InjectMocks
    private SysMenuDomainServiceImpl service;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<SysMenu>> menuWrapperCaptor;

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

    @Nested
    @DisplayName("requirePermsAvailable：权限标识唯一守卫")
    class RequirePermsAvailableTest {

        @Test   
        @DisplayName("权限标识重复时抛出 MENU_PERMS_DUPLICATED")
        void shouldThrow_whenPermsDuplicated() {
            // Given
            given(sysMenuMapper.selectCount(any())).willReturn(1L);

            // When + Then
            assertThatThrownBy(() -> service.requirePermsAvailable("sys:menu:add", null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_PERMS_DUPLICATED.getCode()))
                .hasMessage("权限标识 sys:menu:add 已存在");
        }

        @Test   
        @DisplayName("修改场景排除自身（excludeId 条件写入查询）后无重复时放行")
        void shouldPass_whenExcludeSelfAndNoDuplicate() {
            // Given
            given(sysMenuMapper.selectCount(any())).willReturn(0L);

            // When + Then（不抛异常）
            assertThatCode(() -> service.requirePermsAvailable("sys:menu:add", 1L))
                .doesNotThrowAnyException();
            // 查询参数应携带排除的自身 ID（ne 条件在渲染 SQL 片段时落参）
            verify(sysMenuMapper).selectCount(menuWrapperCaptor.capture());
            LambdaQueryWrapper<SysMenu> wrapper = menuWrapperCaptor.getValue();
            wrapper.getSqlSegment();
            assertThat(wrapper.getParamNameValuePairs().containsValue(1L)).isTrue();
        }

        @Test   
        @DisplayName("perms 为空白时直接放行，不发起唯一性查询")
        void shouldSkipQuery_whenPermsBlank() {
            // When + Then
            assertThatCode(() -> service.requirePermsAvailable("   ", null)).doesNotThrowAnyException();
            assertThatCode(() -> service.requirePermsAvailable(null, 1L)).doesNotThrowAnyException();
            verifyNoInteractions(sysMenuMapper);
        }
    }

    @Nested
    @DisplayName("requireValidParent：父级合法性守卫")
    class RequireValidParentTest {

        @Test   
        @DisplayName("父级为 null（顶级）时直接放行，不查库")
        void shouldSkip_whenParentIdNull() {
            // When + Then（selfId 为 null 表示新增场景，同样跳过防环）
            assertThatCode(() -> service.requireValidParent(null, null)).doesNotThrowAnyException();
            verifyNoInteractions(sysMenuMapper);
        }

        @Test   
        @DisplayName("父级为 0（顶级）时直接放行，不查库")
        void shouldSkip_whenParentIdZero() {
            // When + Then
            assertThatCode(() -> service.requireValidParent(1L, 0L)).doesNotThrowAnyException();
            verifyNoInteractions(sysMenuMapper);
        }

        @Test   
        @DisplayName("父级存在且为目录时放行（新增场景 selfId 为 null，回溯不命中自身）")
        void shouldPass_whenParentExistsAndSelfIdNull() {
            // Given：父级 2（目录，父链 2→1→0）
            SysMenu parent = MenuTestFactory.dirMenu(2L, "基础数据", 1L, 1);
            SysMenu grand = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1);
            given(sysMenuMapper.selectById(2L)).willReturn(parent);
            given(sysMenuMapper.selectById(1L)).willReturn(grand);

            // When + Then
            assertThatCode(() -> service.requireValidParent(null, 2L)).doesNotThrowAnyException();
        }

        @Test   
        @DisplayName("父级不存在时抛出 MENU_NOT_FOUND")
        void shouldThrow_whenParentMissing() {
            // Given
            given(sysMenuMapper.selectById(99L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(null, 99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_NOT_FOUND.getCode()))
                .hasMessage("上级菜单不存在或已删除");
        }

        @Test   
        @DisplayName("父级为按钮时抛出 MENU_LEVEL_EXCEEDED（按钮下禁建子，仅此处判断一次）")
        void shouldThrow_whenParentIsButton() {
            // Given
            given(sysMenuMapper.selectById(9L))
                .willReturn(MenuTestFactory.buttonMenu(9L, "按钮", 3L, "a:b:c", 1));

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(null, 9L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单仅支持三级（目录/菜单/按钮）");
        }

        @Test   
        @DisplayName("父级指向自身时抛出 PARAM_ERROR（自指）")
        void shouldThrow_whenParentIsSelf() {
            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(1L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("父级菜单不能指向自身");
            verifyNoInteractions(sysMenuMapper);
        }

        @Test   
        @DisplayName("父级位于自身下级时抛出 PARAM_ERROR（沿父链回溯命中自身，防环）")
        void shouldThrow_whenParentIsOwnDescendant() {
            // Given：selfId=1，新父级 2 的父链为 2→1（1 是自身下级）
            given(sysMenuMapper.selectById(2L)).willReturn(MenuTestFactory.dirMenu(2L, "子目录", 1L, 1));

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(1L, 2L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("父级菜单不能指向自身或自身下级");
        }
    }
}
