package me.north30.erp.system.codesequence.vo;

/**
 * 重置编号流水响应 VO（接口文档 5.7.2）。
 *
 * @param bizType 单据类型
 * @param period 期间 yyyyMM
 * @param currentNo 重置后的流水号
 */
public record CodeSequenceResetVO(
    String bizType,
    String period,
    Integer currentNo
) {
}
