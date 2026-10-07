package me.north30.erp.system.role.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 分配角色菜单权限请求 DTO（接口文档 5.2.5，全量覆盖语义，空数组表示收回全部）。
 *
 * @param menuIds 菜单权限点 ID 集合
 */
public record RoleAssignMenuDTO(

    @NotNull(message = "菜单权限点 ID 集合不能为空")
    List<Long> menuIds
) {
}
