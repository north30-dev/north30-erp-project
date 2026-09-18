package me.north30.erp.common.result;

import lombok.Data;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 统一响应体：所有接口统一返回该结构。
 * <p>业务码与 HTTP 状态解耦，前端按 code 判断业务结果。</p>
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {

    /** 业务状态码：200 成功，其余见 {@link ErrorCode} */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 响应时间戳（毫秒） */
    private long timestamp;

    /** 链路追踪 ID（预留：后续接入 MDC 后自动填充） */
    private String traceId;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = ErrorCode.SUCCESS.getCode();
        result.message = ErrorCode.SUCCESS.getMessage();
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return failure(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> failure(int code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.timestamp = System.currentTimeMillis();
        return result;
    }
}
