package me.north30.erp.system.menu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 当前用户菜单树响应 VO（接口文档 4.6，目录/菜单两级，children 递归）。
 */
@Data
public class MenuTreeVO {

    /** 菜单 ID（sys_menu.id） */
    private Long menuId;

    /** 菜单名称 */
    private String menuName;

    /** 类型 1-目录 2-菜单 */
    private Integer menuType;

    /** 父级菜单 ID（0 为顶级） */
    private Long parentId;

    /** 路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 图标 */
    private String icon;

    /** 显示顺序 */
    private Integer menuSort;

    /** 是否显示 0-隐藏 1-显示 */
    private Integer visible;

    /** 子菜单（同级递归结构） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MenuTreeVO> children;
}
