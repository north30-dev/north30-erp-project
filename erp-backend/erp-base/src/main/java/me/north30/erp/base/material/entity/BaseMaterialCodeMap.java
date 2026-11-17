package me.north30.erp.base.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 物料旧码→新码映射实体（base_material_code_map，schema 19 号表，迁移追溯用）。
 * <p>唯一约束 old_code + source_dept（部分唯一索引，逻辑删除豁免）；
 * 局部充血：新码绑定保持 new_material_id 与 new_material_code 原子一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_material_code_map")
public class BaseMaterialCodeMap extends BaseEntity {

    /** 旧编码（IC-001 / WZ-089 / PL-023 等历史系统编码） */
    private String oldCode;

    /** 旧名称 */
    private String oldName;

    /** 来源部门（采购部/仓库/生产部） */
    private String sourceDept;

    /** 合并组编号（如 M-0001，一物多码合并追溯） */
    private String mergeGroup;

    /** 新物料 ID（base_material.id，须指向已存在物料） */
    private Long newMaterialId;

    /** 新物料编码（服务端按 newMaterialId 回填，与 base_material.material_code 一致） */
    private String newMaterialCode;

    /** 迁移批次号 */
    private String migrateBatch;

    /** 迁移时间 */
    private LocalDateTime migratedAt;

    /**
     * 绑定新物料：new_material_id 与 new_material_code 必须取自同一物料记录，禁止单独赋值造成两者不一致。
     *
     * @param materialId 新物料 ID
     * @param materialCode 新物料编码
     */
    public void bindNewMaterial(Long materialId, String materialCode) {
        this.newMaterialId = materialId;
        this.newMaterialCode = materialCode;
    }

    /**
     * 应用创建默认值：迁移时间置为当前时间（与 DDL DEFAULT NOW() 口径一致）。
     */
    public void applyCreateDefault() {
        if (migratedAt == null) {
            migratedAt = LocalDateTime.now();
        }
    }
}
