package me.north30.erp.base.material.vo;

import java.time.LocalDateTime;

/**
 * 物料旧码映射 VO（接口文档 6.2.14 出参）。
 *
 * @param id              映射记录 ID
 * @param oldCode         旧编码
 * @param oldName         旧名称
 * @param sourceDept      来源部门
 * @param mergeGroup      合并组编号
 * @param newMaterialId   新物料 ID
 * @param newMaterialCode 新物料编码
 * @param migrateBatch    迁移批次号
 * @param migratedAt      迁移时间
 */
public record MaterialCodeMapVO(
    Long id,
    String oldCode,
    String oldName,
    String sourceDept,
    String mergeGroup,
    Long newMaterialId,
    String newMaterialCode,
    String migrateBatch,
    LocalDateTime migratedAt
) {
}
