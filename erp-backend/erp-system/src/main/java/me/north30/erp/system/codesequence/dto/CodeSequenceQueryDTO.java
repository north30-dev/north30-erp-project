package me.north30.erp.system.codesequence.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 编号序列分页查询参数（接口文档 5.7.1）。
 *
 * @param bizType  业务类型（如：订单、发票等）
 * @param period   日期周期（如：202308）
 * @param pageNum  页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize 每页条数（1-200）
 */
public record CodeSequenceQueryDTO(
    String bizType,
    String period,

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,

    /** 每页条数（1-200） */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
