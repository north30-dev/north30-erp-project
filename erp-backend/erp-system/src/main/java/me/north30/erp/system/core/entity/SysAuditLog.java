package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 审计日志表（sys_audit_log：@AuditLog + AOP 落库，只增不改不删，在线保存 ≥180 天）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_audit_log")
public class SysAuditLog extends BaseEntity {

    /** 业务模块 PURCHASE/SALES/INVENTORY/MANUFACTURING/FINANCE/SYSTEM */
    private String module;

    /** 业务类型 SALES_ORDER/PURCHASE_ORDER/WORK_ORDER/STOCK_TRANSACTION 等 */
    private String bizType;

    /** 单据号（如 SO202608001） */
    private String bizCode;

    /** 操作类型 CREATE/UPDATE/APPROVE/REJECT/DELETE/POST/UNAPPROVE/CLOSE/REOPEN */
    private String operateType;

    /** 操作描述 */
    private String operateDesc;

    /** 操作人（用户名） */
    private String operateBy;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 操作 IP（含 X-Forwarded-For 解析结果） */
    private String operateIp;

    /** 请求地址 */
    private String requestUri;

    /** 请求方法 */
    private String requestMethod;

    /** 变更前 JSON（白名单字段，超长截断标记） */
    private String beforeJson;

    /** 变更后 JSON（白名单字段，超长截断标记） */
    private String afterJson;

    /** 结果 0-失败 1-成功 */
    private Integer resultStatus;

    /** 失败时的错误码 */
    private Integer errorCode;

    /** 失败原因 */
    private String errorMessage;

    /** 方法耗时（毫秒） */
    private Long costTime;

    /** 链路追踪 ID（X-Request-Id） */
    private String traceId;
}
