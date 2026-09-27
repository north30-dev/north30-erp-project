package me.north30.erp.system.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求 DTO（接口文档 4.2）。
 */
public record LoginDTO(

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    String password,

    @NotBlank(message = "验证码不能为空")
    String captcha,

    @NotBlank(message = "验证码标识不能为空")
    String captchaKey
) {
}
