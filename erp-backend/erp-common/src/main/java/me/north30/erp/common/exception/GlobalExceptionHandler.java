package me.north30.erp.common.exception;

import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：错误码与 HTTP 状态解耦，所有异常的 HTTP 状态统一为 200，
 * 前端根据响应体中的业务码（code）判断成功与否并做相应处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：可预期的业务规则失败，仅记录 warn 级别日志（不打堆栈）。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@Valid/@Validated 请求体校验失败）：取第一条字段错误提示返回。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = resolveFieldErrorMessage(e);
        log.warn("参数校验失败：{}", message);
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 表单绑定校验异常：取第一条字段错误提示返回。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = resolveFieldErrorMessage(e);
        log.warn("参数绑定校验失败：{}", message);
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 兜底异常：未预期的系统错误，记录 error 级别日志并携带完整堆栈。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统内部错误：{}", e.getMessage(), e);
        return Result.failure(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 从绑定结果中取第一条字段错误的默认提示。
     */
    private String resolveFieldErrorMessage(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        return fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.PARAM_ERROR.getMessage();
    }
}
