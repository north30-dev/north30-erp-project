package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 字典类型分页查询参数（接口文档 5.5.1）。
 *
 * @param dictType 字典类型编码（模糊）
 * @param dictName 字典类型名称（模糊）
 * @param status   状态 0-停用 1-启用
 * @param pageNum  页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize 每页条数（1-200）
 */
public record DictTypeQueryDTO(
    String dictType,
    String dictName,
    Integer status,
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
