package me.north30.erp.system.auth.vo;

/**
 * 刷新令牌响应 VO（接口文档 4.3）。
 * 
 * @param accessToken 访问令牌（Bearer 格式）
 * @param tokenType 令牌类型（Bearer）
 * @param expiresIn 过期时间（秒）
 */
public record RefreshTokenVO(
    String accessToken,
    String tokenType,
    Integer expiresIn
) {
}
