package me.north30.erp.common.exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * GlobalExceptionHandlerTest 测试数据工厂：统一构造异常对象与字段校验错误。
 * <p>BindingResult 由测试侧提供 Mockito mock，工厂只负责包装异常与填充数据。</p>
 */
final class GlobalExceptionHandlerTestFactory {

    private GlobalExceptionHandlerTestFactory() {
    }

    /**
     * 构造业务异常：自定义业务码 + 自定义消息。
     */
    static BusinessException businessException(int code, String customMessage) {
        return new BusinessException(code, customMessage);
    }

    /**
     * 构造业务异常：错误码枚举 + 自定义消息。
     */
    static BusinessException businessException(ErrorCode errorCode, String customMessage) {
        return new BusinessException(errorCode, customMessage);
    }

    /**
     * 构造字段级校验错误。
     */
    static FieldError fieldError(String objectName, String field, String defaultMessage) {
        return new FieldError(objectName, field, defaultMessage);
    }

    /**
     * 构造请求体校验异常（@Valid 校验失败场景）。
     */
    static MethodArgumentNotValidException methodArgumentNotValidException(BindingResult bindingResult) {
        return new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);
    }

    /**
     * 构造表单绑定校验异常。
     */
    static BindException bindException(BindingResult bindingResult) {
        return new BindException(bindingResult);
    }

    /**
     * 构造静态资源/接口路径不存在异常。
     */
    static NoResourceFoundException noResourceFoundException(String requestUri, String resourcePath) {
        return new NoResourceFoundException(HttpMethod.GET, requestUri, resourcePath);
    }

    /**
     * 构造乐观锁冲突异常（UpdateGuardUtil 检测到 updateCount=0 时抛出的消息）。
     */
    static OptimisticLockingFailureException optimisticLockingFailureException() {
        return new OptimisticLockingFailureException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessage());
    }

    /**
     * 构造唯一约束冲突异常（数据库唯一索引冲突场景）。
     */
    static DuplicateKeyException duplicateKeyException(String constraintMessage) {
        return new DuplicateKeyException(constraintMessage);
    }

    /**
     * 构造方法级约束校验异常（@Validated 类上校验 @RequestParam/@PathVariable 场景）。
     */
    static ConstraintViolationException constraintViolationException() {
        // 用内置违反集合构造：属性路径 + 提示
        return new ConstraintViolationException("id: 不能小于 1", java.util.Set.of());
    }

    /**
     * 构造请求体不可读异常（JSON 解析失败/请求体缺失场景）。
     */
    static HttpMessageNotReadableException httpMessageNotReadableException() {
        return new HttpMessageNotReadableException("JSON parse error", new dummyHttpInputMessage());
    }

    /**
     * 构造方法级鉴权失败异常（@PreAuthorize 拒绝场景）。
     */
    static AccessDeniedException accessDeniedException(String reason) {
        return new AccessDeniedException(reason);
    }

    /**
     * 构造参数类型转换失败异常（路由匹配但值无法转换为目标类型）。
     */
    static MethodArgumentTypeMismatchException methodArgumentTypeMismatchException(String name, String value,
        Class<?> requiredType) {
        return new MethodArgumentTypeMismatchException(value, requiredType, name, dummyMethodParameter(),
            new NumberFormatException("For input string: \"" + value + "\""));
    }

    /**
     * 构造 HTTP 方法不支持异常（对 GET 接口发 POST 场景）。
     */
    static HttpRequestMethodNotSupportedException httpRequestMethodNotSupportedException(String method) {
        return new HttpRequestMethodNotSupportedException(method);
    }

    /**
     * 空的 HttpInputMessage 占位：仅满足 HttpMessageNotReadableException 构造参数。
     */
    private static final class dummyHttpInputMessage implements HttpInputMessage {
        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }

    /**
     * 通过反射构造指向虚拟 Controller 方法的 MethodParameter（异常构造必需）。
     */
    private static MethodParameter dummyMethodParameter() {
        try {
            Method method = GlobalExceptionHandlerTestFactory.class
                .getDeclaredMethod("dummyEndpoint", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("构造测试用 MethodParameter 失败", ex);
        }
    }

    /**
     * 虚拟端点方法：仅供 MethodParameter 反射引用。
     */
    @SuppressWarnings("unused")
    private static void dummyEndpoint(String requestBody) {
        // 空实现：仅提供方法签名
    }
}
