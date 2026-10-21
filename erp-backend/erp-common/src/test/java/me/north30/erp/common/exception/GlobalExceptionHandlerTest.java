package me.north30.erp.common.exception;

import me.north30.erp.common.result.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * GlobalExceptionHandler 单元测试：验证各类异常到 HTTP 状态码与统一响应体 Result 的映射关系。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 全局异常处理")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Nested
    @DisplayName("handleBusinessException：业务异常")
    class HandleBusinessExceptionTest {

        @Test
        @DisplayName("业务异常映射为 HTTP 422，响应体携带业务码与消息")
        void shouldReturn422WithBusinessCodeAndMessage_whenBusinessException() {
            // Given
            BusinessException e = GlobalExceptionHandlerTestFactory.businessException(14001, "库存不足");

            // When
            ResponseEntity<Result<Void>> response = handler.handleBusinessException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(14001);
            assertThat(response.getBody().message()).isEqualTo("库存不足");
        }

        @Test
        @DisplayName("错误码枚举 + 自定义消息时，code 取枚举值、message 取自定义值")
        void shouldKeepEnumCodeAndCustomMessage_whenBusinessExceptionBuiltFromErrorCode() {
            // Given
            BusinessException e = GlobalExceptionHandlerTestFactory.businessException(
                CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT, "订单已被其他人修改");

            // When
            ResponseEntity<Result<Void>> response = handler.handleBusinessException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code())
                .isEqualTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode());
            assertThat(response.getBody().message()).isEqualTo("订单已被其他人修改");
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentNotValidException：请求体校验异常")
    class HandleMethodArgumentNotValidExceptionTest {

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("存在字段错误时返回 HTTP 400，消息取第一条字段错误的提示")
        void shouldReturn400WithFirstFieldErrorMessage_whenFieldErrorPresent() {
            // Given
            given(bindingResult.getFieldError()).willReturn(
                GlobalExceptionHandlerTestFactory.fieldError("orderForm", "quantity", "数量必须大于0"));
            MethodArgumentNotValidException e = GlobalExceptionHandlerTestFactory
                .methodArgumentNotValidException(bindingResult);

            // When
            ResponseEntity<Result<Void>> response = handler.handleMethodArgumentNotValidException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo("数量必须大于0");
        }

        @Test
        @DisplayName("无字段级错误（如全局错误）时回退为通用参数错误提示")
        void shouldReturnFallbackParamErrorMessage_whenNoFieldError() {
            // Given
            given(bindingResult.getFieldError()).willReturn(null);
            MethodArgumentNotValidException e = GlobalExceptionHandlerTestFactory
                .methodArgumentNotValidException(bindingResult);

            // When
            ResponseEntity<Result<Void>> response = handler.handleMethodArgumentNotValidException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo(CommonErrorCode.PARAM_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("handleBindExceptionException：表单绑定校验异常")
    class HandleBindExceptionTest {

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("存在字段错误时返回 HTTP 400，消息取第一条字段错误的提示")
        void shouldReturn400WithFirstFieldErrorMessage_whenFieldErrorPresent() {
            // Given
            given(bindingResult.getFieldError()).willReturn(
                GlobalExceptionHandlerTestFactory.fieldError("orderForm", "customerName", "客户名称不能为空"));
            BindException e = GlobalExceptionHandlerTestFactory.bindException(bindingResult);

            // When
            ResponseEntity<Result<Void>> response = handler.handleBindException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo("客户名称不能为空");
        }

        @Test
        @DisplayName("无字段级错误（如全局错误）时回退为通用参数错误提示")
        void shouldReturnFallbackParamErrorMessage_whenNoFieldError() {
            // Given
            given(bindingResult.getFieldError()).willReturn(null);
            BindException e = GlobalExceptionHandlerTestFactory.bindException(bindingResult);

            // When
            ResponseEntity<Result<Void>> response = handler.handleBindException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo(CommonErrorCode.PARAM_ERROR.getMessage());
        }
    }

    @Nested
    @DisplayName("handleNoResourceFound：资源不存在异常")
    class HandleNoResourceFoundTest {

        @Test
        @DisplayName("路径不存在返回 HTTP 404，响应体 code 取业务码 10004 且不泄漏资源路径")
        void shouldReturn404WithFixedMessage_whenResourceNotFound() {
            // Given
            NoResourceFoundException e = GlobalExceptionHandlerTestFactory
                .noResourceFoundException("/api/unknown", "unknown");

            // When
            ResponseEntity<Result<Void>> response = handler.handleNoResourceFoundException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode());
            assertThat(response.getBody().message()).isEqualTo(CommonErrorCode.NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("handleOptimisticLockingFailureException：乐观锁冲突异常")
    class HandleOptimisticLockingFailureExceptionTest {

        @Test
        @DisplayName("乐观锁冲突返回 HTTP 409，响应体 code 固定 10601，不向客户端泄漏异常细节")
        void shouldReturn409WithOptimisticLockCode_whenOptimisticLockingFailure() {
            // Given
            OptimisticLockingFailureException e = GlobalExceptionHandlerTestFactory.optimisticLockingFailureException();

            // When
            ResponseEntity<Result<Void>> response = handler.handleOptimisticLockingFailureException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode());
            assertThat(response.getBody().message())
                .isEqualTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessage());
        }
    }

    @Nested
    @DisplayName("handleDuplicateKeyException：唯一约束冲突异常")
    class HandleDuplicateKeyExceptionTest {

        @Test
        @DisplayName("唯一约束冲突返回 HTTP 409，响应体 code 固定 10602，不向客户端泄漏约束名")
        void shouldReturn409WithDuplicateKeyCode_whenDuplicateKey() {
            // Given
            DuplicateKeyException e = GlobalExceptionHandlerTestFactory
                .duplicateKeyException("duplicate key value violates unique constraint \"uk_pur_order_code\"");

            // When
            ResponseEntity<Result<Void>> response = handler.handleDuplicateKeyException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.DUPLICATE_KEY.getCode());
            assertThat(response.getBody().message())
                .isEqualTo(CommonErrorCode.DUPLICATE_KEY.getMessage())
                .isNotEqualTo(e.getMessage());
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentTypeMismatchException：参数类型转换失败")
    class HandleMethodArgumentTypeMismatchExceptionTest {

        @Test
        @DisplayName("路径参数类型转换失败返回 HTTP 400，message 提示具体参数名")
        void shouldReturn400WithParamName_whenTypeMismatch() {
            // Given（/api/system/users/abc 绑定 Long id）
            MethodArgumentTypeMismatchException e = GlobalExceptionHandlerTestFactory
                .methodArgumentTypeMismatchException("id", "abc", Long.class);

            // When
            ResponseEntity<Result<Void>> response = handler.handleMethodArgumentTypeMismatchException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo("参数 id 类型错误");
        }
    }

    @Nested
    @DisplayName("handleMethodNotSupportedException：HTTP 方法不支持")
    class HandleMethodNotSupportedExceptionTest {

        @Test
        @DisplayName("HTTP 方法不匹配返回 HTTP 405，响应体 code=10001 且不透出支持的方法列表")
        void shouldReturn405WithFixedMessage_whenMethodNotSupported() {
            // Given（对 GET 接口发 DELETE）
            HttpRequestMethodNotSupportedException e = GlobalExceptionHandlerTestFactory
                .httpRequestMethodNotSupportedException("DELETE");

            // When
            ResponseEntity<Result<Void>> response = handler.handleMethodNotSupportedException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo("请求方法不被支持");
        }
    }

    @Nested
    @DisplayName("handleConstraintViolationException：方法级约束校验异常")
    class HandleConstraintViolationExceptionTest {

        @Test
        @DisplayName("方法级约束校验失败返回 HTTP 400，code=10001，message 透出异常描述")
        void shouldReturn400WithExceptionMessage_whenConstraintViolation() {
            // Given
            ConstraintViolationException e = GlobalExceptionHandlerTestFactory.constraintViolationException();

            // When
            ResponseEntity<Result<Void>> response = handler.handleConstraintViolationException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo(e.getMessage());
        }
    }

    @Nested
    @DisplayName("handleHttpMessageNotReadableException：请求体不可读异常")
    class HandleHttpMessageNotReadableExceptionTest {

        @Test
        @DisplayName("请求体缺失或 JSON 非法返回 HTTP 400，code=10001，固定文案不泄漏反序列化细节")
        void shouldReturn400WithFixedMessage_whenBodyNotReadable() {
            // Given
            HttpMessageNotReadableException e = GlobalExceptionHandlerTestFactory.httpMessageNotReadableException();

            // When
            ResponseEntity<Result<Void>> response = handler.handleHttpMessageNotReadableException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo("请求体缺失或格式错误");
        }
    }

    @Nested
    @DisplayName("handleAccessDeniedException：方法级鉴权失败")
    class HandleAccessDeniedExceptionTest {

        @Test
        @DisplayName("方法级越权返回 HTTP 403，code=10003，固定文案不透出权限点细节")
        void shouldReturn403WithForbiddenCode_whenAccessDenied() {
            // Given
            AccessDeniedException e = GlobalExceptionHandlerTestFactory
                .accessDeniedException("Access Denied: system:user:delete");

            // When
            ResponseEntity<Result<Void>> response = handler.handleAccessDeniedException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.FORBIDDEN.getCode());
            assertThat(response.getBody().message()).isEqualTo(CommonErrorCode.FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("handleException：兜底异常")
    class HandleExceptionTest {

        @Test
        @DisplayName("未预期异常返回 HTTP 500 与系统内部错误提示，不向客户端泄漏异常细节")
        void shouldReturn500WithSystemError_whenUnexpectedException() {
            // Given
            Exception e = new RuntimeException("数据库连接失败");

            // When
            ResponseEntity<Result<Void>> response = handler.handleException(e);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.SYSTEM_ERROR.getCode());
            assertThat(response.getBody().message())
                .isEqualTo(CommonErrorCode.SYSTEM_ERROR.getMessage())
                .isNotEqualTo("数据库连接失败");
        }
    }
}
