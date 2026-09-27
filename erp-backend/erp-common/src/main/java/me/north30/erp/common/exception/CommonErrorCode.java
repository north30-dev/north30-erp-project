package me.north30.erp.common.exception;

import lombok.Getter;

/**
 * 业务错误码枚举：集中定义全系统错误码。
 * <p>错误码区间预留：</p>
 * <ul>
 *     <li>10001-10999：通用</li>
 *     <li>11001-11999：物料/BOM</li>
 *     <li>12001-12999：采购</li>
 *     <li>13001-13999：销售</li>
 *     <li>14001-14999：库存</li>
 *     <li>15001-15999：生产</li>
 *     <li>16001-16999：财务</li>
 * </ul>
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 通用：参数错误 */
    PARAM_ERROR(10001, "参数错误"),

    /** 通用：未登录或凭证已过期 */
    UNAUTHORIZED(10002, "未登录或凭证已过期"),

    /** 通用：无权限访问 */
    FORBIDDEN(10003, "无权限访问"),

    /** 通用：未认证或登录已过期（10101-10199 认证与会话段，401 场景细化码） */
    AUTH_EXPIRED(10101, "未认证或登录已过期，请重新登录"),

    /** 通用：刷新令牌无效或已失效（过期/已登出/type≠refresh） */
    REFRESH_TOKEN_INVALID(10102, "登录状态已失效，请重新登录"),

    /** 通用：令牌类型不匹配（refresh token 访问业务接口） */
    TOKEN_TYPE_INVALID(10103, "令牌类型不匹配，请重新登录"),

    /** 通用：资源不存在 */
    NOT_FOUND(10004, "资源不存在"),

    /** 通用：数据冲突（乐观锁/唯一约束等） */
    CONFLICT(10005, "数据冲突，请刷新后重试"),

    /** 通用：数据已被其他操作修改（乐观锁 version 冲突） */
    OPTIMISTIC_LOCK_CONFLICT(10601, "数据已被其他操作修改，请重试"),

    /** 通用：数据唯一约束冲突（编码/键重复） */
    DUPLICATE_KEY(10602, "数据已存在，请检查后重试"),

    /** 通用：系统内部错误（兜底） */
    SYSTEM_ERROR(10999, "系统内部错误");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
