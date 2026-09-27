package me.north30.erp.system.core.enums;

import lombok.Getter;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 用户管理域错误码（接口文档 2.10 系统管理错误码，18001-18999 段）。
 * <p>18001-18011 已在 {@code me.north30.erp.common.exception.SystemErrorCode} 定义，
 * 本枚举避让已用值，仅补充用户管理写操作所需的码位。</p>
 */
@Getter
public enum UserErrorCode implements ErrorCode {

    /** 用户名已存在（username 唯一冲突） */
    USERNAME_EXISTS(18006, "用户名 {username} 已存在"),

    /** 用户编号已存在（user_code 唯一冲突） */
    USER_CODE_EXISTS(18007, "用户编号 {userCode} 已存在"),

    /** 内置管理员不可操作（删除/停用 is_admin=1 用户） */
    ADMIN_PROTECTED(18012, "内置超级管理员不可删除或停用"),

    /** 用户已被引用不可删除（存在角色分配等关联） */
    USER_REFERENCED(18013, "用户 {username} 已被引用，不可删除（请停用）");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    UserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
