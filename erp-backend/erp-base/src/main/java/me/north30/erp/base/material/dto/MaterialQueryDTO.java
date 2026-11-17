package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 物料分页查询/导出请求 DTO（接口文档 6.2.1/6.2.13，GET 查询参数绑定）。
 *
 * @param materialCode 物料编码（模糊）
 * @param materialName 物料名称（模糊）
 * @param categoryId   物料分类 ID（精确匹配；含下级展开需分类域批量接口，本期未接入）
 * @param materialType 物料类型 1-原材料 2-半成品 3-成品 4-辅料
 * @param sourceType   来源类型 1-自制 2-外购 3-外协
 * @param status       状态 0-新建 1-审核 2-启用 3-停用
 * @param abcClass     ABC 分类 A/B/C
 * @param isMrpActive  是否参与 MRP 运算 0-否 1-是
 * @param pageNum      页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize     每页条数（1-200）
 */
public record MaterialQueryDTO(
    @Size(max = 50, message = "物料编码长度不能超过 50")
    String materialCode,

    @Size(max = 200, message = "物料名称长度不能超过 200")
    String materialName,

    Long categoryId,

    @Min(value = 1, message = "物料类型取值仅支持 1-原材料 2-半成品 3-成品 4-辅料")
    @Max(value = 4, message = "物料类型取值仅支持 1-原材料 2-半成品 3-成品 4-辅料")
    Integer materialType,

    @Min(value = 1, message = "来源类型取值仅支持 1-自制 2-外购 3-外协")
    @Max(value = 3, message = "来源类型取值仅支持 1-自制 2-外购 3-外协")
    Integer sourceType,

    @Min(value = 0, message = "状态取值仅支持 0-新建 1-审核 2-启用 3-停用")
    @Max(value = 3, message = "状态取值仅支持 0-新建 1-审核 2-启用 3-停用")
    Integer status,

    @Pattern(regexp = "^[ABC]$", message = "ABC 分类取值仅支持 A/B/C")
    String abcClass,

    @Min(value = 0, message = "MRP 参与标记取值仅支持 0-否 1-是")
    @Max(value = 1, message = "MRP 参与标记取值仅支持 0-否 1-是")
    Integer isMrpActive,

    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
