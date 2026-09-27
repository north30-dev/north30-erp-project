package me.north30.erp.system.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * 重置用户密码请求 DTO（接口文档 5.1.7）。
 * <p>newPassword 缺省时服务端生成随机口令；密码字段仅入站传输（WRITE_ONLY），不进日志（S-06）。</p>
 */
public record UserResetPasswordDTO(

    @Size(max = 64, message = "新密码长度不能超过 64")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String newPassword
) {
}
