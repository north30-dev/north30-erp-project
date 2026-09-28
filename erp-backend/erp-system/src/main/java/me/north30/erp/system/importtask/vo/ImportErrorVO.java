package me.north30.erp.system.importtask.vo;

/**
 * 导入错误行 VO（接口文档 5.10.2：rowNo 为 Excel 行号，rawData 脱敏后回显）。
 */
public record ImportErrorVO(
    Integer rowNo,
    String fieldName,
    Integer errorCode,
    String errorMessage,
    Object rawData
) {
}
