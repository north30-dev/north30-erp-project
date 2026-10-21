package me.north30.erp.system.importtask.vo;

import java.util.List;

/**
 * 导入结果 VO（接口文档 5.10.2，含错误行回显）。
 *
 * @param taskId      任务 ID
 * @param totalRows   总行数
 * @param successRows 成功行数
 * @param failRows    失败行数
 * @param committed   是否已提交
 * @param errors      错误行明细
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
