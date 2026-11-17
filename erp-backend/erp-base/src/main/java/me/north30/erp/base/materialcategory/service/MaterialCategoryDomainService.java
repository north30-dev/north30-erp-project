package me.north30.erp.base.materialcategory.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;

/**
 * 物料分类领域服务接口：承载本域业务规则（编码联动唯一性、层级、删除校验、
 * 叶子标记维护、move 同级规范化重排）与本域 Mapper 网关。
 */
public interface MaterialCategoryDomainService {

    /**
     * 校验分类存在，不存在抛 10001（@TableLogic 自动过滤已删除行）。
     *
     * @param id 分类 ID
     * @return 分类实体
     */
    BaseMaterialCategory requireCategory(Long id);

    /**
     * 校验父级分类存在（顶级传 null/0 直接返回 null）：不存在抛 10001。
     *
     * @param parentId 父级分类 ID（顶级传 null 或 0）
     * @return 父级分类实体（顶级返回 null）
     */
    BaseMaterialCategory requireParentCategory(Long parentId);

    /**
     * 校验分类编码可用，已存在抛 11007（库级 uk_base_material_category_code 全局唯一）。
     *
     * @param categoryCode 分类编码
     */
    void requireCodeAvailable(String categoryCode);

    /**
     * 删除前置校验（11006）：存在下级分类或已挂载物料均不可删除。
     * <p>挂载物料计数由应用服务层经物料域最小接口取得后作参数传入（领域服务不跨域注入对方 ApplicationService）。</p>
     *
     * @param category             分类实体
     * @param mountedMaterialCount 该分类已挂载物料数（物料域 countByCategoryId 结果）
     */
    void requireDeletable(BaseMaterialCategory category, long mountedMaterialCount);

    /**
     * 校验分类存在且启用并返回其编码（跨域最小接口，物料域校验 11004/11017 用）：
     * 不存在抛 10001，已停用抛 11017。
     *
     * @param categoryId 分类 ID
     * @return 分类编码
     */
    String requireEnabledCategoryCode(Long categoryId);

    /**
     * 校验分类是否叶子节点（跨域最小接口，物料域校验必须挂载叶子分类 11005 用）：
     * 分类不存在抛 10001。
     *
     * @param categoryId 分类 ID
     * @return true-叶子节点（可挂物料）
     */
    boolean isLeaf(Long categoryId);

    /**
     * 按条件查询分类列表（categorySort ASC, id ASC，树构造一次查全量，禁 N+1）。
     *
     * @param categoryName 分类名称（模糊，空则不过滤）
     * @param categoryCode 分类编码（模糊，空则不过滤）
     * @param status       状态（null 不过滤）
     * @return 分类列表
     */
    List<BaseMaterialCategory> listCategories(String categoryName, String categoryCode, Integer status);

    /**
     * 查询指定父级下的同级分类（categorySort ASC, id ASC，move 规范化重排用）。
     *
     * @param parentId 父级分类 ID（顶级传 null 或 0）
     * @return 同级分类列表
     */
    List<BaseMaterialCategory> listSiblings(Long parentId);

    /**
     * 计算同级下一个显示顺序（当前同级 max(categorySort) + 10，空同级返回 10）。
     * <p>新增分类未显式指定 categorySort 时调用（排序统一约定）。</p>
     *
     * @param parentId 父级分类 ID（顶级传 null 或 0）
     * @return 下一个显示顺序值
     */
    int nextCategorySort(Long parentId);

    /**
     * 刷新父级叶子标记：父级存在下级分类置 FALSE，否则恢复 TRUE（is_leaf 依据是否存在下级）。
     * <p>新增二级分类后、删除末个二级分类后调用；parentId 为顶级（null/0）时直接跳过。</p>
     *
     * @param parentId 父级分类 ID
     */
    void refreshParentLeafFlag(Long parentId);

    /**
     * 新增分类落库（主键由 MyBatis-Plus 雪花算法回填）。
     *
     * @param category 分类实体
     */
    void insert(BaseMaterialCategory category);

    /**
     * 更新分类行，更新 0 行视为乐观锁冲突（10601，HTTP 409）。
     *
     * @param category 分类实体（version 须携带客户端版本号）
     */
    void updateRow(BaseMaterialCategory category);

    /**
     * 按 ID 逻辑删除分类。
     *
     * @param id 分类 ID
     */
    void deleteById(Long id);

    /**
     * 同级上移/下移：一次查出同级，规范化重排后交换相邻位置，变更行逐一乐观锁落库。
     *
     * @param id 目标分类 ID
     * @param up true-上移 false-下移
     */
    void moveCategory(Long id, boolean up);

    /**
     * 批量加载分类名称（一次 IN 查询，避免循环查库；跨域最小接口，物料域列表 categoryName 回填用）。
     *
     * @param categoryIds 分类 ID 集合
     * @return 分类 ID 映射分类名称
     */
    Map<Long, String> getCategoryNameMap(Set<Long> categoryIds);
}
