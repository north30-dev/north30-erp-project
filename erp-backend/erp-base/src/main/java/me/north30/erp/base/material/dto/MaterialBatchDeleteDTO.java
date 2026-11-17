package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 物料批量删除请求 DTO（接口文档 6.2.7，单次 ≤200 条，部分失败响应见 13.1）。
 *
 * @param ids 物料 ID 集合（必填，1-200 条）
 */
public record MaterialBatchDeleteDTO(

    @NotEmpty(message = "物料 ID 集合不能为空")
    @Size(max = 200, message = "单次批量删除不能超过 200 条")
    List<Long> ids
) {
}
