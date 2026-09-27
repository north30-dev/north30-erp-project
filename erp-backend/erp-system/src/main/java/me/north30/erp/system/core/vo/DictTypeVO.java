package me.north30.erp.system.core.vo;

/**
 * 字典类型分页响应 VO（接口文档 5.5.1）。
 *
 * @param id 字典类型 ID
 * @param dictType 字典类型编码
 * @param dictName 字典类型名称
 * @param status 状态 0-停用 1-启用
 * @param remark 备注
 * @param createTime 创建时间（yyyy-MM-dd HH:mm:ss）
 * @param itemCount 字典项数量（派生：sys_dict_item 计数，is_deleted=0）
 */
public record DictTypeVO(
    Long id,
    String dictType,
    String dictName,
    Integer status,
    String remark,
    String createTime,
    long itemCount
) {
}
