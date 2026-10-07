package me.north30.erp.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求上下文工具：从 RequestContextHolder 取当前请求，解析客户端 IP（含反向代理）与 User-Agent。
 * <p>供 AuditLogAspect 与业务服务共用；异步/非 Web 场景当前请求为空时按约定返回空串/null。</p>
 */
public final class RequestContextUtil {

    /** User-Agent 截断上限（与 sys_login_log.user_agent 字段长度对齐） */
    private static final int USER_AGENT_MAX_LENGTH = 500;

    private RequestContextUtil() {
    }

    /**
     * 获取当前 HTTP 请求（异步/非 Web 场景可能为 null）。
     */
    public static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 解析客户端 IP：优先取反向代理透传的 X-Forwarded-For 第一段，取不到用 getRemoteAddr()；无请求返回空串。
     */
    public static String resolveClientIp() {
        return resolveClientIp(currentRequest());
    }

    /**
     * 解析客户端 IP（指定请求）：优先 X-Forwarded-For 第一段，其次 getRemoteAddr()；请求为空返回空串。
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 解析 User-Agent：超过 500 字符截断（sys_login_log.user_agent 长度约束）；无请求返回 null。
     */
    public static String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && userAgent.length() > USER_AGENT_MAX_LENGTH
            ? userAgent.substring(0, USER_AGENT_MAX_LENGTH) : userAgent;
    }
}
