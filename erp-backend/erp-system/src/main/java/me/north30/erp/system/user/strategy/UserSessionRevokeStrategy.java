package me.north30.erp.system.user.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.constant.RedisKeyConstants;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 用户会话失效策略：删除指定用户全部在线会话（Redis IO 类策略，@Component）。
 * <p>停用账号后调用；Redis 不可用时降级 WARN 不阻断主流程（SYS-01 降级约定）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSessionRevokeStrategy {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 停用后使该用户全部会话失效：删除 access 会话与 refresh 标记。
     * <p>Redis 不可用时降级 WARN，不抛出、不阻断停用主流程（调用方照常返回已触发失效）。</p>
     *
     * @param userId 用户 ID
     */
    public void revoke(Long userId) {
        try {
            deleteByPattern(RedisKeyConstants.SESSION_PREFIX + userId + ":*");
            deleteByPattern(RedisKeyConstants.SESSION_REFRESH_PREFIX + userId + ":*");
        } catch (RedisConnectionFailureException e) {
            log.warn("停用用户后会话清理 Redis 失败，已降级处理：{}", e.getMessage());
        }
    }

    /**
     * 根据模式删除 Redis 键。
     */
    private void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
