package me.north30.erp.system.auth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证链路安全配置（登录链路防爆破，09 文档批次三）。
 * <p>限流阈值按 IP 维度每分钟计数，{@code <= 0} 表示关闭对应限速；
 * 渐进式验证码阈值与用户名维度锁定阈值用于控制验证码升级与账号锁定。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "erp.auth")
public class AuthSecurityProperties {

    /** 登录接口单 IP 每分钟最大请求数（<=0 关闭限速） */
    private int loginRateLimitPerMinute = 10;

    /** 取码接口单 IP 每分钟最大请求数（<=0 关闭限速） */
    private int captchaRateLimitPerMinute = 30;

    /** 同一用户名失败次数达到该值后，登录必须校验验证码（渐进式验证码） */
    private int captchaRequiredThreshold = 3;

    /** 同一用户名失败次数达到该值后锁定账号（窗口 30 分钟） */
    private int usernameFailLockThreshold = 20;
}
