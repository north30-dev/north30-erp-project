package me.north30.erp.system.dict.dto;

/**
 * 字典项分页查询参数（接口文档 5.5.5，按字典类型过滤）。
 * <p>lang 缺省 zh-CN、pageNum/pageSize 缺省值由服务层处理。</p>
 */
public record DictItemQueryDTO(
    String dictType,
    String lang,
    Integer status,
    Integer pageNum,
    Integer pageSize
) {
}
