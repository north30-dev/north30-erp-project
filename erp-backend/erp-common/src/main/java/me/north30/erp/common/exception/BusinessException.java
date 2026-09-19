package me.north30.erp.common.exception;

import lombok.Getter;

/**
 * 业务异常：业务校验失败时直接抛出，由 {@link GlobalExceptionHandler} 统一转换为响应体，
 * 严禁在业务代码中 try-catch 吞异常。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 构造函数：根据业务错误码创建异常。
     * @param errorCode 业务错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数：根据业务错误码和自定义提示消息创建异常。
     * @param errorCode 业务错误码
     * @param customMessage 自定义提示消息
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数：根据业务状态码和自定义提示消息创建异常。
     * @param code 业务状态码
     * @param customMessage 自定义提示消息
     */
    public BusinessException(int code, String customMessage) {
        super(customMessage);
        this.code = code;
    }
}
