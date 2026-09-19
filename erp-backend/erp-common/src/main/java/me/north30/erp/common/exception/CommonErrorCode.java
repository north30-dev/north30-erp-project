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
public enum ErrorCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 通用：参数错误 */
    PARAM_ERROR(10001, "参数错误"),

    /** 通用：未登录或凭证已过期 */
    UNAUTHORIZED(10002, "未登录或凭证已过期"),

    /** 通用：无权限访问 */
    FORBIDDEN(10003, "无权限访问"),

    /** 通用：资源不存在 */
    NOT_FOUND(10004, "资源不存在"),

    /** 通用：数据冲突（乐观锁/唯一约束等） */
    CONFLICT(10005, "数据冲突，请刷新后重试"),

    /** 通用：系统内部错误（兜底） */
    SYSTEM_ERROR(10999, "系统内部错误");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
