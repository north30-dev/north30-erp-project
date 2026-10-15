package me.north30.erp.system.common.util;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.config.service.SysConfigDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link PageParamResolver} 纯单元测试：配置采用、60s TTL 缓存、上限只收紧、异常回落。
 */
@ExtendWith(MockitoExtension.class)
class PageParamResolverTest {

    private static final String KEY_DEFAULT = "system.page.default.size";
    private static final String KEY_MAX = "system.page.max.size";

    @Mock
    private SysConfigDomainService sysConfigDomainService;

    private PageParamResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PageParamResolver(sysConfigDomainService);
    }

    @Nested
    @DisplayName("normalizePageSize：配置采用与缓存")
    class PageSizeTest {

        @Test
        @DisplayName("DB 默认页大小 50，不传 pageSize 时归一为 50")
        void shouldAdoptConfiguredDefault() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "50", KEY_MAX, "200"));

            long pageSize = resolver.normalizePageSize(null);

            assertThat(pageSize).isEqualTo(50L);
        }

        @Test
        @DisplayName("TTL 内第二次调用命中缓存，不再打配置服务")
        void shouldUseCache_withinTtl() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "50", KEY_MAX, "200"));

            resolver.normalizePageSize(null);
            resolver.normalizePageSize(null);

            verify(sysConfigDomainService, times(1)).values(anyCollection());
        }

        @Test
        @DisplayName("配置缺失时回落常量默认值 20")
        void shouldFallbackTo20_whenConfigMissing() {
            given(sysConfigDomainService.values(anyCollection())).willReturn(Map.of());

            long pageSize = resolver.normalizePageSize(null);

            assertThat(pageSize).isEqualTo(20L);
        }

        @Test
        @DisplayName("配置值非法时回落常量默认值 20")
        void shouldFallbackTo20_whenConfigInvalid() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "abc", KEY_MAX, "200"));

            long pageSize = resolver.normalizePageSize(null);

            assertThat(pageSize).isEqualTo(20L);
        }

        @Test
        @DisplayName("配置服务抛异常时保持旧值且不向上传播")
        void shouldKeepOldValue_whenConfigServiceFails() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "50", KEY_MAX, "200"));
            assertThat(resolver.normalizePageSize(null)).isEqualTo(50L);

            // 第二个 resolver 实例首次刷新即失败：仍按常量兜底 20
            given(sysConfigDomainService.values(anyCollection())).willThrow(new RuntimeException("db down"));
            PageParamResolver failing = new PageParamResolver(sysConfigDomainService);

            long pageSize = failing.normalizePageSize(null);

            assertThat(pageSize).isEqualTo(20L);
        }

        @Test
        @DisplayName("pageSize 超过生效上限时抛 422，文案携带当前上限")
        void shouldThrow422_whenExceedsMax() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "20", KEY_MAX, "100"));

            assertThatThrownBy(() -> resolver.normalizePageSize(150))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("pageSize 不能超过 100");
        }
    }

    @Nested
    @DisplayName("max.size 只能收紧")
    class MaxSizeClampTest {

        @Test
        @DisplayName("DB 配置 500 超过契约上界，仍按 200 拦截")
        void shouldClampTo200_whenConfigExceedsContract() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "20", KEY_MAX, "500"));

            assertThatThrownBy(() -> resolver.normalizePageSize(201))
                .hasMessage("pageSize 不能超过 200");
        }

        @Test
        @DisplayName("DB 配置 100 收紧上限，pageSize=150 拦截、pageSize=90 放行")
        void shouldTightenLimit_whenConfigBelowContract() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "20", KEY_MAX, "100"));

            assertThat(resolver.normalizePageSize(90)).isEqualTo(90L);
            assertThatThrownBy(() -> resolver.normalizePageSize(150))
                .hasMessage("pageSize 不能超过 100");
        }
    }

    @Nested
    @DisplayName("normalizePageNum / Strict 变体")
    class PageNumTest {

        @Test
        @DisplayName("页码 null 或 ≤0 归一为 1（不读配置）")
        void shouldNormalizePageNum() {
            assertThat(resolver.normalizePageNum(null)).isEqualTo(1L);
            assertThat(resolver.normalizePageNum(0)).isEqualTo(1L);
            assertThat(resolver.normalizePageNum(3)).isEqualTo(3L);
            verify(sysConfigDomainService, times(0)).values(anyCollection());
        }

        @Test
        @DisplayName("严格版：越界（<1 或 >上限）抛业务异常，null 回退配置默认值")
        void shouldNormalizeStrict() {
            given(sysConfigDomainService.values(anyCollection()))
                .willReturn(Map.of(KEY_DEFAULT, "50", KEY_MAX, "200"));

            assertThat(resolver.normalizePageSizeStrict(null)).isEqualTo(50L);
            assertThatThrownBy(() -> resolver.normalizePageSizeStrict(0))
                .hasMessage("每页条数须在 1-200 之间");
            assertThatThrownBy(() -> resolver.normalizePageSizeStrict(201))
                .hasMessage("每页条数须在 1-200 之间");
        }
    }
}
