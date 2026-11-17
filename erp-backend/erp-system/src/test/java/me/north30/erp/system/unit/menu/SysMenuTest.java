package me.north30.erp.system.unit.menu;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.menu.entity.SysMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SysMenu 实体级充血校验单测（requireTypeFieldsValid / clearRouteFieldsIfButton）。
 * <p>覆盖 path/component/perms 三条格式正则的正反用例、trim 归一化写回、
 * 按类型区分的必填字段（阶段二第 3 步）。</p>
 */
@DisplayName("SysMenu 实体格式校验单测")
class SysMenuTest {

    /** 构建指定类型菜单 */
    private SysMenu menu(int menuType, String path, String component, String perms) {
        SysMenu menu = new SysMenu();
        menu.setMenuType(menuType);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerms(perms);
        return menu;
    }

    @Nested
    @DisplayName("合法格式")
    class ValidFormatTest {

        @Test
        @DisplayName("目录：合法 path 通过，且 trim 后写回")
        void shouldPassAndTrim_whenDirPathValid() {
            // Given：path 带首尾空格
            SysMenu menu = menu(SysMenu.TYPE_DIR, " /system/menu ", null, null);

            // When + Then
            assertThatCode(menu::requireTypeFieldsValid).doesNotThrowAnyException();
            assertThat(menu.getPath()).isEqualTo("/system/menu");
        }

        @Test
        @DisplayName("菜单：合法 path + 小写 component 通过")
        void shouldPass_whenMenuPathAndLowercaseComponentValid() {
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "system/user/index", null);
            assertThatCode(menu::requireTypeFieldsValid).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("菜单：大驼峰 component 通过（与前端页面组件命名规范一致）")
        void shouldPass_whenComponentCamelCase() {
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/login-log", "system/loginLog/index", null);
            assertThatCode(menu::requireTypeFieldsValid).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("按钮：合法 perms 通过，且 trim 后写回")
        void shouldPassAndTrim_whenButtonPermsValid() {
            SysMenu menu = menu(SysMenu.TYPE_BUTTON, null, null, " system:menu:create ");
            assertThatCode(menu::requireTypeFieldsValid).doesNotThrowAnyException();
            assertThat(menu.getPerms()).isEqualTo("system:menu:create");
        }
    }

    @Nested
    @DisplayName("非法格式")
    class InvalidFormatTest {

        @Test
        @DisplayName("menuType 非法，抛 10001")
        void shouldThrow_whenMenuTypeInvalid() {
            SysMenu menu = menu(9, "/system/menu", null, null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("菜单类型非法：9");
        }

        @Test
        @DisplayName("目录 path 为空，抛 10001")
        void shouldThrow_whenDirPathBlank() {
            SysMenu menu = menu(SysMenu.TYPE_DIR, "  ", null, null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("路由地址不能为空");
        }

        @Test
        @DisplayName("path 缺少开头斜杠，抛 10001 并带示例")
        void shouldThrow_whenPathWithoutLeadingSlash() {
            SysMenu menu = menu(SysMenu.TYPE_DIR, "system/menu", null, null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("路由地址格式不正确：system/menu，正确格式如 /system/menu");
        }

        @Test
        @DisplayName("path 含大写字母，抛 10001")
        void shouldThrow_whenPathWithUppercase() {
            SysMenu menu = menu(SysMenu.TYPE_DIR, "/System/Menu", null, null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("路由地址格式不正确");
        }

        @Test
        @DisplayName("component 以 .vue 结尾，抛 10001")
        void shouldThrow_whenComponentWithVueSuffix() {
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "system/user.vue", null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("前端组件路径格式不正确：system/user.vue，正确格式如 system/menu/index");
        }

        @Test
        @DisplayName("component 单级（无层级分隔），抛 10001")
        void shouldThrow_whenComponentSingleSegment() {
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "user", null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前端组件路径格式不正确");
        }

        @Test
        @DisplayName("component 以 / 开头，抛 10001")
        void shouldThrow_whenComponentWithLeadingSlash() {
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "/system/user/index", null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前端组件路径格式不正确");
        }

        @Test
        @DisplayName("perms 两段式，抛 10001")
        void shouldThrow_whenPermsTwoSegments() {
            SysMenu menu = menu(SysMenu.TYPE_BUTTON, null, null, "system:create");
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("权限标识格式不正确：system:create，正确格式如 system:menu:create");
        }

        @Test
        @DisplayName("perms 含大写，抛 10001")
        void shouldThrow_whenPermsWithUppercase() {
            SysMenu menu = menu(SysMenu.TYPE_BUTTON, null, null, "System:Menu:Create");
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限标识格式不正确");
        }
    }

    @Nested
    @DisplayName("按类型区分的必填字段")
    class TypeSpecificRequiredTest {

        @Test
        @DisplayName("按钮类型不校验 path/component（垃圾值放行，由 clearRouteFieldsIfButton 清空）")
        void shouldSkipPathAndComponent_whenButton() {
            SysMenu menu = menu(SysMenu.TYPE_BUTTON, "garbage-path", "garbage.vue", "system:menu:create");
            assertThatCode(menu::requireTypeFieldsValid).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("目录类型不校验 component（垃圾值放行，目录无页面）")
        void shouldSkipComponent_whenDir() {
            SysMenu menu = menu(SysMenu.TYPE_DIR, "/system/dir", "garbage.vue", null);
            assertThatCode(menu::requireTypeFieldsValid).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("菜单类型缺 component，抛 10001")
        void shouldThrow_whenMenuComponentBlank() {
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", null, null);
            assertThatThrownBy(menu::requireTypeFieldsValid)
                .isInstanceOf(BusinessException.class)
                .hasMessage("前端组件路径不能为空");
        }

        @Test
        @DisplayName("clearRouteFieldsIfButton：按钮清空 path/component，目录/菜单保留")
        void shouldClearRouteFieldsOnly_whenButton() {
            // Given：按钮
            SysMenu button = menu(SysMenu.TYPE_BUTTON, "/a/b", "a/b/index", null);
            button.clearRouteFieldsIfButton();
            assertThat(button.getPath()).isNull();
            assertThat(button.getComponent()).isNull();

            // Given：菜单保留
            SysMenu page = menu(SysMenu.TYPE_MENU, "/system/user", "system/user/index", null);
            page.clearRouteFieldsIfButton();
            assertThat(page.getPath()).isEqualTo("/system/user");
            assertThat(page.getComponent()).isEqualTo("system/user/index");
        }
    }

    @Nested
    @DisplayName("状态变更守卫（changeStatus）")
    class ChangeStatusTest {

        @Test
        @DisplayName("普通菜单停用：放行并写回 status=0")
        void shouldAllowDisable_whenNormalMenu() {
            // Given
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "system/user/index", null);
            menu.setIsSystem(0);

            // When
            menu.changeStatus(0);

            // Then
            assertThat(menu.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("内置菜单停用：抛 18039")
        void shouldThrow18039_whenDisableSystemMenu() {
            // Given
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "system/user/index", null);
            menu.setIsSystem(1);

            // When + Then
            assertThatThrownBy(() -> menu.changeStatus(0))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18039));
        }

        @Test
        @DisplayName("内置菜单启用：放行")
        void shouldAllowEnable_whenSystemMenu() {
            // Given
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "system/user/index", null);
            menu.setIsSystem(1);
            menu.setStatus(0);

            // When
            menu.changeStatus(1);

            // Then
            assertThat(menu.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("非法状态值：抛 10001")
        void shouldThrowParamError_whenStatusInvalid() {
            // Given
            SysMenu menu = menu(SysMenu.TYPE_MENU, "/system/user", "system/user/index", null);

            // When + Then
            assertThatThrownBy(() -> menu.changeStatus(2))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }
}
