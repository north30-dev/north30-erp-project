package me.north30.erp.common.audit;

import java.time.LocalDateTime;

/**
 * 审计日志记录值对象：字段对照 sys_audit_log 表列（SYS-05）。
 * <p>由 {@link AuditLogAspect} 构造，经 {@link AuditLogStore} 持久化；
 * common 模块内不得引用任何业务模块类型。</p>
 * <p>字段映射约定：连接点签名记入 operateDesc，方法入参 JSON 记入 beforeJson，
 * 方法返回/结果 JSON 记入 afterJson（本阶段无变更前后快照，以入参/结果近似，后续可扩展）。</p>
 *
 * @param module        业务模块（AuditModuleEnum.name()，如 SYSTEM/PURCHASE）
 * @param bizType       业务类型（如 SALES_ORDER，暂未采集时为 null）
 * @param bizCode       单据号/业务标识
 * @param operateType   操作类型（OperateTypeEnum.name()，如 CREATE/UPDATE/DELETE）
 * @param operateDesc   操作描述（连接点签名）
 * @param operateBy     操作人用户名（可能为 null，落库侧兜底 anonymous）
 * @param operateTime   操作时间
 * @param operateIp     操作 IP（X-Forwarded-For 解析结果）
 * @param requestUri    请求地址
 * @param requestMethod 请求方法（GET/POST 等）
 * @param beforeJson    方法入参 JSON（密码字段已在序列化前排除）
 * @param afterJson     方法返回结果 JSON（失败时为 null）
 * @param resultStatus  结果 0-失败 1-成功
 * @param errorCode     失败时的业务错误码（BusinessException 携带，其余异常为 null）
 * @param errorMessage  失败原因
 * @param costTime      方法耗时（毫秒）
 * @param traceId       链路追踪 ID（MDC traceId）
 */
public record AuditLogRecord(
    String module,
    String bizType,
    String bizCode,
    String operateType,
    String operateDesc,
    String operateBy,
    LocalDateTime operateTime,
    String operateIp,
    String requestUri,
    String requestMethod,
    String beforeJson,
    String afterJson,
    Integer resultStatus,
    Integer errorCode,
    String errorMessage,
    Long costTime,
    String traceId
) {
}
