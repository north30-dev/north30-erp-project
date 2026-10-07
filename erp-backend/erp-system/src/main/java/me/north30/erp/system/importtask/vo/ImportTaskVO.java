package me.north30.erp.system.importtask.vo;

/**
 * 导入任务 VO（接口文档 5.10 任务与批次统计口径）。
 * <p>D2 口径：schema.sql 未建导入任务表（无 sys_import_task），本 VO 预留给导入任务列表查询空实现与后续落地。</p>
 *
 * @param taskId      任务 ID
 * @param bizType     业务类型
 * @param importBatch 导入批次号
 * @param totalRows   总行数
 * @param successRows 成功行数
 * @param failRows    失败行数
 * @param committed   是否已提交
 */
public record ImportTaskVO(
    String taskId,
    String bizType,
    String importBatch,
    Integer totalRows,
    Integer successRows,
    Integer failRows,
    Boolean committed
) {
}
