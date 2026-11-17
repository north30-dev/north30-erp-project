package me.north30.erp.base.unit.material;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import me.north30.erp.base.material.dto.MaterialAuditDTO;
import me.north30.erp.base.material.dto.MaterialBatchDeleteDTO;
import me.north30.erp.base.material.dto.MaterialCodeMapCreateDTO;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.dto.MaterialDisableDTO;
import me.north30.erp.base.material.dto.MaterialUpdateDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;

/**
 * 物料域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class MaterialTestFactory {

    private MaterialTestFactory() {
    }

    /**
     * 构建物料实体（已应用创建默认值：状态 0-新建、标准成本 0、最小采购批量 1、计价方法 0）。
     */
    public static BaseMaterial material(Long id, String code, String name, Integer status) {
        BaseMaterial material = new BaseMaterial();
        material.setId(id);
        material.setMaterialCode(code);
        material.setMaterialName(name);
        material.setCategoryId(3L);
        material.setMaterialType(1);
        material.setSourceType(2);
        material.setUnit("个");
        material.setVersion(0);
        material.applyCreateDefault();
        if (status != null) {
            material.setStatus(status);
        }
        return material;
    }

    /**
     * 新增物料请求 DTO（名称必填 + 分类 3L + 类型 1-原材料 + 来源 2-外购 + 单位"个"）。
     */
    public static MaterialCreateDTO createDTO(String name) {
        return new MaterialCreateDTO(name, null, null, 3L, 1, 2, "个",
            null, null, null, null, null, null, null, null, null, 0,
            null, null, null, null, null, null,
            null, null, null, null, 1, null);
    }

    /**
     * 修改物料请求 DTO（部分更新语义：null 不修改，携带乐观锁版本）。
     */
    public static MaterialUpdateDTO updateDTO(String name, Long categoryId, String unit, Integer version) {
        return new MaterialUpdateDTO(name, null, null, categoryId, null, null, unit,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, version);
    }

    /**
     * 审核请求 DTO。
     */
    public static MaterialAuditDTO auditDTO(boolean approved) {
        return new MaterialAuditDTO(approved, null);
    }

    /**
     * 批量删除请求 DTO。
     */
    public static MaterialBatchDeleteDTO batchDeleteDTO(List<Long> ids) {
        return new MaterialBatchDeleteDTO(ids);
    }

    /**
     * 停用请求 DTO。
     */
    public static MaterialDisableDTO disableDTO(String reason) {
        return new MaterialDisableDTO(reason);
    }

    /**
     * 旧码映射新增请求 DTO。
     */
    public static MaterialCodeMapCreateDTO codeMapCreateDTO(String oldCode, String sourceDept, Long newMaterialId) {
        return new MaterialCodeMapCreateDTO(oldCode, null, sourceDept, null, newMaterialId, null);
    }

    /**
     * 旧码映射实体。
     */
    public static BaseMaterialCodeMap codeMap(Long id, String oldCode, String sourceDept) {
        BaseMaterialCodeMap codeMap = new BaseMaterialCodeMap();
        codeMap.setId(id);
        codeMap.setOldCode(oldCode);
        codeMap.setSourceDept(sourceDept);
        codeMap.setVersion(0);
        codeMap.setMigratedAt(LocalDateTime.of(2026, 11, 16, 12, 0));
        return codeMap;
    }

    /**
     * 便捷构造带标准成本的物料。
     */
    public static BaseMaterial materialWithCost(Long id, String code, String cost) {
        BaseMaterial material = material(id, code, "电阻", 2);
        material.setStandardCost(new BigDecimal(cost));
        return material;
    }
}
