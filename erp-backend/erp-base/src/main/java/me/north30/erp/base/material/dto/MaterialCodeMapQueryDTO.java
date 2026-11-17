package me.north30.erp.base.material.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 物料旧码映射分页查询请求 DTO（接口文档 6.2.14，GET 查询参数绑定）。
 *
 * @param oldCode         旧编码（模糊）
 * @param newMaterialCode 新物料编码（模糊）
 * @param sourceDept      来源部门（精确，采购部/仓库/生产部）
 * @param mergeGroup      合并组编号（精确）
 * @param migrateBatch    迁移批次号（精确）
 * @param pageNum         页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize        每页条数（1-200）
 */
public record MaterialCodeMapQueryDTO(
    @Size(max = 50, message = "旧编码长度不能超过 50")
    String oldCode,

    @Size(max = 50, message = "新物料编码长度不能超过 50")
    String newMaterialCode,

    @Size(max = 50, message = "来源部门长度不能超过 50")
    String sourceDept,

    @Size(max = 50, message = "合并组编号长度不能超过 50")
    String mergeGroup,

    @Size(max = 50, message = "迁移批次号长度不能超过 50")
    String migrateBatch,

    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
