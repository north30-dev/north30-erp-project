package me.north30.erp.common.web;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * TraceIdFilter 单元测试：MDC 链路 ID 的生成/复用/清理，响应头回写与下游异常兜底。
 * <p>说明：过滤器直接依赖 slf4j MDC 静态门面（无可注入的 MutableContext），
 * 通过真实的 MDC ThreadLocal 断言行为，Mockito 用于 mock Servlet 请求/响应/过滤链。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TraceIdFilter 链路追踪过滤器")
class TraceIdFilterTest {

    @InjectMocks
    private TraceIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private ServletResponse plainResponse;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void clearMdcBefore() {
        // 保证每个用例从干净的 MDC 状态开始
        MDC.clear();
    }

    @AfterEach
    void clearMdcAfter() {
        // 过滤器自身会清理 MDC，此处兜底防止用例间串扰
        MDC.clear();
    }

    @Nested
    @DisplayName("doFilter：链路 ID 的生成、复用与清理")
    class DoFilterTest {

        @Test
        @DisplayName("MDC 为空时生成 32 位无连字符的链路 ID，写入 MDC 与响应头，执行后清理")
        void shouldGenerateTraceIdIntoMdcAndHeader_whenMdcIsEmpty() throws Exception {
            // Given
            AtomicReference<String> traceIdInChain = new AtomicReference<>();
            willAnswer(invocation -> {
                // 在下游链路执行期间捕获 MDC 中的链路 ID
                traceIdInChain.set(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
                return null;
            }).given(chain).doFilter(same(request), same(httpResponse));

            // When
            filter.doFilter(request, httpResponse, chain);

            // Then
            ArgumentCaptor<String> traceIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpResponse).setHeader(eq(TraceIdFilter.TRACE_ID_HEADER), traceIdCaptor.capture());
            String traceId = traceIdCaptor.getValue();
            assertThat(traceId).hasSize(32).doesNotContain("-");
            // 下游链路执行期间 MDC 中可见同一链路 ID，执行结束后被清理
            assertThat(traceIdInChain.get()).isEqualTo(traceId);
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
            verify(chain).doFilter(same(request), same(httpResponse));
        }

        @Test
        @DisplayName("MDC 已有链路 ID 时复用既有值，不重新生成")
        void shouldReuseExistingTraceId_whenMdcAlreadyHasBeenSet() throws Exception {
            // Given
            MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "existing-trace-id-0001");

            // When
            filter.doFilter(request, httpResponse, chain);

            // Then
            ArgumentCaptor<String> traceIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpResponse).setHeader(eq(TraceIdFilter.TRACE_ID_HEADER), traceIdCaptor.capture());
            assertThat(traceIdCaptor.getValue()).isEqualTo("existing-trace-id-0001");
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
            verify(chain).doFilter(same(request), same(httpResponse));
        }

        @Test
        @DisplayName("MDC 值为空白字符串时视为缺失，重新生成链路 ID")
        void shouldRegenerateTraceId_whenMdcValueIsBlank() throws Exception {
            // Given
            MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "   ");

            // When
            filter.doFilter(request, httpResponse, chain);

            // Then
            ArgumentCaptor<String> traceIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(httpResponse).setHeader(eq(TraceIdFilter.TRACE_ID_HEADER), traceIdCaptor.capture());
            assertThat(traceIdCaptor.getValue()).hasSize(32).doesNotContain("-");
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("响应为非 HttpServletResponse 时不回写响应头，下游链路仍继续执行")
        void shouldSkipHeaderAndContinueChain_whenResponseIsNotHttpServletResponse() throws Exception {
            // When
            filter.doFilter(request, plainResponse, chain);

            // Then
            // 非 HTTP 响应不写响应头：未传入的 HttpServletResponse 必须零交互
            verifyNoInteractions(httpResponse);
            verify(chain).doFilter(same(request), same(plainResponse));
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("下游链路抛出异常时异常向上传播，且 MDC 仍被兜底清理")
        void shouldClearMdcEven_whenChainThrows() throws Exception {
            // Given
            willThrow(new IOException("下游服务中断")).given(chain)
                .doFilter(same(request), same(httpResponse));

            // When
            // Then
            assertThatThrownBy(() -> filter.doFilter(request, httpResponse, chain))
                .isInstanceOf(IOException.class)
                .hasMessage("下游服务中断");

            // 异常前响应头已回写，MDC 在 finally 中被清理
            verify(httpResponse).setHeader(eq(TraceIdFilter.TRACE_ID_HEADER),
                argThat(traceId -> traceId != null && traceId.length() == 32));
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
        }
    }
}
