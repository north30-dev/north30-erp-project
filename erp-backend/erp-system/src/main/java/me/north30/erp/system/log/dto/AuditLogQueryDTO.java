package me.north30.erp.system.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 审计日志分页查询入参（接口文档 5.8.1）。
 * <p>GET 绑定用可变对象；startTime/endTime 支持yyyy-MM-dd 与yyyy-MM-dd HH:mm:ss 两种格式，
 * 时间区间左闭右开，由服务层解析后填充查询参数。</p>
 */
@Data
public class AuditLogQueryDTO {

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum;

    /** 每页条数（1-200） */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    private Integer pageSize;

    /** 单据号（sys_audit_log.biz_code，模糊） */
    private String bizCode;

    /** 业务模块（sys_audit_log.module） */
    private String module;

    /** 业务类型（sys_audit_log.biz_type） */
    private String bizType;

    /** 操作类型（sys_audit_log.operate_type） */
    private String operateType;

    /** 操作人（sys_audit_log.operate_by，模糊） */
    private String operateBy;

    /** 操作时间区间起点（含） */
    private String startTime;

    /** 操作时间区间终点（不含） */
    private String endTime;

    /** 结果 0-失败 1-成功（sys_audit_log.result_status） */
    private Integer resultStatus;
}
