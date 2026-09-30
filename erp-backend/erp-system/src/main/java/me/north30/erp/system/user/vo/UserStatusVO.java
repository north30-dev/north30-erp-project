package me.north30.erp.system.user.vo;

/**
 * 启用/停用用户响应 VO（接口文档 5.1.6 出参：变更后状态与会话是否已失效）。
 * 
 * @param status 变更后状态
 * @param sessionRevoked 会话是否已失效
 */
public record UserStatusVO(
    Integer status,
    Boolean sessionRevoked
) {
}
