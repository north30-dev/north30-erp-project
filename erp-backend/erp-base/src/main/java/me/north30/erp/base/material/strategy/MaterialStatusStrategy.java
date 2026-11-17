package me.north30.erp.base.material.strategy;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.common.exception.BusinessException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 物料状态机策略：0-新建 → 1-审核 → 2-启用 → 3-停用 的命名流转入口与删除前置守卫。
 * <p>流转前置校验与非法流转错误码（11003/11009/11010）收敛于实体
 * {@link BaseMaterial} 的充血方法（局部充血：状态机自身校验），本策略承载
 * 需要外部信息的守卫（删除引用计数 → 11011）与流转附带动作（停用原因落备注），
 * 供 DomainService/ApplicationService 统一调用。</p>
 */
@Component
public class MaterialStatusStrategy {

    /**
     * 审核流转入口（接口文档 6.2.8）：通过置 1-审核，驳回答 0-新建；非法前置实体抛 11010。
     *
     * @param material 物料实体（携带当前状态）
     * @param approved true-审核通过 / false-审核驳回
     * @return 变更后状态
     */
    public Integer audit(BaseMaterial material, boolean approved) {
        material.audit(approved);
        return material.getStatus();
    }

    /**
     * 启用流转入口（接口文档 6.2.9）：1-审核 → 2-启用；0-新建抛 11010、3-停用抛 11003、已启用幂等。
     *
     * @param material 物料实体（携带当前状态）
     * @return 变更后状态
     */
    public Integer enable(BaseMaterial material) {
        material.enable();
        return material.getStatus();
    }

    /**
     * 停用流转入口（接口文档 6.2.10）：1-审核/2-启用 → 3-停用；0-新建抛 11010、已停用幂等；
     * 停用原因非空时落 base_material.remark（接口文档 6.2.10 字段口径）。
     *
     * @param material 物料实体（携带当前状态）
     * @param reason   停用原因（可空）
     * @return 变更后状态
     */
    public Integer disable(BaseMaterial material, String reason) {
        material.disable();
        if (StringUtils.hasText(reason)) {
            material.setRemark(reason);
        }
        return material.getStatus();
    }

    /**
     * 删除前置守卫（接口文档 6.2.6：仅 0-新建 可删除）。
     * <p>守卫顺序：先引用计数（11011 已被业务单据引用），后状态守卫（11009 非新建不可删除）；
     * BOM/订单引用计数由调用方传入，BOM 域落地前恒为 0。</p>
     *
     * @param material       物料实体（携带当前状态与编码）
     * @param referenceCount 业务单据引用计数（BOM/订单/流水）
     * @throws BusinessException 11011 物料已被引用不可删除 / 11009 非新建状态不可删除
     */
    public void requireDeletable(BaseMaterial material, long referenceCount) {
        if (referenceCount > 0) {
            throw new BusinessException(BaseErrorCode.MATERIAL_REFERENCED,
                "物料 " + material.getMaterialCode() + " 已被业务单据引用，不可删除");
        }
        Integer status = material.getStatus();
        if (status == null || status != BaseMaterial.STATUS_NEW) {
            throw new BusinessException(BaseErrorCode.MATERIAL_ENABLED_UNDELETABLE,
                "物料 " + material.getMaterialCode() + " 非新建状态（当前状态 " + status + "），不可删除");
        }
    }
}
