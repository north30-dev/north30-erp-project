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
    @DisplayName("requireValidParent：父级结构守卫（存在性 + 父子类型配对）")
    class RequireValidParentTest {

        @Test
        @DisplayName("父级为 null（顶级）建目录/菜单时放行，不查库；建按钮抛 10001")
        void shouldValidateRootChildType_whenParentIdNull() {
            assertThatCode(() -> service.requireValidParent(null, SysMenu.TYPE_DIR)).doesNotThrowAnyException();
            assertThatCode(() -> service.requireValidParent(null, SysMenu.TYPE_MENU)).doesNotThrowAnyException();
            assertThatThrownBy(() -> service.requireValidParent(null, SysMenu.TYPE_BUTTON))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("顶级仅支持目录或菜单类型");
            verifyNoInteractions(sysMenuMapper);
        }

        @Test
        @DisplayName("父级为 0（顶级）建按钮时抛 10001，不查库")
        void shouldThrow_whenParentIdZeroAndChildIsButton() {
            assertThatThrownBy(() -> service.requireValidParent(0L, SysMenu.TYPE_BUTTON))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("顶级仅支持目录或菜单类型");
            verifyNoInteractions(sysMenuMapper);
        }

        @Test
        @DisplayName("目录父下建目录/菜单放行")
        void shouldPass_whenDirParentWithDirOrMenuChild() {
            // Given：父级 2（目录）
            given(sysMenuMapper.selectById(2L)).willReturn(MenuTestFactory.dirMenu(2L, "基础数据", 1L, 1));

            // When + Then
            assertThatCode(() -> service.requireValidParent(2L, SysMenu.TYPE_DIR)).doesNotThrowAnyException();
            assertThatCode(() -> service.requireValidParent(2L, SysMenu.TYPE_MENU)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("菜单父下建菜单/按钮放行")
        void shouldPass_whenMenuParentWithMenuOrButtonChild() {
            // Given：父级 2（菜单）
            given(sysMenuMapper.selectById(2L))
                .willReturn(MenuTestFactory.pageMenu(2L, "用户管理", 1L, "system/user/index", "system:user:list", 1));

            // When + Then
            assertThatCode(() -> service.requireValidParent(2L, SysMenu.TYPE_MENU)).doesNotThrowAnyException();
            assertThatCode(() -> service.requireValidParent(2L, SysMenu.TYPE_BUTTON)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("父级不存在时抛出 MENU_NOT_FOUND")
        void shouldThrow_whenParentMissing() {
            // Given
            given(sysMenuMapper.selectById(99L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(99L, SysMenu.TYPE_DIR))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_NOT_FOUND.getCode()))
                .hasMessage("上级菜单不存在或已删除");
        }

        @Test
        @DisplayName("父级为按钮时抛出 18023（按钮下不可建子）")
        void shouldThrow_whenParentIsButton() {
            // Given
            given(sysMenuMapper.selectById(9L))
                .willReturn(MenuTestFactory.buttonMenu(9L, "按钮", 3L, "system:user:create", 1));

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(9L, SysMenu.TYPE_DIR))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单层级超过上限（最多 4 级）");
        }

        @Test
        @DisplayName("目录父下建按钮抛 10001（按钮仅可挂菜单下）")
        void shouldThrow_whenDirParentWithButtonChild() {
            // Given
            given(sysMenuMapper.selectById(2L)).willReturn(MenuTestFactory.dirMenu(2L, "系统管理", 0L, 1));

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(2L, SysMenu.TYPE_BUTTON))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("目录下仅支持目录或菜单类型");
        }

        @Test
        @DisplayName("菜单父下建目录抛 10001（类型倒挂）")
        void shouldThrow_whenMenuParentWithDirChild() {
            // Given
            given(sysMenuMapper.selectById(2L))
                .willReturn(MenuTestFactory.pageMenu(2L, "用户管理", 1L, "system/user/index", "system:user:list", 1));

            // When + Then
            assertThatThrownBy(() -> service.requireValidParent(2L, SysMenu.TYPE_DIR))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("菜单下仅支持菜单或按钮类型");
        }
    }

    @Nested
    @DisplayName("requireNotDescendant：防环守卫（update 换父级）")
    class RequireNotDescendantTest {

        @Test
        @DisplayName("新父级为自身时抛出 PARAM_ERROR")
        void shouldThrow_whenNewParentIsSelf() {
            // Given：全量菜单仅自身
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "自身", 0L, 1)));

            // When + Then
            assertThatThrownBy(() -> service.requireNotDescendant(1L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("父级菜单不能指向自身或自身下级");
        }

        @Test
        @DisplayName("新父级为自身子孙（沿父链回溯命中自身）时抛出 PARAM_ERROR")
        void shouldThrow_whenNewParentIsOwnDescendant() {
            // Given：selfId=1，新父级 2 的父链为 2→1
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "根目录", 0L, 1),
                    MenuTestFactory.dirMenu(2L, "子目录", 1L, 1)));

            // When + Then
            assertThatThrownBy(() -> service.requireNotDescendant(1L, 2L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("父级菜单不能指向自身或自身下级");
        }

        @Test
        @DisplayName("新父级为其他分支节点时放行")
        void shouldPass_whenNewParentNotDescendant() {
            // Given：selfId=3，新父级 2 的父链为 2→0，不命中自身
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "根目录", 0L, 1),
                    MenuTestFactory.dirMenu(2L, "其他目录", 0L, 2),
                    MenuTestFactory.dirMenu(3L, "自身", 1L, 1)));

            // When + Then
            assertThatCode(() -> service.requireNotDescendant(3L, 2L)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("requireDepthWithinLimit：深度守卫（新增场景）")
    class RequireDepthWithinLimitTest {

        @Test
        @DisplayName("顶级新增不查库，直接放行")
        void shouldSkip_whenParentIdRoot() {
            assertThatCode(() -> service.requireDepthWithinLimit(null, SysMenu.TYPE_DIR)).doesNotThrowAnyException();
            assertThatCode(() -> service.requireDepthWithinLimit(0L, SysMenu.TYPE_MENU)).doesNotThrowAnyException();
            verifyNoInteractions(sysMenuMapper);
        }

        @Test
        @DisplayName("第 3 层父级下新增放行（4 ≤ 4）")
        void shouldPass_whenParentAtDepth3() {
            // Given：1→2→3 三层链（父级 3 深度为 3，新增后深度 4）
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "一层", 0L, 1),
                    MenuTestFactory.dirMenu(2L, "二层", 1L, 1),
                    MenuTestFactory.dirMenu(3L, "三层", 2L, 1)));

            // When + Then
            assertThatCode(() -> service.requireDepthWithinLimit(3L, SysMenu.TYPE_BUTTON)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("第 4 层父级下新增被拒（5 > 4，抛 18023）")
        void shouldThrow_whenParentAtDepth4() {
            // Given：1→2→3→4 四层链（父级 4 深度为 4，新增后深度 5）
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "一层", 0L, 1),
                    MenuTestFactory.dirMenu(2L, "二层", 1L, 1),
                    MenuTestFactory.dirMenu(3L, "三层", 2L, 1),
                    MenuTestFactory.dirMenu(4L, "四层", 3L, 1)));

            // When + Then
            assertThatThrownBy(() -> service.requireDepthWithinLimit(4L, SysMenu.TYPE_BUTTON))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单层级超过上限（最多 4 级）");
        }
    }

    @Nested
    @DisplayName("requireSubtreeDepthWithinLimit：子树深度守卫（update 换父级）")
    class RequireSubtreeDepthWithinLimitTest {

        @Test
        @DisplayName("新父级深度 3 + 子树高度 2 = 5 > 4，抛 18023")
        void shouldThrow_whenSubtreeExceedsLimit() {
            // Given：新父级 3 链 1→2→3（深度 3）；自身 9（子树 9→10，高度 2）
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "一层", 0L, 1),
                    MenuTestFactory.dirMenu(2L, "二层", 1L, 1),
                    MenuTestFactory.dirMenu(3L, "三层", 2L, 1),
                    MenuTestFactory.dirMenu(9L, "自身", 0L, 1),
                    MenuTestFactory.buttonMenu(10L, "子按钮", 9L, "system:a:create", 1)));

            // When + Then
            assertThatThrownBy(() -> service.requireSubtreeDepthWithinLimit(9L, 3L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单层级超过上限（最多 4 级）");
        }

        @Test
        @DisplayName("新父级深度 3 + 子树高度 1 = 4，放行")
        void shouldPass_whenSubtreeAtLimit() {
            // Given：新父级 3 链 1→2→3（深度 3）；自身 9 无子节点（高度 1）
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(1L, "一层", 0L, 1),
                    MenuTestFactory.dirMenu(2L, "二层", 1L, 1),
                    MenuTestFactory.dirMenu(3L, "三层", 2L, 1),
                    MenuTestFactory.dirMenu(9L, "自身", 0L, 1)));

            // When + Then
            assertThatCode(() -> service.requireSubtreeDepthWithinLimit(9L, 3L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("新父级为顶级时按深度 0 计算，子树高度 4 放行")
        void shouldPass_whenNewParentIsRoot() {
            // Given：自身 9 子树 9→10→11→12（高度 4），移到顶级后深度恰为 4
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(9L, "自身", 1L, 1),
                    MenuTestFactory.dirMenu(10L, "二层", 9L, 1),
                    MenuTestFactory.dirMenu(11L, "三层", 10L, 1),
                    MenuTestFactory.buttonMenu(12L, "四层", 11L, "system:a:create", 1)));

            // When + Then
            assertThatCode(() -> service.requireSubtreeDepthWithinLimit(9L, 0L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("新父级不存在时按深度 0 处理，放行")
        void shouldPass_whenNewParentMissing() {
            // Given：映射中无新父级 99
            given(sysMenuMapper.selectList(any())).willReturn(
                List.of(MenuTestFactory.dirMenu(9L, "自身", 0L, 1)));

            // When + Then
            assertThatCode(() -> service.requireSubtreeDepthWithinLimit(9L, 99L)).doesNotThrowAnyException();
        }
    }
}
