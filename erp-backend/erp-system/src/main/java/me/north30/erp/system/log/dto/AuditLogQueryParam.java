package me.north30.erp.system.log.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志分页查询 Mapper 参数（由服务层从 {@link AuditLogQueryDTO} 归一化构建，不直接对外暴露）。
 */
@Data
public class AuditLogQueryParam {

    /** 单据号（模糊） */
    private String bizCode;

    /** 业务模块 */
    private String module;

    /** 业务类型 */
    private String bizType;

    /** 操作类型 */
    private String operateType;

    /** 操作人（模糊） */
    private String operateBy;

    /** 结果 0-失败 1-成功 */
    private Integer resultStatus;

    /** 操作时间起点（左闭） */
    private LocalDateTime timeStart;

    /** 操作时间终点（右开） */
    private LocalDateTime timeEnd;
}
