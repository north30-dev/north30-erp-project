package me.north30.erp.system.common.util;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.menu.converter.MenuConverter;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单树构建工具：一次查出菜单后内存组树（禁止循环内查库）。
 * <p>MenuTreeVO 为不可变 record，组树采用自底向上递归构造，同级按 menuSort 升序排列。</p>
 * <p>Entity → VO 转换委托 {@link MenuConverter}（MapStruct 唯一转换方案）。</p>
 */
@Component
@RequiredArgsConstructor
public class MenuTreeUtil {

    /** 顶级菜单父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private static final Comparator<MenuTreeVO> MENU_SORT_COMPARATOR =
        Comparator.comparing(MenuTreeVO::menuSort, Comparator.nullsLast(Comparator.naturalOrder()));

    private final MenuConverter menuConverter;

    /**
     * 将平铺菜单列表组装为树（parentId=0 为根节点），同级按 menuSort 升序排列。
     *
     * @param menus 平铺菜单 VO 列表
     * @return 树形菜单列表（无子节点时 children 为 null）
     */
    public List<MenuTreeVO> buildTree(List<MenuTreeVO> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<MenuTreeVO>> childrenMap = menus.stream()
            .filter(menu -> menu.parentId() != null && menu.parentId() != ROOT_PARENT_ID)
            .collect(Collectors.groupingBy(MenuTreeVO::parentId));
        return menus.stream()
            .filter(menu -> menu.parentId() == null || menu.parentId() == ROOT_PARENT_ID)
            .sorted(MENU_SORT_COMPARATOR)
            .map(menu -> attachChildren(menu, childrenMap))
            .collect(Collectors.toList());
    }

    /**
     * 自底向上递归装配子树（record 不可变，需逐层新建节点）。
     */
    private static MenuTreeVO attachChildren(MenuTreeVO menu, Map<Long, List<MenuTreeVO>> childrenMap) {
        List<MenuTreeVO> children = childrenMap.get(menu.menuId());
        if (children == null) {
            return menu;
        }
        List<MenuTreeVO> attached = children.stream()
            .sorted(MENU_SORT_COMPARATOR)
            .map(child -> attachChildren(child, childrenMap))
            .collect(Collectors.toList());
        return new MenuTreeVO(menu.menuId(), menu.menuName(), menu.menuType(), menu.parentId(),
            menu.path(), menu.component(), menu.icon(), menu.menuSort(), menu.visible(), attached);
    }

    /**
     * 实体 → 树 VO 转换（委托 MenuConverter）。
     */
    public MenuTreeVO toVO(SysMenu menu) {
        return menuConverter.toTreeVO(menu);
    }
}
