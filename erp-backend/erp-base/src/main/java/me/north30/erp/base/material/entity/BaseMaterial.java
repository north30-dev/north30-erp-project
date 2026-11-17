package me.north30.erp.base.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;

import java.math.BigDecimal;

/**
 * 物料主数据实体（base_material，schema 17 号表）。
 * <p>局部充血：仅承载自身状态机校验与创建默认值填充，流转入口见
 * {@code me.north30.erp.base.material.strategy.MaterialStatusStrategy}；
 * 金额/数量字段为 DECIMAL(20,6) 语义，统一 {@link BigDecimal}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_material")
public class BaseMaterial extends BaseEntity {

    /** 状态：0-新建（仅该状态可删除/可审核） */
    public static final int STATUS_NEW = 0;

    /** 状态：1-审核（可启用/可停用） */
    public static final int STATUS_AUDITED = 1;

    /** 状态：2-启用（可停用；再次启用幂等） */
    public static final int STATUS_ENABLED = 2;

    /** 状态：3-停用（不可用于新建单据与 BOM，再次停用幂等） */
    public static final int STATUS_DISABLED = 3;

    /** 物料编码（2 位一级分类 + 2 位二级分类 + 6 位流水，服务端生成，不可修改） */
    private String materialCode;

    /** 物料名称（重复判定四要素之一） */
    private String materialName;

    /** 规格型号（重复判定四要素之一） */
    private String spec;

    /** 品牌（重复判定四要素之一） */
    private String brand;

    /** 物料分类 ID（base_material_category.id，须为叶子节点） */
    private Long categoryId;

    /** 物料类型 1-原材料 2-半成品 3-成品 4-辅料 */
    private Integer materialType;

    /** 来源类型 1-自制 2-外购 3-外协 */
    private Integer sourceType;

    /** 计量单位（字典 unit 取值） */
    private String unit;

    /** 图号 */
    private String drawingNo;

    /** 图号版本 */
    private String drawingVersion;

    /** 标准成本（DECIMAL(20,6)） */
    private BigDecimal standardCost;

    /** 安全库存量（DECIMAL(20,6)） */
    private BigDecimal safetyStock;

    /** 最大库存量（DECIMAL(20,6)） */
    private BigDecimal maxStock;

    /** 最小采购批量（DECIMAL(20,6)，≥1） */
    private BigDecimal minOrderQty;

    /** ABC 分类 A/B/C */
    private String abcClass;

    /** 采购提前期（天） */
    private Integer leadTime;

    /** 生产提前期（天） */
    private Integer productionLeadTime;

    /** 计价方法 0-移动加权平均 1-先进先出（本期仅实现 0） */
    private Integer costMethod;

    /** 默认存货科目 ID（fin_subject.id） */
    private Long inventorySubjectId;

    /** 默认成本科目 ID（fin_subject.id） */
    private Long costSubjectId;

    /** 默认收入科目 ID（fin_subject.id） */
    private Long revenueSubjectId;

    /** 默认供应商 ID */
    private Long defaultSupplierId;

    /** 默认仓库 ID */
    private Long defaultWarehouseId;

    /** 默认库位 ID */
    private Long defaultLocationId;

    /** 是否批次管理 */
    private Boolean isBatchManaged;

    /** 是否序列号管理 */
    private Boolean isSerialManaged;

    /** 是否保质期管理 */
    private Boolean isShelfLife;

    /** 保质期天数（isShelfLife=true 时必填） */
    private Integer shelfLifeDays;

    /** 是否参与 MRP 运算 0-否 1-是 */
    private Integer isMrpActive;

    /** 状态 0-新建 1-审核 2-启用 3-停用 */
    private Integer status;

    /**
     * 保质期开关一致性守卫：isShelfLife=true 时保质期天数必填，缺失抛 10001。
     * <p>仅校验自身状态，新增/修改前调用。</p>
     */
    public void requireShelfLifeConsistency() {
        if (Boolean.TRUE.equals(isShelfLife) && shelfLifeDays == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "物料 " + materialName + " 开启保质期管理时须填写保质期天数");
        }
    }

    /**
     * 应用创建默认值并落初始状态 0-新建（接口文档 6.2.4）：
     * 金额/数量缺省 0、最小采购批量缺省 1、ABC 分类缺省 C、提前期缺省 0、
     * 计价方法缺省 0（移动加权平均）、管理开关缺省 false、MRP 参与缺省 1。
     */
    public void applyCreateDefault() {
        if (standardCost == null) {
            standardCost = BigDecimal.ZERO;
        }
        if (safetyStock == null) {
            safetyStock = BigDecimal.ZERO;
        }
        if (maxStock == null) {
            maxStock = BigDecimal.ZERO;
        }
        if (minOrderQty == null) {
            minOrderQty = BigDecimal.ONE;
        }
        if (abcClass == null || abcClass.isBlank()) {
            abcClass = "C";
        }
        if (leadTime == null) {
            leadTime = 0;
        }
        if (productionLeadTime == null) {
            productionLeadTime = 0;
        }
        if (costMethod == null) {
            costMethod = 0;
        }
        if (isBatchManaged == null) {
            isBatchManaged = false;
        }
        if (isSerialManaged == null) {
            isSerialManaged = false;
        }
        if (isShelfLife == null) {
            isShelfLife = false;
        }
        if (isMrpActive == null) {
            isMrpActive = 1;
        }
        this.status = STATUS_NEW;
    }

    /**
     * 审核流转（接口文档 6.2.8）：前置状态仅允许 0-新建；通过置 1-审核，驳回答 0-新建；
     * 其他前置状态（1/2/3）非法流转抛 11010（HTTP 422，状态不变）。
     *
     * @param approved true-审核通过 / false-审核驳回
     */
    public void audit(boolean approved) {
        if (status == null || status != STATUS_NEW) {
            throw new BusinessException(BaseErrorCode.MATERIAL_NOT_AUDITED,
                "仅新建状态物料可审核（物料 " + materialCode + " 当前状态 " + status + "）");
        }
        if (approved) {
            this.status = STATUS_AUDITED;
        }
    }

    /**
     * 启用流转（接口文档 6.2.9）：前置状态仅允许 1-审核 → 2-启用；
     * 0-新建 → 11010（未审核不可启用）；3-停用 → 11003；2-启用 → 幂等返回成功。
     */
    public void enable() {
        if (status == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "物料状态缺失，不可启用");
        }
        if (status == STATUS_ENABLED) {
            return;
        }
        if (status == STATUS_NEW) {
            throw new BusinessException(BaseErrorCode.MATERIAL_NOT_AUDITED);
        }
        if (status == STATUS_DISABLED) {
            throw new BusinessException(BaseErrorCode.MATERIAL_DISABLED,
                "物料 " + materialCode + " 已停用，不可直接启用");
        }
        this.status = STATUS_ENABLED;
    }

    /**
     * 停用流转（接口文档 6.2.10）：前置状态 1-审核/2-启用 → 3-停用；
     * 0-新建 → 11010（须先审核）；3-停用 → 幂等返回成功。
     */
    public void disable() {
        if (status == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "物料状态缺失，不可停用");
        }
        if (status == STATUS_DISABLED) {
            return;
        }
        if (status == STATUS_NEW) {
            throw new BusinessException(BaseErrorCode.MATERIAL_NOT_AUDITED,
                "物料 " + materialCode + " 未审核，不可停用（须先审核）");
        }
        this.status = STATUS_DISABLED;
    }
}
