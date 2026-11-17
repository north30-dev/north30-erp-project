package me.north30.erp.base.material.vo;

/**
 * 物料列表项 VO（接口文档 6.2.1 出参）。
 * <p>金额/数量为 6 位小数字符串（接口文档 1.7 口径）。</p>
 *
 * @param id                 物料 ID
 * @param materialCode       物料编码
 * @param materialName       物料名称
 * @param spec               规格型号
 * @param brand              品牌
 * @param categoryId         物料分类 ID
 * @param categoryName       物料分类名称（待分类域批量名称查询接口落地后回填，本期恒为 null）
 * @param materialType       物料类型 1-原材料 2-半成品 3-成品 4-辅料
 * @param sourceType         来源类型 1-自制 2-外购 3-外协
 * @param unit               计量单位
 * @param status             状态 0-新建 1-审核 2-启用 3-停用
 * @param abcClass           ABC 分类 A/B/C
 * @param standardCost       标准成本（6 位小数字符串）
 * @param safetyStock        安全库存量（6 位小数字符串）
 * @param maxStock           最大库存量（6 位小数字符串）
 * @param minOrderQty        最小采购批量（6 位小数字符串）
 * @param leadTime           采购提前期（天）
 * @param productionLeadTime 生产提前期（天）
 * @param costMethod         计价方法（本期固定 0）
 * @param isBatchManaged     是否批次管理
 * @param isSerialManaged    是否序列号管理
 * @param isShelfLife        是否保质期管理
 * @param isMrpActive        是否参与 MRP 运算 0-否 1-是
 */
public record MaterialVO(
    Long id,
    String materialCode,
    String materialName,
    String spec,
    String brand,
    Long categoryId,
    String categoryName,
    Integer materialType,
    Integer sourceType,
    String unit,
    Integer status,
    String abcClass,
    String standardCost,
    String safetyStock,
    String maxStock,
    String minOrderQty,
    Integer leadTime,
    Integer productionLeadTime,
    Integer costMethod,
    Boolean isBatchManaged,
    Boolean isSerialManaged,
    Boolean isShelfLife,
    Integer isMrpActive
) {
}
