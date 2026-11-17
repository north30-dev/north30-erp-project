package me.north30.erp.base.materialcategory.dto;

/**
 * 物料分类树查询参数（接口文档 6.1.1，GET /api/base/material-categories/tree）。
 *
 * @param categoryName 分类名称（base_material_category.category_name，模糊）
 * @param categoryCode 分类编码（base_material_category.category_code，模糊）
 * @param status       状态 0-停用 1-启用（base_material_category.status）
 */
public record CategoryTreeQueryDTO(
    String categoryName,
    String categoryCode,
    Integer status
) {
}
