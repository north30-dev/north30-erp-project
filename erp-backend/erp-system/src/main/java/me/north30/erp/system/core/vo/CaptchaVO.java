package me.north30.erp.system.core.vo;

/**
 * 图形验证码响应 VO（接口文档 4.1）。
 */
public record CaptchaVO(
    String captchaKey,
    String captchaImage,
    Integer expireSeconds
) {
}
