package me.north30.erp.system.auth.vo;

/**
 * 登出响应 VO（接口文档 4.4）。
 * 
 * @param logoutTime 登出时间
 */
public record LogoutVO(
    String logoutTime
) {
}
