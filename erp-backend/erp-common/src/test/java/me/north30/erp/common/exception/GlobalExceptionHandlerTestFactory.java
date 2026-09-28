package me.north30.erp.common.exception;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
