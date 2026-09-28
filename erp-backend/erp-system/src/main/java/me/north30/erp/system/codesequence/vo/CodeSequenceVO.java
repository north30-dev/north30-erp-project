package me.north30.erp.system.codesequence.vo;

/**
 * 编号序列响应 VO（接口文档 5.7.1）。
 *
 * @param id 序列 ID
 * @param bizType 单据类型
 * @param prefix 编号前缀
 * @param period 期间 yyyyMM
 * @param currentNo 当前已用流水号
 * @param seqLength 流水位数
 * @param nextCodePreview 下一编号预览（prefix + period + 补零流水）
 * @param updateTime 更新时间（yyyy-MM-dd HH:mm:ss）
 */
public record CodeSequenceVO(
    Long id,
    String bizType,
    String prefix,
    String period,
    Integer currentNo,
    Integer seqLength,
    String nextCodePreview,
    String updateTime
) {
}
