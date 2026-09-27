package me.north30.erp.system.core.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志 VO（接口文档 5.8.1 分页 / 5.8.2 详情）。
 * <p>使用可变对象承接 Mapper XML resultType 驼峰自动映射；
 * beforeJson/afterJson 仅详情接口返回，diffFields 为详情接口派生字段。</p>
 */
@Data
public class AuditLogVO {

    /** 主键 */
    private Long id;

    /** 业务模块 PURCHASE/SALES/INVENTORY/MANUFACTURING/FINANCE/SYSTEM */
    private String module;

    /** 业务类型 SALES_ORDER/PURCHASE_ORDER/WORK_ORDER/STOCK_TRANSACTION 等 */
    private String bizType;

    /** 单据号 */
    private String bizCode;

    /** 操作类型 CREATE/UPDATE/APPROVE/REJECT/DELETE/POST/UNAPPROVE/CLOSE/REOPEN */
    private String operateType;

    /** 操作描述 */
    private String operateDesc;

    /** 操作人（用户名） */
    private String operateBy;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 操作 IP */
    private String operateIp;

    /** 请求地址 */
    private String requestUri;

    /** 请求方法 */
    private String requestMethod;

    /** 结果 0-失败 1-成功 */
    private Integer resultStatus;

    /** 失败时的错误码 */
    private Integer errorCode;

    /** 失败原因 */
    private String errorMessage;

    /** 方法耗时（毫秒） */
    private Long costTime;

    /** 链路追踪 ID */
    private String traceId;

    /** 变更前 JSON（仅详情接口返回） */
    private String beforeJson;

    /** 变更后 JSON（仅详情接口返回） */
    private String afterJson;

    /** 变更字段清单（详情接口派生：对比 before_json/after_json 键值差异） */
    private List<String> diffFields;
}
