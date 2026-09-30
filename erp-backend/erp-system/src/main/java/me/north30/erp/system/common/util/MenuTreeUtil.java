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
 * <p>Entity → VO 转换委托 {@link MenuConverter}（MapStruct 唯一转换方案）。</p>
 */
@Component
@RequiredArgsConstructor
public class MenuTreeUtil {

    /** 顶级菜单父 ID */
    private static final long ROOT_PARENT_ID = 0L;

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
     * 实体 → 树 VO 转换（委托 MenuConverter）。
     */
    public MenuTreeVO toVO(SysMenu menu) {
        return menuConverter.toTreeVO(menu);
    }
}
