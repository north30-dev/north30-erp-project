package me.north30.erp.system.core.vo;

/**
 * 重置用户密码响应 VO（接口文档 5.1.7 出参：临时口令仅本次响应返回）。
 */
public record UserResetPasswordVO(
    String initialPassword,
    Boolean forceChangeOnLogin
) {
}
