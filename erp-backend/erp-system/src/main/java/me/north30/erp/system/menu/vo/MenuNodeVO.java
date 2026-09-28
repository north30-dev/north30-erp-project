package me.north30.erp.system.menu.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 菜单管理树节点响应 VO（接口文档 5.3.1，三级：目录/菜单/按钮，children 递归）。
 */
@Data
public class MenuNodeVO {

    /** 菜单 ID（sys_menu.id） */
    private Long id;

    /** 菜单/权限点名称 */
    private String menuName;

    /** 父级菜单 ID（0 为顶级） */
    private Long parentId;

    /** 类型 1-目录 2-菜单 3-按钮 */
    private Integer menuType;

    /** 路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限点标识（如 system:user:create） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 显示顺序 */
    private Integer menuSort;

    /** 是否显示 0-隐藏 1-显示 */
    private Integer visible;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 子节点（同级递归结构） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MenuNodeVO> children;
}
