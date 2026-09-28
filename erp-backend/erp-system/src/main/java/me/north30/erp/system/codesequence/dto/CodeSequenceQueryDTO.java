package me.north30.erp.system.codesequence.dto;

import lombok.Data;

/**
 * 编号序列分页查询参数（接口文档 5.7.1）。
 */
@Data
public class CodeSequenceQueryDTO {

    /** 单据类型（如 SALES_ORDER） */
    private String bizType;

    /** 期间 yyyyMM */
    private String period;

    /** 页码（从 1 开始） */
    private long pageNum = 1;

    /** 每页条数（上限 200） */
    private long pageSize = 20;
}
