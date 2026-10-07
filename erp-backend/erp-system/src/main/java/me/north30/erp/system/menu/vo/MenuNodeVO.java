package me.north30.erp.system.menu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 菜单管理树节点响应 VO（接口文档 5.3.1，三级：目录/菜单/按钮，children 递归）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuNodeVO(

    /** 菜单 ID（sys_menu.id） */
    Long id,

    /** 菜单/权限点名称 */
    String menuName,

    /** 父级菜单 ID（0 为顶级） */
    Long parentId,

    /** 类型 1-目录 2-菜单 3-按钮 */
    Integer menuType,

    /** 路由地址 */
    String path,

    /** 前端组件路径 */
    String component,

    /** 权限点标识（如 system:user:create） */
    String perms,

    /** 图标 */
    String icon,

    /** 显示顺序 */
    Integer menuSort,

    /** 是否显示 0-隐藏 1-显示 */
    Integer visible,

    /** 状态 0-停用 1-启用 */
    Integer status,

    /** 子节点（同级递归结构，无子节点时为 null） */
    List<MenuNodeVO> children
) {
}
