package me.north30.erp.system.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配用户角色请求 DTO（接口文档 5.1.8，全量覆盖语义：空数组表示清空）。
 */
public record UserAssignRolesDTO(

    @NotNull(message = "角色 ID 集合不能为空")
    List<Long> roleIds
) {
}
