package me.north30.erp.base.materialcategory.vo;

/**
 * 物料分类写操作响应 VO（接口文档 6.1.2/6.1.3/6.1.4）。
 *
 * @param id            分类 ID（base_material_category.id）
 * @param categoryLevel 服务端计算/推导的层级（base_material_category.category_level，删除时为 null）
 * @param isLeaf        服务端维护的叶子标记（base_material_category.is_leaf，删除时为 null）
 * @param updateTime    更新时间（新增/删除时为 null）
 * @param isDeleted     逻辑删除标记（仅删除接口返回）
 */
public record CategoryMutationVO(
    Long id,
    Integer categoryLevel,
    Boolean isLeaf,
    String updateTime,
    Integer isDeleted
) {
}
