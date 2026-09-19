package me.north30.erp.common.exception;

import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.result.Result;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：HTTP 状态码承载错误类别（400/401/404/409/422/500），
 * 响应体 code 承载精确的业务码（10001-16999），前端在 axios 错误拦截器中统一处理非 2xx。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：可预期的业务规则失败，仅记录 warn 级别日志（不打堆栈）。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        // 业务规则失败 → HTTP 422，响应体 code 携带精确业务码（如 14001 库存不足）
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(Result.failure(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验异常（@Valid/@Validated 请求体校验失败）：取第一条字段错误提示返回。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = resolveFieldErrorMessage(e);
        log.warn("参数校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 表单绑定校验异常：取第一条字段错误提示返回。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = resolveFieldErrorMessage(e);
        log.warn("参数绑定校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 静态资源/接口路径不存在：返回 HTTP 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在：{}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Result.failure(HttpStatus.NOT_FOUND.value(), "请求的资源不存在"));
    }

    /**
     * 兜底异常：未预期的系统错误，记录 error 级别日志并携带完整堆栈。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统内部错误：{}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.failure(CommonErrorCode.SYSTEM_ERROR.getCode(), CommonErrorCode.SYSTEM_ERROR.getMessage()));
    }

    /**
     * 从绑定结果中取第一条字段错误的默认提示。
     * <p>优先返回具体字段（FieldError）上的校验消息（来自 @NotNull/@Size 等注解的 message 属性），
     * 没有任何字段级错误时（如全局错误），回退到通用的参数错误提示。</p>
     */
    private String resolveFieldErrorMessage(BindException e) {
        // 从绑定结果中取出第一条字段级校验错误
        FieldError fieldError = e.getBindingResult().getFieldError();
        // 有字段错误则使用注解上定义的提示消息，否则回退为通用参数错误提示
        return fieldError != null ? fieldError.getDefaultMessage() : CommonErrorCode.PARAM_ERROR.getMessage();
    }
}
