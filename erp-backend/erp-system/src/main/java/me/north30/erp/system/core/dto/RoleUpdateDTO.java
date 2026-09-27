package me.north30.erp.system.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改角色请求 DTO（接口文档 5.2.3，role_code 不可修改，乐观锁 version 必填）。
 */
public record RoleUpdateDTO(

    @Size(max = 100, message = "角色名称长度不能超过 100")
    String roleName,

    Integer roleSort,

    Integer dataScope,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "版本号不能为空")
    Integer version
) {
}
