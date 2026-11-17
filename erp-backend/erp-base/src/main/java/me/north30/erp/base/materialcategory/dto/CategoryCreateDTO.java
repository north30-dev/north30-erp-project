package me.north30.erp.base.materialcategory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增物料分类请求 DTO（接口文档 6.1.2，POST /api/base/material-categories）。
 * <p>编码联动（一级 2 位数字、二级 4 位数字且以父级编码为前缀）与层级上限（最多 2 级）
 * 由服务端业务校验，失败抛 10001；编码重复抛 11007。</p>
 *
 * @param categoryCode 分类编码（base_material_category.category_code）：一级 2 位（如 01）、二级 4 位（如 0103 = 上级编码 + 2 位）
 * @param categoryName 分类名称（base_material_category.category_name）
 * @param parentId     父级分类 ID（base_material_category.parent_id），0 为一级，最多 2 级
 * @param categorySort 显示顺序（base_material_category.category_sort），缺省取同级 max+10
 * @param status       状态 0-停用 1-启用（base_material_category.status），缺省 1-启用
 * @param remark       备注（base_material_category.remark）
 */
public record CategoryCreateDTO(

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "分类编码长度不能超过 50")
    String categoryCode,

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过 100")
    String categoryName,

    @NotNull(message = "父级分类不能为空")
    Long parentId,

    Integer categorySort,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
