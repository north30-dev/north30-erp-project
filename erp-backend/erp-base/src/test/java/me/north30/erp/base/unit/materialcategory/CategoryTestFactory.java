package me.north30.erp.base.unit.materialcategory;

import me.north30.erp.base.materialcategory.dto.CategoryCreateDTO;
import me.north30.erp.base.materialcategory.dto.CategoryStatusDTO;
import me.north30.erp.base.materialcategory.dto.CategoryTreeQueryDTO;
import me.north30.erp.base.materialcategory.dto.CategoryUpdateDTO;
import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;

/**
 * 物料分类域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class CategoryTestFactory {

    private CategoryTestFactory() {
    }

    /**
     * 构建分类实体（默认启用、乐观锁版本 0）。
     */
    public static BaseMaterialCategory category(Long id, String code, String name, Long parentId,
                                                Integer level, Boolean isLeaf, Integer sort, Integer status) {
        BaseMaterialCategory category = new BaseMaterialCategory();
        category.setId(id);
        category.setCategoryCode(code);
        category.setCategoryName(name);
        category.setParentId(parentId);
        category.setCategoryLevel(level);
        category.setIsLeaf(isLeaf);
        category.setCategorySort(sort);
        category.setStatus(status);
        category.setVersion(0);
        return category;
    }

    /**
     * 一级分类实体（parentId=0，层级 1，叶子标记 TRUE）。
     */
    public static BaseMaterialCategory level1(Long id, String code, String name, Integer sort) {
        return category(id, code, name, 0L, 1, Boolean.TRUE, sort, 1);
    }

    /**
     * 二级分类实体（叶子标记 TRUE）。
     */
    public static BaseMaterialCategory level2(Long id, String code, String name, Long parentId, Integer sort) {
        return category(id, code, name, parentId, 2, Boolean.TRUE, sort, 1);
    }

    /**
     * 新增分类请求 DTO（备注固定为 null）。
     */
    public static CategoryCreateDTO createDTO(String code, String name, Long parentId, Integer sort, Integer status) {
        return new CategoryCreateDTO(code, name, parentId, sort, status, null);
    }

    /**
     * 修改分类请求 DTO（备注固定为 null 表示不修改）。
     */
    public static CategoryUpdateDTO updateDTO(String name, Integer sort, Integer status, Integer version) {
        return new CategoryUpdateDTO(name, sort, status, null, version);
    }

    /**
     * 状态变更请求 DTO。
     */
    public static CategoryStatusDTO statusDTO(Integer status, Integer version) {
        return new CategoryStatusDTO(status, version);
    }

    /**
     * 分类树查询参数（全不过滤）。
     */
    public static CategoryTreeQueryDTO treeQueryDTO() {
        return new CategoryTreeQueryDTO(null, null, null);
    }

    /**
     * 分类树 VO（materialCount 本阶段不装配为 null、children 由树构造装配）。
     */
    public static CategoryTreeVO treeVO(Long id, String code, String name, Long parentId,
                                        Integer level, Boolean isLeaf, Integer sort, Integer status) {
        return new CategoryTreeVO(id, code, name, parentId, level, isLeaf, sort, status, null, 0, null, null);
    }

    /**
     * 分类实体 → 树 VO（字段逐一拷贝，供树构造用例从实体工厂复用）。
     */
    public static CategoryTreeVO toTreeVO(BaseMaterialCategory category) {
        return treeVO(category.getId(), category.getCategoryCode(), category.getCategoryName(),
            category.getParentId(), category.getCategoryLevel(), category.getIsLeaf(),
            category.getCategorySort(), category.getStatus());
    }
}
