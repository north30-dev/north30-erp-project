package me.north30.erp.base.materialcategory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改物料分类请求 DTO（接口文档 6.1.3，PUT /api/base/material-categories/{id}）。
 * <p>category_code 与 parent_id 不可修改（已使用编码不复用）；各业务字段为否（选填），
 * 传 null 表示不修改；category_level/is_leaf 由服务端维护，接口不接受。</p>
 *
 * @param categoryName 分类名称（base_material_category.category_name），null 表示不修改
 * @param categorySort 显示顺序（base_material_category.category_sort），null 表示不修改
 * @param status       状态 0-停用 1-启用（base_material_category.status），null 表示不修改
 * @param remark       备注（base_material_category.remark），null 表示不修改
 * @param version      乐观锁版本号（base_material_category.version）
 */
public record CategoryUpdateDTO(

    @Size(max = 100, message = "分类名称长度不能超过 100")
    String categoryName,

    Integer categorySort,

    @Min(value = 0, message = "状态值非法")
    @Max(value = 1, message = "状态值非法")
    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号非法")
    Integer version
) {
}
