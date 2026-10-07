package me.north30.erp.system.menu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 当前用户菜单树响应 VO（接口文档 4.6，目录/菜单两级，children 递归）。
 *
 * @param menuId    菜单 ID（sys_menu.id）
 * @param menuName  菜单名称
 * @param menuType  类型 1-目录 2-菜单
 * @param parentId  父级菜单 ID（0 为顶级）
 * @param path      路由地址
 * @param component 前端组件路径
 * @param icon      图标
 * @param menuSort  显示顺序
 * @param visible   是否显示 0-隐藏 1-显示
 * @param children  子菜单（同级递归结构，无子节点时为 null）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuTreeVO(

    Long menuId,

    String menuName,

    Integer menuType,

    Long parentId,

    String path,

    String component,

    String icon,

    Integer menuSort,

    Integer visible,

    List<MenuTreeVO> children
) {
}
