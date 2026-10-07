package me.north30.erp.system.common.vo;

/**
 * 逻辑删除结果响应 VO（接口文档 5.2.4/5.3.4，返回主键与删除标记）。
 *
 * @param id        记录 ID
 * @param isDeleted 逻辑删除标记
 */
public record DeleteResultVO(
    Long id,
    Integer isDeleted
) {
}
