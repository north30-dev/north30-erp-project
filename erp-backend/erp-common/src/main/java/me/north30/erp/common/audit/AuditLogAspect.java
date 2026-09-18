package me.north30.erp.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志切面：拦截标注 {@link AuditLog} 的方法，记录操作人、IP、请求 URI、
 * 模块、操作类型、单据号、方法名与耗时。
 * <p>日志输出统一使用 SLF4J 占位符，禁止字符串拼接。</p>
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    /**
     * 环绕通知：方法执行结束后输出审计日志。
     * <p>操作人本期占位为 anonymous（D2 接入 JWT 后从安全上下文取当前用户）；
     * 落库持久化在 D2 数据库就绪后接入 sys_audit_log。</p>
     */
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            HttpServletRequest request = currentRequest();
            String ip = resolveClientIp(request);
            String uri = request != null ? request.getRequestURI() : "";
            // D2 接入 JWT 后从安全上下文取当前用户
            String operator = "anonymous";
            log.info("审计日志 | 操作人: {} | IP: {} | URI: {} | 模块: {} | 操作类型: {} | 单据号: {} | 方法: {} | 耗时: {}ms",
                    operator, ip, uri, auditLog.module(), auditLog.operateType(),
                    auditLog.bizCode(), joinPoint.getSignature().toShortString(), cost);
        }
    }

    /**
     * 从 RequestContextHolder 获取当前请求（异步/非 Web 场景可能为空）。
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 解析客户端 IP：优先取反向代理透传的 X-Forwarded-For 第一段，取不到用 getRemoteAddr()。
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
