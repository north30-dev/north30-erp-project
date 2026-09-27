package me.north30.erp.common.exception;

import lombok.Getter;

/**
 * 系统管理域错误码（18001-18999，接口文档 2.10）。
 * <p>认证相关通用码（10101-10103）见 {@link CommonErrorCode}。</p>
 */
@Getter
public enum SystemErrorCode implements ErrorCode {

    /** 用户名或密码错误 */
    USERNAME_PASSWORD_ERROR(18001, "用户名或密码错误"),

    /** 账号已锁定（连续 5 次密码错误，锁定 30 分钟） */
    ACCOUNT_LOCKED(18002, "账号已锁定，请 {unlockMinutes} 分钟后重试"),

    /** 账号已停用 */
    ACCOUNT_DISABLED(18003, "账号已停用，请联系管理员"),

    /** 验证码错误或已过期 */
    CAPTCHA_ERROR(18004, "验证码错误或已过期"),

    /** 用户不存在 */
    USER_NOT_FOUND(18005, "用户不存在"),

    /** 原密码错误 */
    OLD_PASSWORD_ERROR(18008, "原密码错误"),

    /** 新密码不符合复杂度要求 */
    PASSWORD_COMPLEXITY_ERROR(18009, "新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类"),

    /** 新密码与历史密码重复 */
    PASSWORD_HISTORY_CONFLICT(18010, "新密码不得与最近 3 次使用的密码相同"),

    /** 口令已过期需修改 */
    PASSWORD_EXPIRED(18011, "口令已超过 90 天有效期，请先修改密码");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    SystemErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
