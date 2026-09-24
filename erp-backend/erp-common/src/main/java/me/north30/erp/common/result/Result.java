package me.north30.erp.common.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.exception.ErrorCode;
import org.slf4j.MDC;

/**
 * 统一响应体：所有接口统一返回该结构。
 * <p>业务码与 HTTP 状态解耦，前端按 code 判断业务结果。</p>
 *
 * @param <T> 响应数据类型
 * @param code 业务状态码：200 成功，其余见 {@link CommonErrorCode}
 * @param message 提示消息
 * @param data 响应数据
 * @param timestamp 响应时间戳（毫秒）
 * @param traceId 链路追踪 ID（从 MDC 自动获取）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(
    int code,
    String message,
    T data,
    long timestamp,
    String traceId
) {

    /**
     * 辅助构造器：自动填充 timestamp 和 traceId。
     * 所有工厂方法都通过这个构造器创建实例。
     */
    // TODO 异步线程/定时任务/MQ消费场景下 MDC 可能为空，后续用 TaskDecorator 统一传递
    private Result(int code, String message, T data) {
        this(code, message, data, System.currentTimeMillis(), MDC.get("traceId"));
    }

    /**
     * 成功响应
     * @return 成功响应体，数据为空时
     */
    public static <T> Result<T> success() {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应
     * @param data 响应数据
     * @return 成功响应体，数据不为空时为 data，为空时为 null
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应
     * @param message 自定义提示消息
     * @param data 响应数据
     * @return 成功响应体，数据不为空时为 data，为空时为 null
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应
     * @param errorCode 业务错误码
     * @param data 响应数据
     * @return 失败响应体，数据不为空时为 data，为空时为 null
     */
    public static <T> Result<T> failure(ErrorCode errorCode, T data) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), data);
    }

    /**
     * 失败响应
     * @param errorCode 业务错误码
     * @return 失败响应体，数据为空时
     */
    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应
     * @param code 业务状态码
     * @param message 自定义提示消息
     * @return 失败响应体，数据为空时
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断是否成功（不参与 JSON 序列化）
     */
    @JsonIgnore
    public boolean isSuccess() {
        return code == CommonErrorCode.SUCCESS.getCode();
    }
}
