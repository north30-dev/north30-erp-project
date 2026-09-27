package me.north30.erp.system.core.vo;

/**
 * 系统参数响应 VO（接口文档 5.6.1）。
 *
 * @param id 参数 ID
 * @param configKey 参数键
 * @param configName 参数名称
 * @param configValue 参数值
 * @param valueType 值类型 1-字符串 2-数字 3-布尔 4-JSON
 * @param configGroup 分组
 * @param isSystem 是否系统内置 0-否 1-是
 * @param status 状态 0-停用 1-启用
 * @param remark 备注
 */
public record ConfigVO(
    Long id,
    String configKey,
    String configName,
    String configValue,
    Integer valueType,
    String configGroup,
    Integer isSystem,
    Integer status,
    String remark
) {
}
