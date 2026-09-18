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

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
