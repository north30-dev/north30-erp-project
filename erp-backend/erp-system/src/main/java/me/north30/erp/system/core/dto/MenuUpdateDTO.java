package me.north30.erp.system.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改菜单请求 DTO（接口文档 5.3.3，乐观锁 version 必填，parentId 不得指向自身或自身下级）。
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
