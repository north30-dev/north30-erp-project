package me.north30.erp.base.material.vo;

/**
 * 物料详情 VO（接口文档 6.2.2 出参，base_material 全列 + 默认供应商/仓库/库位/科目引用）。
 * <p>金额/数量为 6 位小数字符串；科目名称派生自 fin_subject，待财务域落地后回填，本期仅返回 ID。</p>
 *
 * @param id                    物料 ID
 * @param materialCode          物料编码
 * @param materialName          物料名称
 * @param spec                  规格型号
 * @param brand                 品牌
 * @param categoryId            物料分类 ID
 * @param categoryName          物料分类名称（待分类域批量名称查询接口落地后回填，本期恒为 null）
 * @param materialType          物料类型 1-原材料 2-半成品 3-成品 4-辅料
 * @param sourceType            来源类型 1-自制 2-外购 3-外协
 * @param unit                  计量单位
 * @param drawingNo             图号
 * @param drawingVersion        图号版本
 * @param standardCost          标准成本（6 位小数字符串）
 * @param safetyStock           安全库存量（6 位小数字符串）
 * @param maxStock              最大库存量（6 位小数字符串）
 * @param minOrderQty           最小采购批量（6 位小数字符串）
 * @param abcClass              ABC 分类 A/B/C
 * @param leadTime              采购提前期（天）
 * @param productionLeadTime    生产提前期（天）
 * @param costMethod            计价方法（本期固定 0）
 * @param inventorySubjectId    默认存货科目 ID
 * @param costSubjectId         默认成本科目 ID
 * @param revenueSubjectId      默认收入科目 ID
 * @param defaultSupplierId     默认供应商 ID
 * @param defaultWarehouseId    默认仓库 ID
 * @param defaultLocationId     默认库位 ID
 * @param isBatchManaged        是否批次管理
 * @param isSerialManaged       是否序列号管理
 * @param isShelfLife           是否保质期管理
 * @param shelfLifeDays         保质期天数
 * @param isMrpActive           是否参与 MRP 运算 0-否 1-是
 * @param status                状态 0-新建 1-审核 2-启用 3-停用
 * @param version               乐观锁版本号（修改/启停请求回传）
 * @param createBy              创建人
 * @param createTime            创建时间
 * @param updateBy              更新人
 * @param updateTime            更新时间
 * @param remark                备注
 */
public record MaterialDetailVO(
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
    String drawingNo,
    String drawingVersion,
    String standardCost,
    String safetyStock,
    String maxStock,
    String minOrderQty,
    String abcClass,
    Integer leadTime,
    Integer productionLeadTime,
    Integer costMethod,
    Long inventorySubjectId,
    Long costSubjectId,
    Long revenueSubjectId,
    Long defaultSupplierId,
    Long defaultWarehouseId,
    Long defaultLocationId,
    Boolean isBatchManaged,
    Boolean isSerialManaged,
    Boolean isShelfLife,
    Integer shelfLifeDays,
    Integer isMrpActive,
    Integer status,
    Integer version,
    String createBy,
    String createTime,
    String updateBy,
    String updateTime,
    String remark
) {
}
