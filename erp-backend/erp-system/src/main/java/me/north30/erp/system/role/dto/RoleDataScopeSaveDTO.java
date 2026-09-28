package me.north30.erp.system.role.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 配置角色数据范围（自定义）请求 DTO（接口文档 5.2.6，全量覆盖语义）。
 */
public record RoleDataScopeSaveDTO(

    @NotNull(message = "数据范围配置不能为空")
    List<@Valid RoleDataScopeItemDTO> scopes
) {
}
