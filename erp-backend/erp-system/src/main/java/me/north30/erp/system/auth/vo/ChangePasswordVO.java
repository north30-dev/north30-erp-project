package me.north30.erp.system.auth.vo;

/**
 * 修改本人密码响应 VO（接口文档 4.8）。
 * 
 * @param passwordUpdateTime 密码更新时间
 * @param reLoginRequired 是否需要重新登录 0-否 1-是
 */
public record ChangePasswordVO(
    String passwordUpdateTime,
    Boolean reLoginRequired
) {
}
