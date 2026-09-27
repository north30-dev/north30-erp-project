package me.north30.erp.common.jwt;

import java.time.Instant;

/**
 * 已签发令牌信息：令牌字符串与其元数据（用于写 Redis 会话等场景）。
 *
 * @param token JWT 字符串
 * @param jti 令牌唯一 ID
 * @param expiresAt 过期时间
 * @param ttlSeconds 有效期（秒）
 */
public record IssuedToken(
    String token,
    String jti,
    Instant expiresAt,
    long ttlSeconds
) {
}
