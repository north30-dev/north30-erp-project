package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.Size;

/**
 * 物料停用请求 DTO（接口文档 6.2.10）。
 *
 * @param reason 停用原因（≤500，落 base_material.remark，仅提示不拦截）
 */
public record MaterialDisableDTO(

    @Size(max = 500, message = "停用原因长度不能超过 500")
    String reason
) {
}
