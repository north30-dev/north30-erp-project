package me.north30.erp.system.auth.vo;

/**
 * 图形验证码响应 VO（接口文档 4.1）。
 * 
 * @param captchaKey 验证码标识（登录名）
 * @param captchaImage 验证码图片（Base64 编码）
 * @param expireSeconds 过期时间（秒）
 */
public record CaptchaVO(
    String captchaKey,
    String captchaImage,
    Integer expireSeconds
) {
}
