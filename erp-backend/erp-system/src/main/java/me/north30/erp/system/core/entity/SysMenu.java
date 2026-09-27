package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 菜单与按钮权限点表（sys_menu，三级：目录/菜单/按钮）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

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

    /** 权限点标识（如 sales:order:approve） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 显示顺序 */
    private Integer menuSort;

    /** 是否显示 0-隐藏 1-显示 */
    private Integer visible;

    /** 状态 0-停用 1-启用 */
    private Integer status;
}
