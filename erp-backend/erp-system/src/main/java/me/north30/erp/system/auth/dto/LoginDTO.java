package me.north30.erp.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求 DTO（接口文档 4.2）。
 * 
 * @param username 用户名（登录名）
 * @param password 密码（明文）
 * @param captcha 验证码（渐进式：未达失败阈值可不填；前端始终携带）
 * @param captchaKey 验证码标识（与 captcha 配对；未达失败阈值可不填）
 */
public record LoginDTO(

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    String password,

    String captcha,

    String captchaKey
) {
}
