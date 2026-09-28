package me.north30.erp.system.auth.vo;

/**
 * 修改本人密码响应 VO（接口文档 4.8）。
 */
public record ChangePasswordVO(
    String passwordUpdateTime,
    Boolean reLoginRequired
) {
}
