package me.north30.erp.base.materialcategory.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 物料分类树响应 VO（接口文档 6.1.1，children 递归，两级树）。
 * <p>remark/version 为编辑弹窗回填用附加字段（对齐组织部门树 VO 惯例）。</p>
 *
 * @param id           分类 ID（base_material_category.id）
 * @param categoryCode 分类编码（base_material_category.category_code，一级 2 位、二级 4 位）
 * @param categoryName 分类名称（base_material_category.category_name）
 * @param parentId     父级分类 ID（base_material_category.parent_id，0 为一级）
 * @param categoryLevel 层级（base_material_category.category_level，1-一级 2-二级）
 * @param isLeaf       是否叶子节点（base_material_category.is_leaf，TRUE-可挂物料）
 * @param categorySort 显示顺序（base_material_category.category_sort）
 * @param status       状态 0-停用 1-启用（base_material_category.status）
 * @param remark       备注（编辑弹窗回填用）
 * @param version      乐观锁版本号（编辑提交用）
 * @param materialCount 该分类下物料数（派生：base_material 计数；跨域物料计数为单分类粒度最小接口，树查询批量口径待物料域提供后回填，本阶段为 null）
 * @param children     下级分类（递归结构，无子节点时为 null）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryTreeVO(

    Long id,

    String categoryCode,

    String categoryName,

    Long parentId,

    Integer categoryLevel,

    Boolean isLeaf,

    Integer categorySort,

    Integer status,

    String remark,

    Integer version,

    Integer materialCount,

    List<CategoryTreeVO> children
) {
}
