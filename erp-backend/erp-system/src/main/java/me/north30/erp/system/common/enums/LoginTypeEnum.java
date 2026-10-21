package me.north30.erp.system.common.enums;

import lombok.Getter;

/**
 * 登录日志事件类型（sys_login_log.login_type）。
 */
@Getter
public enum LoginTypeEnum {

    /** 登录成功 */
    LOGIN(1),

    /** 登出 */
    LOGOUT(2),

    /** 令牌刷新 */
    REFRESH(3),

    /** 登录失败 */
    LOGIN_FAIL(4);

    /** 事件类型编码 */
    private final int code;

    LoginTypeEnum(int code) {
        this.code = code;
    }
}
