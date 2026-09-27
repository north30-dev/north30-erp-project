package me.north30.erp.system.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色数据范围明细项 DTO（接口文档 5.2.6 scopes[] 元素）。
 */
public record RoleDataScopeItemDTO(

    @NotBlank(message = "业务对象不能为空")
    String bizObject,

    @NotBlank(message = "过滤维度不能为空")
    String filterDimension,

    @NotNull(message = "数据范围档位不能为空")
    Integer scopeType,

    List<Long> deptIds,

    List<Long> userIds,

    Integer fieldMask
) {
}
