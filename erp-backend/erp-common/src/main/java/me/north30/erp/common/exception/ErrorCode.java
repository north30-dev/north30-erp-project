package me.north30.erp.common.exception;

/**
 * 错误码统一接口。
 * <p>各业务模块通过实现该接口，定义自己的错误码枚举。</p>
 */
public interface ErrorCode {
    /** 错误码 */
    int getCode();

    /** 错误消息 */
    String getMessage();
}
