package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 字典项状态变更请求 DTO（POST /api/system/dict-items/{id}/status）。
 *
 * @param status  目标状态 0-停用 1-启用
 * @param version 乐观锁版本号
 */
public record DictItemStatusDTO(

    /** 目标状态 0-停用 1-启用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    Integer status,

    /** 乐观锁版本号 */
    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号非法")
    Integer version
) {
}
