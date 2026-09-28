package me.north30.erp.system.auth.vo;

/**
 * 刷新令牌响应 VO（接口文档 4.3）。
 */
public record RefreshTokenVO(
    String accessToken,
    String tokenType,
    Integer expiresIn
) {
}
