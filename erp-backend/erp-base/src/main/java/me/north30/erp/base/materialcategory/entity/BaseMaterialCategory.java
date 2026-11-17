package me.north30.erp.base.materialcategory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 物料分类表（base_material_category，MD-06 两级分类树）。
 * <p>局部充血：仅校验自身状态与修改自身字段（层级推导、编码联动格式校验、状态值域守卫、
 * 叶子标记维护），抛 {@link BusinessException}；同级唯一/删除校验等需查库的规则由
 * {@code MaterialCategoryDomainService} 承载。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_material_category")
public class BaseMaterialCategory extends BaseEntity {

    /** 顶级分类父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 分类层级上限（接口文档 6.1，最多 2 级） */
    private static final int MAX_CATEGORY_LEVEL = 2;

    /** 一级分类编码位数（如 01） */
    private static final int LEVEL_1_CODE_LENGTH = 2;

    /** 二级分类编码位数（如 0103） */
    private static final int LEVEL_2_CODE_LENGTH = 4;

    /** 分类编码（一级 2 位数字；二级 4 位数字且以父级编码为前缀，与物料编码前 4 位对应） */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 父级分类 ID（0 为一级） */
    private Long parentId;

    /** 层级 1-一级 2-二级（由上级推导，最多 2 级） */
    private Integer categoryLevel;

    /** 是否叶子节点（TRUE-无下级分类可挂物料；FALSE-有下级分类） */
    private Boolean isLeaf;

    /** 显示顺序 */
    private Integer categorySort;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /**
     * 应用父级并推导层级（parent 为 null 表示一级分类）。
     * <p>仅校验/修改自身状态：父级已是二级时子级将超过 {@value #MAX_CATEGORY_LEVEL} 级上限，抛 10001。</p>
     *
     * @param parent 父级分类实体（一级分类传 null）
     */
    public void applyParent(BaseMaterialCategory parent) {
        if (parent == null) {
            this.parentId = ROOT_PARENT_ID;
            this.categoryLevel = 1;
            return;
        }
        int level = parent.getCategoryLevel() + 1;
        if (level > MAX_CATEGORY_LEVEL) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "物料分类层级超过上限，最多两级");
        }
        this.parentId = parent.getId();
        this.categoryLevel = level;
    }

    /**
     * 应用分类编码并校验编码联动：一级 2 位数字、二级 4 位数字且以父级编码为前缀。
     * <p>仅校验/修改自身状态，不查库；同级唯一性由 DomainService 查库校验（11007）。
     * 须在 {@link #applyParent(BaseMaterialCategory)} 之后调用（依赖已推导的层级）。</p>
     *
     * @param categoryCode 分类编码
     * @param parent       父级分类实体（一级分类传 null）
     */
    public void applyCategoryCode(String categoryCode, BaseMaterialCategory parent) {
        if (categoryLevel != null && categoryLevel == 1) {
            if (categoryCode == null || !categoryCode.matches("\\d{" + LEVEL_1_CODE_LENGTH + "}")) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "一级分类编码须为 " + LEVEL_1_CODE_LENGTH + " 位数字（如 01）");
            }
        } else if (categoryLevel != null && categoryLevel == MAX_CATEGORY_LEVEL) {
            if (categoryCode == null || !categoryCode.matches("\\d{" + LEVEL_2_CODE_LENGTH + "}")) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "二级分类编码须为 " + LEVEL_2_CODE_LENGTH + " 位数字（如 0103）");
            }
            if (parent == null || parent.getCategoryCode() == null || !categoryCode.startsWith(parent.getCategoryCode())) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "二级分类编码须以父级编码开头");
            }
        }
        this.categoryCode = categoryCode;
    }

    /**
     * 变更状态：仅值域守卫（10001）。
     * <p>仅校验/修改自身状态，不查库；停用分类不可挂载物料的校验（11017）由物料域经
     * {@code requireEnabledCategoryCode} 最小接口落地。</p>
     *
     * @param targetStatus 目标状态 0-停用 1-启用
     */
    public void changeStatus(Integer targetStatus) {
        if (targetStatus == null || (targetStatus != 0 && targetStatus != 1)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "状态取值非法，仅支持 0-停用 1-启用");
        }
        this.status = targetStatus;
    }

    /**
     * 维护叶子标记：依据是否存在下级分类刷新（is_leaf 依据是否存在下级，接口文档 6.1.2）。
     * <p>仅修改自身字段：新增二级分类后父级置 FALSE，末个下级删除后父级恢复 TRUE。</p>
     *
     * @param hasChildren 是否存在下级分类
     */
    public void refreshLeafFlag(boolean hasChildren) {
        this.isLeaf = !hasChildren;
    }

    /**
     * 应用创建默认值：categorySort=0、status=1（启用）、isLeaf=TRUE（新节点无下级）。
     * <p>仅校验/修改自身状态：字段为 null 时设置，显式传入值不覆盖。</p>
     */
    public void applyCreateDefault() {
        if (categorySort == null) {
            categorySort = 0;
        }
        if (status == null) {
            status = 1;
        }
        if (isLeaf == null) {
            isLeaf = Boolean.TRUE;
        }
    }
}
