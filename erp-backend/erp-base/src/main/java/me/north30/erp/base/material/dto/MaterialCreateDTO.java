package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增物料请求 DTO（接口文档 6.2.4）。
 * <p>物料编码由服务端按"分类编码前 4 位 + 6 位流水"生成，入参不含编码；
 * 金额/数量以字符串传输（6 位小数口径，接口文档 1.7），服务端解析为 DECIMAL(20,6)。</p>
 *
 * @param materialName         物料名称（必填，≤200，四要素之一）
 * @param spec                 规格型号（≤200，四要素之一）
 * @param brand                品牌（≤100，四要素之一）
 * @param categoryId           物料分类 ID（必填，须为启用的叶子分类，决定编码前 4 位）
 * @param materialType         物料类型（必填 1-原材料 2-半成品 3-成品 4-辅料）
 * @param sourceType           来源类型（必填 1-自制 2-外购 3-外协）
 * @param unit                 计量单位（必填，取值须在字典 unit 内）
 * @param drawingNo            图号（≤100）
 * @param drawingVersion       图号版本（≤20）
 * @param standardCost         标准成本（≥0，6 位小数字符串）
 * @param safetyStock          安全库存量（≥0，6 位小数字符串）
 * @param maxStock             最大库存量（≥0，6 位小数字符串）
 * @param minOrderQty          最小采购批量（≥1，6 位小数字符串）
 * @param abcClass             ABC 分类 A/B/C（缺省 C）
 * @param leadTime             采购提前期（天，≥0）
 * @param productionLeadTime   生产提前期（天，≥0）
 * @param costMethod           计价方法（本期仅允许 0-移动加权平均）
 * @param inventorySubjectId   默认存货科目 ID（叶子科目校验待财务域落地后接入）
 * @param costSubjectId        默认成本科目 ID（叶子科目校验待财务域落地后接入）
 * @param revenueSubjectId     默认收入科目 ID（叶子科目校验待财务域落地后接入）
 * @param defaultSupplierId    默认供应商 ID
 * @param defaultWarehouseId   默认仓库 ID
 * @param defaultLocationId    默认库位 ID
 * @param isBatchManaged       是否批次管理（缺省 false）
 * @param isSerialManaged      是否序列号管理（缺省 false）
 * @param isShelfLife          是否保质期管理（缺省 false，true 时 shelfLifeDays 必填）
 * @param shelfLifeDays        保质期天数
 * @param isMrpActive          是否参与 MRP 运算（缺省 1）
 * @param remark               备注（≤500）
 */
public record MaterialCreateDTO(

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 200, message = "物料名称长度不能超过 200")
    String materialName,

    @Size(max = 200, message = "规格型号长度不能超过 200")
    String spec,

    @Size(max = 100, message = "品牌长度不能超过 100")
    String brand,

    @NotNull(message = "物料分类不能为空")
    Long categoryId,

    @NotNull(message = "物料类型不能为空")
    @Min(value = 1, message = "物料类型取值仅支持 1-原材料 2-半成品 3-成品 4-辅料")
    @Max(value = 4, message = "物料类型取值仅支持 1-原材料 2-半成品 3-成品 4-辅料")
    Integer materialType,

    @NotNull(message = "来源类型不能为空")
    @Min(value = 1, message = "来源类型取值仅支持 1-自制 2-外购 3-外协")
    @Max(value = 3, message = "来源类型取值仅支持 1-自制 2-外购 3-外协")
    Integer sourceType,

    @NotBlank(message = "计量单位不能为空")
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
    @Max(value = 0, message = "计价方法本期仅支持 0-移动加权平均")
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
    String remark
) {
}
