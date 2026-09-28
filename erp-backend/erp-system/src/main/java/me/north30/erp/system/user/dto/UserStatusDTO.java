package me.north30.erp.system.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 启用/停用用户请求 DTO（接口文档 5.1.6，状态机守卫 + 乐观锁）。
 */
public record UserStatusDTO(

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值仅支持 0-停用 1-启用")
    @Max(value = 1, message = "状态取值仅支持 0-停用 1-启用")
    Integer status,

    @NotNull(message = "乐观锁版本号不能为空")
    Integer version
) {
}
