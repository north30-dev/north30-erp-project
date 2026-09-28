package me.north30.erp.system.menu;

import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuTreeQueryDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.vo.MenuTreeVO;

/**
 * 菜单域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class MenuTestFactory {

    /** 菜单类型：目录 */
    private static final int TYPE_DIR = 1;
    /** 菜单类型：菜单 */
    private static final int TYPE_MENU = 2;
    /** 菜单类型：按钮 */
    private static final int TYPE_BUTTON = 3;

    private MenuTestFactory() {
    }

    /**
     * 构建菜单实体（默认显示、启用）。
     */
    public static SysMenu menu(Long id, String menuName, Long parentId, Integer menuType,
                               String path, String component, String perms, Integer menuSort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setMenuName(menuName);
        menu.setParentId(parentId);
        menu.setMenuType(menuType);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerms(perms);
        menu.setMenuSort(menuSort);
        menu.setVisible(1);
        menu.setStatus(1);
        return menu;
    }

    /**
     * 目录型菜单实体（menuType=1，无组件与权限点）。
     */
    public static SysMenu dirMenu(Long id, String menuName, Long parentId, Integer menuSort) {
        return menu(id, menuName, parentId, TYPE_DIR, "/" + menuName, null, null, menuSort);
    }

    /**
     * 页面型菜单实体（menuType=2，含组件与权限点）。
     */
    public static SysMenu pageMenu(Long id, String menuName, Long parentId, String component,
                                   String perms, Integer menuSort) {
        return menu(id, menuName, parentId, TYPE_MENU, "/" + menuName, component, perms, menuSort);
    }

    /**
     * 按钮型菜单实体（menuType=3，仅权限点）。
     */
    public static SysMenu buttonMenu(Long id, String menuName, Long parentId, String perms, Integer menuSort) {
        return menu(id, menuName, parentId, TYPE_BUTTON, null, null, perms, menuSort);
    }

    /**
     * 新增菜单请求 DTO（icon/visible/remark 固定为 null 以触发服务端默认值）。
     */
    public static MenuCreateDTO createDTO(String menuName, Long parentId, Integer menuType, String path,
                                          String component, String perms, Integer menuSort, Integer status) {
        return new MenuCreateDTO(menuName, parentId, menuType, path, component, perms, null, menuSort, null, status, null);
    }

    /**
     * 新增目录请求 DTO（menuType=1）。
     */
    public static MenuCreateDTO dirCreateDTO(String menuName, Long parentId, String path, Integer menuSort) {
        return createDTO(menuName, parentId, TYPE_DIR, path, null, null, menuSort, null);
    }

    /**
     * 新增按钮请求 DTO（menuType=3）。
     */
    public static MenuCreateDTO buttonCreateDTO(String menuName, Long parentId, String perms, Integer menuSort) {
        return createDTO(menuName, parentId, TYPE_BUTTON, null, null, perms, menuSort, null);
    }

    /**
     * 修改菜单请求 DTO（全字段，icon/visible/status/remark 固定为 null 表示不修改）。
     */
    public static MenuUpdateDTO updateDTO(Integer version, String menuName, Long parentId, Integer menuType,
                                          String path, String component, String perms, Integer menuSort) {
        return new MenuUpdateDTO(menuName, parentId, menuType, path, component, perms, null, menuSort, null, null, null, version);
    }

    /**
     * 最小修改请求 DTO（仅携带乐观锁版本号，其余字段不修改）。
     */
    public static MenuUpdateDTO updateDTO(Integer version) {
        return updateDTO(version, null, null, null, null, null, null, null);
    }

    /**
     * 菜单树查询请求 DTO。
     */
    public static MenuTreeQueryDTO treeQueryDTO(String menuName, String perms, Integer status) {
        return new MenuTreeQueryDTO(menuName, perms, status);
    }

    /**
     * 菜单树节点 VO（MenuTreeUtil 测试用）。
     */
    public static MenuTreeVO treeVO(Long menuId, Long parentId, Integer menuSort) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setMenuId(menuId);
        vo.setMenuName("菜单" + menuId);
        vo.setMenuType(TYPE_MENU);
        vo.setParentId(parentId);
        vo.setPath("/menu" + menuId);
        vo.setMenuSort(menuSort);
        vo.setVisible(1);
        return vo;
    }
}
