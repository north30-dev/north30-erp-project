package me.north30.erp.system.importtask.vo;

import java.util.List;

/**
 * 导入结果 VO（接口文档 5.10.2，含错误行回显）。
 */
public record ImportResultVO(
    String taskId,
    Integer totalRows,
    Integer successRows,
    Integer failRows,
    Boolean committed,
    List<ImportErrorVO> errors
) {
}
