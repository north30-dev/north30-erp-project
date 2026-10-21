package me.north30.erp.system.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 审计日志分页查询入参（接口文档 5.8.1）。
 * <p>startTime/endTime 支持 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss 两种格式，
 * 时间区间左闭右开，由服务层解析后填充查询参数。</p>
 *
 * @param pageNum      页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize     每页条数（1-200）
 * @param bizCode      单据号（sys_audit_log.biz_code，模糊）
 * @param module       业务模块（sys_audit_log.module）
 * @param bizType      业务类型（sys_audit_log.biz_type）
 * @param operateType  操作类型（sys_audit_log.operate_type）
 * @param operateBy    操作人（sys_audit_log.operate_by，模糊）
 * @param startTime    操作时间区间起点（含）
 * @param endTime      操作时间区间终点（不含）
 * @param resultStatus 结果 0-失败 1-成功（sys_audit_log.result_status）
 */
public record AuditLogQueryDTO(
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize,
    String bizCode,
    String module,
    String bizType,
    String operateType,
    String operateBy,
    String startTime,
    String endTime,
    Integer resultStatus
) {
}
