package me.north30.erp.system.role.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改角色请求 DTO（接口文档 5.2.3，role_code 不可修改，乐观锁 version 必填）。
 *
 * @param roleName  角色名称（≤100 字符）
 * @param roleSort  排序号
 * @param dataScope 数据范围
 * @param status    状态
 * @param remark    备注（≤500 字符）
 * @param version   乐观锁版本号
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
