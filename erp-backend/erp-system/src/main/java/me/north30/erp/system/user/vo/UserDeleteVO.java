package me.north30.erp.system.user.vo;

/**
 * 删除用户响应 VO（接口文档 5.1.5 出参：被删除用户 ID 与逻辑删除标记）。
 */
public record UserDeleteVO(
    Long id,
    Integer isDeleted
) {
}
