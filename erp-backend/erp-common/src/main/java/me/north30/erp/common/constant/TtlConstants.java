package me.north30.erp.common.constant;

import java.time.Duration;

/**
 * TTL 常量：各缓存/会话的有效期统一在此维护（必须设 TTL）。
 */
public final class TtlConstants {

    /** 图形验证码有效期（秒）：5 分钟、一次有效 */
    public static final int CAPTCHA_SECONDS = 300;

    /** 登录失败计数窗口：30 分钟（达到阈值锁定期间亦为 30 分钟） */
    public static final Duration LOGIN_FAIL_TTL = Duration.ofMinutes(30);

    /** 登录失败锁定阈值：连续失败 5 次 */
    public static final int LOGIN_FAIL_LOCK_THRESHOLD = 5;

    /** 口令有效期：90 天（password_update_time 基准） */
    public static final Duration PASSWORD_VALID_DURATION = Duration.ofDays(90);

    private TtlConstants() {
    }
}
