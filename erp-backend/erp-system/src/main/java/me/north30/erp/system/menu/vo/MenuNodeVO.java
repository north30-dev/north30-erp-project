package me.north30.erp.system.menu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 菜单管理树节点响应 VO（接口文档 5.3.1，三级：目录/菜单/按钮，children 递归）。
 *
 * @param id        菜单 ID（sys_menu.id）
 * @param menuName  菜单/权限点名称
 * @param parentId  父级菜单 ID（0 为顶级）
 * @param menuType  类型 1-目录 2-菜单 3-按钮
 * @param path      路由地址
 * @param component 前端组件路径
 * @param perms     权限点标识（如 system:user:create）
 * @param icon      图标
 * @param menuSort  显示顺序
 * @param visible   是否显示 0-隐藏 1-显示
 * @param status    状态 0-停用 1-启用
 * @param children  子节点（同级递归结构，无子节点时为 null）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuNodeVO(

    Long id,

    String menuName,

    Long parentId,

    Integer menuType,

    String path,

    String component,

    String perms,

    String icon,

    Integer menuSort,

    Integer visible,

    Integer status,

    List<MenuNodeVO> children
) {
}
