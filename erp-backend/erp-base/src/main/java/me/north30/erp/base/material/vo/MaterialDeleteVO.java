package me.north30.erp.base.material.vo;

/**
 * 物料删除响应 VO（接口文档 6.2.6 出参）。
 *
 * @param id        物料 ID
 * @param isDeleted 逻辑删除标记 1-已删除
 */
public record MaterialDeleteVO(
    Long id,
    Integer isDeleted
) {
}
