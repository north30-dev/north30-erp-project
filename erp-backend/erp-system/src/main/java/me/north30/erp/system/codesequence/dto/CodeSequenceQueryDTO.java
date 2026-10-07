package me.north30.erp.system.codesequence.dto;

/**
 * 编号序列分页查询参数（接口文档 5.7.1）。
 * <p>pageNum/pageSize 缺省值由服务层 normalize 方法处理。</p>
 * 
 * @param bizType 业务类型（如：订单、发票等）
 * @param period 日期周期（如：202308）
 * @param pageNum 页码（默认 1）
 * @param pageSize 每页数量（默认 10）
 */
public record CodeSequenceQueryDTO(
    String bizType,
    String period,
    Integer pageNum,
    Integer pageSize
) {
}
