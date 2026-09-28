package me.north30.erp.system.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增角色请求 DTO（接口文档 5.2.2）。
 */
public record RoleCreateDTO(

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过 50")
    String roleCode,

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过 100")
    String roleName,

    Integer roleSort,

    Integer dataScope,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
