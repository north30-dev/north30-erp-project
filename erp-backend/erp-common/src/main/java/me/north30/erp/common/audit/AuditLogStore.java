package me.north30.erp.common.audit;

/**
 * 审计日志存储接口：common 模块定义，由业务模块（erp-system）提供落库实现。
 * <p>common 不依赖任何业务模块，通过该接口反转依赖；容器中无实现 Bean 时，
 * {@link AuditLogAspect} 自动降级为仅 SLF4J 输出。</p>
 */
public interface AuditLogStore {

    /**
     * 持久化一条审计日志。
     * <p>实现方须自行保证失败不影响主流程语义：切面调用处已 try-catch 包裹，
     * 实现内部异常会被捕获并仅记录 WARN 日志。</p>
     *
     * @param record 审计日志记录（字段对照 sys_audit_log）
     */
    void store(AuditLogRecord record);
}
