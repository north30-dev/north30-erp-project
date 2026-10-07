package me.north30.erp.system.menu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 当前用户菜单树响应 VO（接口文档 4.6，目录/菜单两级，children 递归）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuTreeVO(

    /** 菜单 ID（sys_menu.id） */
    Long menuId,

    /** 菜单名称 */
    String menuName,

    /** 类型 1-目录 2-菜单 */
    Integer menuType,

    /** 父级菜单 ID（0 为顶级） */
    Long parentId,

    /** 路由地址 */
    String path,

    /** 前端组件路径 */
    String component,

    /** 图标 */
    String icon,

    /** 显示顺序 */
    Integer menuSort,

    /** 是否显示 0-隐藏 1-显示 */
    Integer visible,

    /** 子菜单（同级递归结构，无子节点时为 null） */
    List<MenuTreeVO> children
) {
}
