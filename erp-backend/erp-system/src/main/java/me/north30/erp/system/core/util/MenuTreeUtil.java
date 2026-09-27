package me.north30.erp.system.core.util;

import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.vo.MenuTreeVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单树构建工具：一次查出菜单后内存组树（禁止循环内查库）。
 */
public final class MenuTreeUtil {

    /** 顶级菜单父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private MenuTreeUtil() {
    }

    /**
     * 将平铺菜单列表组装为树（parentId=0 为根节点），同级按 menuSort 升序排列。
     *
     * @param menus 平铺菜单 VO 列表
     * @return 树形菜单列表（无子节点时 children 为 null）
     */
    public static List<MenuTreeVO> buildTree(List<MenuTreeVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<MenuTreeVO>> childrenMap = menus.stream()
            .filter(menu -> menu.getParentId() != null && menu.getParentId() != ROOT_PARENT_ID)
            .collect(Collectors.groupingBy(MenuTreeVO::getParentId));
        for (MenuTreeVO menu : menus) {
            List<MenuTreeVO> children = childrenMap.get(menu.getMenuId());
            if (children != null) {
                children.sort(Comparator.comparing(MenuTreeVO::getMenuSort,
                    Comparator.nullsLast(Comparator.naturalOrder())));
                menu.setChildren(children);
            }
        }
        return menus.stream()
            .filter(menu -> menu.getParentId() == null || menu.getParentId() == ROOT_PARENT_ID)
            .sorted(Comparator.comparing(MenuTreeVO::getMenuSort,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    /**
     * Entity → VO 转换。
     */
    public static MenuTreeVO toVO(SysMenu menu) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setMenuId(menu.getId());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuType(menu.getMenuType());
        vo.setParentId(menu.getParentId());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setMenuSort(menu.getMenuSort());
        vo.setVisible(menu.getVisible());
        return vo;
    }
}
