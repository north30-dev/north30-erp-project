package me.north30.erp.system.core.util;

import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.vo.MenuTreeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MenuTreeUtil 单元测试：树构建、同级排序、叶子节点 children 为 null、VO 映射。
 */
class MenuTreeUtilTest {

    private MenuTreeVO vo(Long id, Long parentId, Integer menuType, Integer menuSort, String menuName) {
        MenuTreeVO node = new MenuTreeVO();
        node.setMenuId(id);
        node.setParentId(parentId);
        node.setMenuType(menuType);
        node.setMenuSort(menuSort);
        node.setMenuName(menuName);
        return node;
    }

    @Test
    @DisplayName("树构建：根节点唯一、同级按 menuSort 升序、二级挂载正确")
    void buildTreeSortsAndNests() {
        MenuTreeVO dir = vo(10L, 0L, 1, 2, "系统管理");
        MenuTreeVO roleMenu = vo(12L, 10L, 2, 0, "角色管理");
        MenuTreeVO userMenu = vo(11L, 10L, 2, 1, "用户管理");
        MenuTreeVO button = vo(13L, 11L, 3, 0, "用户新增");

        List<MenuTreeVO> tree = MenuTreeUtil.buildTree(List.of(userMenu, roleMenu, dir, button));

        assertEquals(1, tree.size());
        MenuTreeVO root = tree.get(0);
        assertEquals("系统管理", root.getMenuName());
        assertEquals(2, root.getChildren().size());
        assertEquals("角色管理", root.getChildren().get(0).getMenuName());
        assertEquals("用户管理", root.getChildren().get(1).getMenuName());
        assertEquals(1, root.getChildren().get(1).getChildren().size());
        assertEquals("用户新增", root.getChildren().get(1).getChildren().get(0).getMenuName());
    }

    @Test
    @DisplayName("无子节点的菜单 children 为 null")
    void leafChildrenIsNull() {
        MenuTreeVO dir = vo(10L, 0L, 1, 1, "系统管理");
        MenuTreeVO leaf = vo(11L, 10L, 2, 1, "登录日志");

        List<MenuTreeVO> tree = MenuTreeUtil.buildTree(List.of(dir, leaf));

        assertNull(tree.get(0).getChildren().get(0).getChildren());
    }

    @Test
    @DisplayName("空列表构建结果为空")
    void buildTreeEmptyReturnsEmpty() {
        assertTrue(MenuTreeUtil.buildTree(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Entity 转 VO 字段映射")
    void toVOMapsFields() {
        SysMenu menu = new SysMenu();
        menu.setId(1L);
        menu.setMenuName("用户管理");
        menu.setParentId(0L);
        menu.setMenuType(2);
        menu.setPath("/system/user");
        menu.setComponent("system/user/index");
        menu.setIcon("User");
        menu.setMenuSort(1);
        menu.setVisible(1);

        MenuTreeVO vo = MenuTreeUtil.toVO(menu);

        assertEquals(1L, vo.getMenuId());
        assertEquals("用户管理", vo.getMenuName());
        assertEquals(2, vo.getMenuType());
        assertEquals("/system/user", vo.getPath());
        assertEquals("system/user/index", vo.getComponent());
        assertEquals("User", vo.getIcon());
        assertEquals(1, vo.getMenuSort());
        assertEquals(1, vo.getVisible());
        assertNull(vo.getChildren());
    }
}
