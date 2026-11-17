package me.north30.erp.system.unit.auth.strategy;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.auth.properties.AuthSecurityProperties;
import me.north30.erp.system.auth.strategy.LoginRateLimitStrategy;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LoginRateLimitStrategy 单元测试：Redis 滑动窗口放行/超限、Redis 故障本地降级、关闭限速。
 */
@ExtendWith(MockitoExtension.class)
class LoginRateLimitStrategyTest {

    private static final String CLIENT_IP = "192.168.1.10";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private AuthSecurityProperties properties;

    private LoginRateLimitStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new AuthSecurityProperties();
        strategy = new LoginRateLimitStrategy(stringRedisTemplate, properties);
    }

    /**
     * 统一断言业务异常：类型、错误码与提示消息一次校验。
     */
    private static void assertBusinessError(ThrowingCallable action, int expectedCode, String expectedMessage) {
        assertThatThrownBy(action)
            .isInstanceOf(BusinessException.class)
            .hasMessage(expectedMessage)
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getCode()).isEqualTo(expectedCode));
    }

    @Test
    @DisplayName("Redis 正常且窗口内未超限：放行并写入 ZSET、设置 TTL")
    void shouldAllow_whenUnderLimit() {
        // Given
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(0L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(Boolean.TRUE);
        // When
        strategy.checkLogin(CLIENT_IP);
        // Then
        verify(zSetOperations).removeRangeByScore(anyString(), anyDouble(), anyDouble());
        verify(zSetOperations).add(anyString(), anyString(), anyDouble());
        verify(stringRedisTemplate).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Redis 正常且窗口内已达上限：抛 18042 且不再写入 ZSET")
    void shouldThrowTooFrequent_whenOverLimit() {
        // Given
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.zCard(anyString())).thenReturn(10L);
        // When / Then
        assertBusinessError(() -> strategy.checkLogin(CLIENT_IP),
            SystemErrorCode.REQUEST_TOO_FREQUENT.getCode(), "操作过于频繁，请稍后重试");
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("Redis 故障降级本地计数：超过阈值后抛 18042（不裸奔放行）")
    void shouldThrowTooFrequent_whenRedisUnavailableAndLocalExceeded() {
        // Given
        properties.setLoginRateLimitPerMinute(2);
        when(stringRedisTemplate.opsForZSet())
            .thenThrow(new RedisConnectionFailureException("connection refused"));
        // When / Then
        strategy.checkLogin(CLIENT_IP);
        strategy.checkLogin(CLIENT_IP);
        assertBusinessError(() -> strategy.checkLogin(CLIENT_IP),
            SystemErrorCode.REQUEST_TOO_FREQUENT.getCode(), "操作过于频繁，请稍后重试");
    }

    @Test
    @DisplayName("关闭限速（阈值 <= 0）：直接放行且不触碰 Redis")
    void shouldAllow_whenLimitDisabled() {
        // Given
        properties.setLoginRateLimitPerMinute(0);
        // When
        strategy.checkLogin(CLIENT_IP);
        // Then
        verifyNoInteractions(stringRedisTemplate);
    }
}
