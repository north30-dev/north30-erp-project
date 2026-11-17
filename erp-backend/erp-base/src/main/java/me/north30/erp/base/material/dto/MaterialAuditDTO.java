package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 物料审核请求 DTO（接口文档 6.2.8）。
 *
 * @param approved 审核结论（必填：true-审核通过置 1-审核 / false-审核驳回答 0-新建）
 * @param remark   审核意见（≤500，落 base_material.remark）
 */
public record MaterialAuditDTO(

    @NotNull(message = "审核结论不能为空")
    Boolean approved,

    @Size(max = 500, message = "审核意见长度不能超过 500")
    String remark
) {
}
