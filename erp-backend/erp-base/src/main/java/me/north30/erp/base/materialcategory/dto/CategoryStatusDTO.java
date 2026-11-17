package me.north30.erp.base.materialcategory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 物料分类状态变更请求 DTO（POST /api/base/material-categories/{id}/status，spec 任务 2.2）。
 * <p>接口文档 6.1 未列启停接口，按 spec 要求对齐组织部门状态开关模式补齐；
 * 停用分类不可挂载物料的校验（11017）由物料域落地。</p>
 *
 * @param status  目标状态 0-停用 1-启用
 * @param version 乐观锁版本号
 */
public record CategoryStatusDTO(

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
