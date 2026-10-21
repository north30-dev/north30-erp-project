package me.north30.erp.system.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改菜单请求 DTO（接口文档 5.3.3，乐观锁 version 必填，parentId 不得指向自身或自身下级）。
 *
 * @param menuName  菜单名称
 * @param parentId  父级菜单 ID（0 为顶级）
 * @param menuType  菜单类型 1-目录 2-菜单 3-按钮
 * @param path      路由地址
 * @param component 前端组件路径
 * @param perms     权限标识
 * @param icon      图标
 * @param menuSort  显示顺序
 * @param visible   是否显示 0-隐藏 1-显示
 * @param status    状态 0-停用 1-启用
 * @param remark    备注
 * @param version   乐观锁版本号
 */
public record MenuUpdateDTO(

    @Size(max = 100, message = "菜单名称长度不能超过 100")
    String menuName,

    Long parentId,

    Integer menuType,

    @Size(max = 200, message = "路由地址长度不能超过 200")
    String path,

    @Size(max = 200, message = "前端组件路径长度不能超过 200")
    String component,

    @Size(max = 100, message = "权限标识长度不能超过 100")
    String perms,

    @Size(max = 100, message = "图标长度不能超过 100")
    String icon,

    Integer menuSort,

    Integer visible,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "版本号不能为空")
    Integer version
) {
}
