package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增物料旧码映射请求 DTO（接口文档 6.2.15，old_code + source_dept 唯一）。
 *
 * @param oldCode      旧编码（必填，≤50，如 IC-001/WZ-089）
 * @param oldName      旧名称（≤200）
 * @param sourceDept   来源部门（必填，≤50，采购部/仓库/生产部）
 * @param mergeGroup   合并组编号（≤50，如 M-0001）
 * @param newMaterialId 新物料 ID（必填，须指向已存在物料，服务端回填新物料编码）
 * @param migrateBatch 迁移批次号（≤50）
 */
public record MaterialCodeMapCreateDTO(

    @NotBlank(message = "旧编码不能为空")
    @Size(max = 50, message = "旧编码长度不能超过 50")
    String oldCode,

    @Size(max = 200, message = "旧名称长度不能超过 200")
    String oldName,

    @NotBlank(message = "来源部门不能为空")
    @Size(max = 50, message = "来源部门长度不能超过 50")
    String sourceDept,

    @Size(max = 50, message = "合并组编号长度不能超过 50")
    String mergeGroup,

    @NotNull(message = "新物料 ID 不能为空")
    Long newMaterialId,

    @Size(max = 50, message = "迁移批次号长度不能超过 50")
    String migrateBatch
) {
}
