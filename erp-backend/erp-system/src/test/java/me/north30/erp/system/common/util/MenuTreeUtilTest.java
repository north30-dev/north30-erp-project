package me.north30.erp.system.common.util;

import me.north30.erp.system.menu.MenuTestFactory;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.vo.MenuTreeVO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MenuTreeUtil} 纯工具类测试：平铺列表组树、排序与 VO 转换。
 */
class MenuTreeUtilTest {

    @Nested
    @DisplayName("buildTree：构建菜单树")
    class BuildTreeTest {

        @Test
        @DisplayName("输入 null 时返回空树")
        void shouldReturnEmpty_whenInputNull() {
            // When + Then
            assertThat(MenuTreeUtil.buildTree(null)).isEmpty();
        }

        @Test
        @DisplayName("输入空列表时返回空树")
        void shouldReturnEmpty_whenInputEmpty() {
            // When + Then
            assertThat(MenuTreeUtil.buildTree(List.of())).isEmpty();
        }

        @Test
        @DisplayName("输入平铺列表时，按 menuSort 升序构建树")
        void shouldBuildHierarchySortedBySort_whenFlatListGiven() {
            // Given：两级结构，同级按 menuSort 升序，null 排序值排最后
            MenuTreeVO rootA = MenuTestFactory.treeVO(1L, 0L, 2);
            MenuTreeVO rootB = MenuTestFactory.treeVO(2L, 0L, 1);
            MenuTreeVO rootNoSort = MenuTestFactory.treeVO(3L, 0L, null);
            MenuTreeVO childA1 = MenuTestFactory.treeVO(4L, 1L, 1);
            MenuTreeVO childA2 = MenuTestFactory.treeVO(5L, 1L, 2);
            MenuTreeVO grandChild = MenuTestFactory.treeVO(6L, 4L, 1);

            // When
            List<MenuTreeVO> tree = MenuTreeUtil.buildTree(
                List.of(rootA, rootB, rootNoSort, childA1, childA2, grandChild));

            // Then：根节点 [sort1, sort2, null]，子节点正确挂载
            assertThat(tree).extracting(MenuTreeVO::getMenuId).containsExactly(2L, 1L, 3L);
            assertThat(tree.get(1).getChildren())
                .extracting(MenuTreeVO::getMenuId)
                .containsExactly(4L, 5L);
            assertThat(tree.get(1).getChildren().get(0).getChildren())
                .extracting(MenuTreeVO::getMenuId)
                .containsExactly(6L);
            assertThat(tree.get(0).getChildren()).isNull();
        }

        @Test
        @DisplayName("输入平铺列表时，孤儿节点被丢弃")
        void shouldDropOrphan_whenParentMissing() {
            // Given：父节点不在结果集中且自身无子节点（与 DeptTreeUtil 行为不同）
            MenuTreeVO root = MenuTestFactory.treeVO(1L, 0L, 1);
            MenuTreeVO orphan = MenuTestFactory.treeVO(9L, 99L, 1);

            // When
            List<MenuTreeVO> tree = MenuTreeUtil.buildTree(List.of(root, orphan));

            // Then：孤儿节点被丢弃，不出现在根列表中
            assertThat(tree).extracting(MenuTreeVO::getMenuId).containsExactly(1L);
        }
    }

    @Nested
    @DisplayName("toVO：将菜单实体转换为菜单树节点")
    class ToVOTest {

        @Test
        @DisplayName("将菜单实体转换为菜单树节点，字段一一对应")
        void shouldMapEntityFieldsToVO() {
            // Given
            SysMenu menu = MenuTestFactory.pageMenu(1L, "用户管理", 2L, "system/user/index", "system:user:list", 3);

            // When
            MenuTreeVO vo = MenuTreeUtil.toVO(menu);

            // Then：字段一一对应（perms 不在树 VO 范围）
            assertThat(vo.getMenuId()).isEqualTo(1L);
            assertThat(vo.getMenuName()).isEqualTo("用户管理");
            assertThat(vo.getMenuType()).isEqualTo(2);
            assertThat(vo.getParentId()).isEqualTo(2L);
            assertThat(vo.getPath()).isEqualTo("/用户管理");
            assertThat(vo.getComponent()).isEqualTo("system/user/index");
            assertThat(vo.getMenuSort()).isEqualTo(3);
            assertThat(vo.getVisible()).isEqualTo(1);
        }
    }
}
