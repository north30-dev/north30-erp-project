package me.north30.erp.system.dict.vo;

/**
 * 字典项下拉选项 VO：启用项精简视图，供下拉框/级联等前端控件消费。
 *
 * @param itemValue 字典项值（编码值，不随语言变化，作为表单提交值）
 * @param itemLabel 字典项标签（按 lang 存储的显示文案）
 */
public record DictItemOptionVO(
    String itemValue, 
    String itemLabel) {
}
