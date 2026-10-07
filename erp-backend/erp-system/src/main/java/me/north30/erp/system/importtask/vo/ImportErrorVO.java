package me.north30.erp.system.importtask.vo;

/**
 * 导入错误行 VO（接口文档 5.10.2：rowNo 为 Excel 行号，rawData 脱敏后回显）。
 *
 * @param rowNo        Excel 行号
 * @param fieldName    出错字段名
 * @param errorCode    错误码
 * @param errorMessage 错误信息
 * @param rawData      原始行数据（脱敏后回显）
 */
public record ImportErrorVO(
    Integer rowNo,
    String fieldName,
    Integer errorCode,
    String errorMessage,
    Object rawData
) {
}
