package me.north30.erp.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增菜单请求 DTO（接口文档 5.3.2，menuType 1-目录 2-菜单 3-按钮）。
 */
public record MenuCreateDTO(

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 100, message = "菜单名称长度不能超过 100")
    String menuName,

    @NotNull(message = "父级菜单 ID 不能为空")
    Long parentId,

    @NotNull(message = "菜单类型不能为空")
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
    String remark
) {
}
