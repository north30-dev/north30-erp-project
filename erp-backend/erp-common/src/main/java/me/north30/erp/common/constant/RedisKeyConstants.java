package me.north30.erp.common.constant;

/**
 * Redis Key 常量：统一按 业务模块:功能:唯一标识 命名，所有 Key 必须设置 TTL。
 * <p>Key 模板与《详细设计说明书》2.8 Redis 使用详细设计一致。</p>
 */
public final class RedisKeyConstants {

    /** access token 会话白名单前缀：system:session:{userId}:{jti}，TTL 与 token 一致 */
    public static final String SESSION_PREFIX = "system:session:";

    /** refresh token 会话标记前缀：system:session:refresh:{userId}:{jti}，TTL 7 天 */
    public static final String SESSION_REFRESH_PREFIX = "system:session:refresh:";

    /** refresh token jti 失效集合前缀：system:refresh:invalid:{userId}:{jti}，TTL = refresh 剩余有效期 */
    public static final String REFRESH_INVALID_PREFIX = "system:refresh:invalid:";

    /** 图形验证码前缀：system:captcha:{captchaKey}，TTL 5 分钟、一次有效 */
    public static final String CAPTCHA_PREFIX = "system:captcha:";

    /** 登录失败计数前缀：system:login:fail:{username}，TTL 30 分钟，阈值 5 次 */
    public static final String LOGIN_FAIL_PREFIX = "system:login:fail:";

    private RedisKeyConstants() {
    }

    /** access token 会话 Key：system:session:{userId}:{jti} */
    public static String sessionKey(Long userId, String jti) {
        return SESSION_PREFIX + userId + ":" + jti;
    }

    /** refresh token 会话标记 Key：system:session:refresh:{userId}:{jti} */
    public static String sessionRefreshKey(Long userId, String jti) {
        return SESSION_REFRESH_PREFIX + userId + ":" + jti;
    }

    /** refresh token jti 失效集合 Key：system:refresh:invalid:{userId}:{jti} */
    public static String refreshInvalidKey(Long userId, String jti) {
        return REFRESH_INVALID_PREFIX + userId + ":" + jti;
    }

    /** 图形验证码 Key：system:captcha:{captchaKey} */
    public static String captchaKey(String captchaKey) {
        return CAPTCHA_PREFIX + captchaKey;
    }

    /** 登录失败计数 Key：system:login:fail:{username} */
    public static String loginFailKey(String username) {
        return LOGIN_FAIL_PREFIX + username;
    }
}
