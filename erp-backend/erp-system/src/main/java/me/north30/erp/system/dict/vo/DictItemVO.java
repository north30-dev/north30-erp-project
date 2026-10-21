package me.north30.erp.system.dict.vo;

/**
 * 字典项响应 VO（接口文档 5.5.5/5.5.6）。
 *
 * @param id 字典项 ID
 * @param dictType 字典类型编码
 * @param itemLabel 字典项标签
 * @param itemValue 字典项值（编码值，不随语言变化）
 * @param lang 语言
 * @param itemSort 显示顺序
 * @param cssClass 前端标签样式
 * @param isDefault 是否默认选中 0-否 1-是
 * @param extJson 扩展属性 JSON
 * @param status 状态 0-停用 1-启用
 * @param remark 备注
 * @param cached 是否命中缓存（派生，字典缓存后续阶段接入，当前恒为 false）
 */
public record DictItemVO(
    Long id,
    String dictType,
    String itemLabel,
    String itemValue,
    String lang,
    Integer itemSort,
    String cssClass,
    Integer isDefault,
    String extJson,
    Integer status,
    String remark,
    Boolean cached
) {
}
