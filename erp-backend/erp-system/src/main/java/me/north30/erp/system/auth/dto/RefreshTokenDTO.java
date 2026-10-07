package me.north30.erp.system.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求 DTO（接口文档 4.3）。
 * 
 * @param refreshToken 刷新令牌（用于刷新新令牌）
 */
public record RefreshTokenDTO(

    @NotBlank(message = "refreshToken 不能为空")
    String refreshToken
) {
}
