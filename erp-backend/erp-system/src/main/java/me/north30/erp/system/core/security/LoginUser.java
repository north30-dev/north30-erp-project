package me.north30.erp.system.core.security;

/**
 * 登录用户模型：由 JWT 解析并经会话/状态校验后装载，作为 SecurityContext 的 principal。
 *
 * @param userId 用户 ID（token sub）
 * @param username 用户名
 * @param jti access token 唯一 ID（会话 Key 组成部分）
 * @param refreshJti 配对 refresh token 的 jti（登出时加入失效集合）
 */
public record LoginUser(
    Long userId,
    String username,
    String jti,
    String refreshJti
) {
}
