package me.north30.erp.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.security.CurrentUserProvider;
import me.north30.erp.common.web.RequestContextUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * 审计日志切面：拦截标注 {@link AuditLog} 的方法，记录操作人、IP、请求 URI、
 * 模块、操作类型、单据号、方法名、入参/结果、结果状态与耗时。
 * <p>持久化通过 {@link AuditLogStore} 接口反转依赖（实现位于 erp-system 写 sys_audit_log）：
 * 容器中有实现 Bean 则构造 {@link AuditLogRecord} 落库，落库失败仅 WARN 不影响主流程；
 * 无实现 Bean 时保持原行为，仅 SLF4J 输出。日志输出统一使用 SLF4J 占位符。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ObjectProvider<AuditLogStore> auditLogStoreProvider;
    private final ObjectProvider<CurrentUserProvider> currentUserProvider;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知：方法执行结束后记录审计日志（成功/失败均记录）。
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            recordAuditLog(joinPoint, auditLog, System.currentTimeMillis() - start, true, null, result);
            return result;
        } catch (Throwable ex) {
            recordAuditLog(joinPoint, auditLog, System.currentTimeMillis() - start, false, ex, null);
            throw ex;
        }
    }

    /**
     * 组装并持久化审计日志：始终输出 SLF4J 日志；有 AuditLogStore Bean 时落库（失败仅 WARN）。
     */
    private void recordAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog, long cost,
                                boolean success, Throwable ex, Object result) {
        HttpServletRequest request = RequestContextUtil.currentRequest();
        String ip = RequestContextUtil.resolveClientIp(request);
        String uri = request != null ? request.getRequestURI() : "";
        String operator = resolveOperator();
        String method = joinPoint.getSignature().toShortString();
        log.info("审计日志 | 操作人: {} | IP: {} | URI: {} | 模块: {} | 操作类型: {} | 单据号: {} | 方法: {} | 结果: {} | 耗时: {}ms",
                operator, ip, uri, auditLog.module().getDesc(), auditLog.operateType().getDesc(),
                auditLog.bizCode(), method, success ? "成功" : "失败", cost);
        AuditLogStore store = auditLogStoreProvider.getIfAvailable();
        if (store == null) {
            return;
        }
        try {
            store.store(buildRecord(joinPoint, auditLog, cost, success, ex, result, operator, ip, uri, request, method));
        } catch (Exception e) {
            // 落库失败仅告警，不影响业务主流程（SYS-05 降级约定）
            log.warn("审计日志落库失败，不影响主流程：{}", e.getMessage());
        }
    }

    /**
     * 构造审计记录：入参 JSON 记 beforeJson、返回结果 JSON 记 afterJson（密码字段已在 DTO 序列化时排除）。
     */
    private AuditLogRecord buildRecord(ProceedingJoinPoint joinPoint, AuditLog auditLog, long cost,
                                       boolean success, Throwable ex, Object result,
                                       String operator, String ip, String uri,
                                       HttpServletRequest request, String method) {
        Integer errorCode = null;
        String errorMessage = null;
        if (ex != null) {
            errorCode = ex instanceof BusinessException businessException ? businessException.getCode() : null;
            errorMessage = ex.getMessage();
        }
        return new AuditLogRecord(
                auditLog.module().name(),
                null,
                auditLog.bizCode(),
                auditLog.operateType().name(),
                method,
                operator,
                LocalDateTime.now(),
                ip,
                uri,
                request != null ? request.getMethod() : null,
                toJson(joinPoint.getArgs()),
                success ? toJson(result) : null,
                success ? 1 : 0,
                errorCode,
                errorMessage,
                cost,
                org.slf4j.MDC.get("traceId"));
    }

    /**
     * 解析当前操作人：无 CurrentUserProvider Bean 或无登录上下文时返回 null（落库侧兜底 anonymous）。
     */
    private String resolveOperator() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        return provider != null ? provider.getCurrentUsername() : null;
    }

    /**
     * 对象转 JSON：序列化失败返回 null（入参中可能含不可序列化对象，不因审计中断业务）。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("审计日志对象序列化失败，已跳过该字段：{}", e.getMessage());
            return null;
        }
    }
}
