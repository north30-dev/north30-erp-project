package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 字典项分页查询参数（接口文档 5.5.5，按字典类型过滤）。
 * <p>lang 缺省 zh-CN、pageNum/pageSize 缺省值由服务层处理。</p>
 *
 * @param dictType  字典类型编码
 * @param lang      语言编码
 * @param status    状态 0-停用 1-启用
 * @param pageNum   页码（从 1 开始）
 * @param pageSize  每页条数（1-200）
 */
public record DictItemQueryDTO(
    String dictType,
    String lang,
    Integer status,
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
