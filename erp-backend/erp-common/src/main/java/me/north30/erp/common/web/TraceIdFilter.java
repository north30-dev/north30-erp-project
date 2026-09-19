package me.north30.erp.common.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器：从请求头 X-Request-Id 读取链路 ID（网关/前端传入），
 * 缺失时生成 UUID；写入 MDC 供日志与 Result.traceId 使用，并回写响应头。
 * <p>最终必须清理 MDC，避免线程池复用导致链路 ID 串扰。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        try {
            String traceId = MDC.get(TRACE_ID_MDC_KEY);
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            if (response instanceof jakarta.servlet.http.HttpServletResponse httpResponse) {
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
