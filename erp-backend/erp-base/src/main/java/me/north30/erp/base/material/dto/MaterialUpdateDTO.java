package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改物料请求 DTO（接口文档 6.2.5，部分更新语义 + 乐观锁）。
 * <p>物料编码不可修改（无对应入参字段）；已启用（status=2）物料修改关键字段
 * （分类/类型/计量单位）须先停用（11003）；计价方法本期不支持变更（11012）。</p>
 *
 * @param materialName       物料名称（≤200，四要素之一，null 表示不修改）
 * @param spec               规格型号（≤200，四要素之一，null 表示不修改）
 * @param brand              品牌（≤100，四要素之一，null 表示不修改）
 * @param categoryId         物料分类 ID（null 表示不修改；变更须为启用的叶子分类）
 * @param materialType       物料类型 1-原材料 2-半成品 3-成品 4-辅料（null 表示不修改）
 * @param sourceType         来源类型 1-自制 2-外购 3-外协（null 表示不修改）
 * @param unit               计量单位（≤10，null 表示不修改；变更须在字典 unit 内）
 * @param drawingNo          图号（≤100，null 表示不修改）
 * @param drawingVersion     图号版本（≤20，null 表示不修改）
 * @param standardCost       标准成本（≥0，6 位小数字符串，null 表示不修改）
 * @param safetyStock        安全库存量（≥0，6 位小数字符串，null 表示不修改）
 * @param maxStock           最大库存量（≥0，6 位小数字符串，null 表示不修改）
 * @param minOrderQty        最小采购批量（≥1，6 位小数字符串，null 表示不修改）
 * @param abcClass           ABC 分类 A/B/C（null 表示不修改）
 * @param leadTime           采购提前期（天，null 表示不修改）
 * @param productionLeadTime 生产提前期（天，null 表示不修改）
 * @param costMethod         计价方法（本期不支持变更，传入非 0 返回 11012）
 * @param inventorySubjectId 默认存货科目 ID（null 表示不修改）
 * @param costSubjectId      默认成本科目 ID（null 表示不修改）
 * @param revenueSubjectId   默认收入科目 ID（null 表示不修改）
 * @param defaultSupplierId  默认供应商 ID（null 表示不修改）
 * @param defaultWarehouseId 默认仓库 ID（null 表示不修改）
 * @param defaultLocationId  默认库位 ID（null 表示不修改）
 * @param isBatchManaged     是否批次管理（null 表示不修改）
 * @param isSerialManaged    是否序列号管理（null 表示不修改）
 * @param isShelfLife        是否保质期管理（null 表示不修改）
 * @param shelfLifeDays      保质期天数（null 表示不修改）
 * @param isMrpActive        是否参与 MRP 运算（null 表示不修改）
 * @param remark             备注（≤500，null 表示不修改）
 * @param version            乐观锁版本号（必填，与 base_material.version 一致）
 */
public record MaterialUpdateDTO(

    @Size(max = 200, message = "物料名称长度不能超过 200")
    String materialName,

    @Size(max = 200, message = "规格型号长度不能超过 200")
    String spec,

    @Size(max = 100, message = "品牌长度不能超过 100")
    String brand,

    Long categoryId,

    @Min(value = 1, message = "物料类型取值仅支持 1-原材料 2-半成品 3-成品 4-辅料")
    @Max(value = 4, message = "物料类型取值仅支持 1-原材料 2-半成品 3-成品 4-辅料")
    Integer materialType,

    @Min(value = 1, message = "来源类型取值仅支持 1-自制 2-外购 3-外协")
    @Max(value = 3, message = "来源类型取值仅支持 1-自制 2-外购 3-外协")
    Integer sourceType,

    @Size(max = 10, message = "计量单位长度不能超过 10")
    String unit,

    @Size(max = 100, message = "图号长度不能超过 100")
    String drawingNo,

    @Size(max = 20, message = "图号版本长度不能超过 20")
    String drawingVersion,

    @Pattern(regexp = "^\\d{1,14}(\\.\\d{1,6})?$", message = "标准成本须为非负数字（最多 6 位小数）")
    String standardCost,

    @Pattern(regexp = "^\\d{1,14}(\\.\\d{1,6})?$", message = "安全库存量须为非负数字（最多 6 位小数）")
    String safetyStock,

    @Pattern(regexp = "^\\d{1,14}(\\.\\d{1,6})?$", message = "最大库存量须为非负数字（最多 6 位小数）")
    String maxStock,

    @Pattern(regexp = "^\\d{1,14}(\\.\\d{1,6})?$", message = "最小采购批量须为非负数字（最多 6 位小数）")
    String minOrderQty,

    @Pattern(regexp = "^[ABC]$", message = "ABC 分类取值仅支持 A/B/C")
    String abcClass,

    @Min(value = 0, message = "采购提前期不能小于 0")
    Integer leadTime,

    @Min(value = 0, message = "生产提前期不能小于 0")
    Integer productionLeadTime,

    @Min(value = 0, message = "计价方法本期仅支持 0-移动加权平均")
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

    @Min(value = 1, message = "保质期天数不能小于 1")
    Integer shelfLifeDays,

    @Min(value = 0, message = "MRP 参与标记取值仅支持 0-否 1-是")
    @Max(value = 1, message = "MRP 参与标记取值仅支持 0-否 1-是")
    Integer isMrpActive,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "乐观锁版本号不能为空")
    Integer version
) {
}
