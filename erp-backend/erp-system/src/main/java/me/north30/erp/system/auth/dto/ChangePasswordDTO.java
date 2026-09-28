package me.north30.erp.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改本人密码请求 DTO（接口文档 4.8）。
 * <p>密码字段仅传输层使用，禁止入日志。</p>
 */
public record ChangePasswordDTO(

    @NotBlank(message = "原密码不能为空")
    @Size(max = 64, message = "原密码长度不能超过 64")
    String oldPassword,

    @NotBlank(message = "新密码不能为空")
    @Size(max = 64, message = "新密码长度不能超过 64")
    String newPassword,

    @NotBlank(message = "确认新密码不能为空")
    @Size(max = 64, message = "确认新密码长度不能超过 64")
    String confirmPassword
) {
}
