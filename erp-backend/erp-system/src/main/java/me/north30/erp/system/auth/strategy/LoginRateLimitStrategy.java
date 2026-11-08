package me.north30.erp.system.auth.strategy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.auth.properties.AuthSecurityProperties;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 认证接口 IP 限流策略（09 文档批次三）。
 * <p>正常链路使用 Redis ZSET 60 秒滑动窗口；Redis 不可用时降级为本地 Caffeine 1 分钟
 * 固定窗口计数，保证降级期间限流仍然生效。超限统一抛 18042。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitStrategy {

    /** 限流窗口：1 分钟 */
    private static final long WINDOW_SECONDS = 60;

    /** Redis 限流 Key 的保留时长：窗口 + 10 秒缓冲，避免残留 */
    private static final long KEY_TTL_SECONDS = WINDOW_SECONDS + 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthSecurityProperties authSecurityProperties;

    /** Redis 不可用时的本地降级计数（key 为 scene:ip，1 分钟窗口） */
    private final Cache<String, AtomicInteger> localCounters = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .maximumSize(10_000)
        .build();

    /**
     * 校验登录接口 IP 限流。
     *
     * @param clientIp 客户端 IP
     */
    public void checkLogin(String clientIp) {
        checkRate("login", clientIp, authSecurityProperties.getLoginRateLimitPerMinute());
    }

    /**
     * 校验取码接口 IP 限流。
     *
     * @param clientIp 客户端 IP
     */
    public void checkCaptcha(String clientIp) {
        checkRate("captcha", clientIp, authSecurityProperties.getCaptchaRateLimitPerMinute());
    }

    private void checkRate(String scene, String clientIp, int limit) {
        if (limit <= 0) {
            return;
        }
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        if (!allow(scene, ip, limit)) {
            log.warn("认证接口触发 IP 限流 | scene: {} | ip: {} | limit: {}/min", scene, ip, limit);
            throw new BusinessException(SystemErrorCode.REQUEST_TOO_FREQUENT);
        }
    }

    /**
     * 判断本次请求是否放行：优先 Redis 滑动窗口，Redis 不可用时降级本地固定窗口。
     */
    private boolean allow(String scene, String ip, int limit) {
        String key = RedisKeyConstants.rateLimitKey(scene, ip);
        try {
            ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
            long now = System.currentTimeMillis();
            zSetOperations.removeRangeByScore(key, 0, now - WINDOW_SECONDS * 1000);
            Long count = zSetOperations.zCard(key);
            if (count != null && count >= limit) {
                return false;
            }
            zSetOperations.add(key, now + ":" + UUID.randomUUID(), now);
            stringRedisTemplate.expire(key, KEY_TTL_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (RedisConnectionFailureException e) {
            log.warn("限流计数读写 Redis 失败，降级为本地计数：{}", e.getMessage());
            return allowByLocal(scene, ip, limit);
        }
    }

    /**
     * 本地降级计数（固定 1 分钟窗口），仅 Redis 不可用时使用。
     */
    private boolean allowByLocal(String scene, String ip, int limit) {
        AtomicInteger counter = localCounters.get(scene + ":" + ip, key -> new AtomicInteger());
        return counter.incrementAndGet() <= limit;
    }
}
