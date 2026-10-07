package me.north30.erp.system.dict.dto;

/**
 * 字典类型分页查询参数（接口文档 5.5.1）。
 * <p>pageNum/pageSize 缺省值由服务层 normalize 方法处理。</p>
 */
public record DictTypeQueryDTO(
    String dictType,
    String dictName,
    Integer status,
    Integer pageNum,
    Integer pageSize
) {
}
