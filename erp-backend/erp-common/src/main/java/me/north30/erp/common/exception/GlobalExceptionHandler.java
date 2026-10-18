package me.north30.erp.common.exception;

import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.result.Result;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器：HTTP 状态码承载错误类别，响应体 code 承载精确业务码，前端 axios 错误拦截器统一处理非 2xx。
 * <p>职责边界：</p>
 * <ul>
 *     <li>401 未认证由 Security Filter 层的 AuthenticationEntryPoint 负责，不进入本类；</li>
 *     <li>过滤器层的 403 由 SecurityConfig 的 AccessDeniedHandler 负责，与本类的方法级 403 响应体结构一致；</li>
 *     <li>幂等键冲突（10603）待幂等框架落地后补充；外部接口异常（10501-10599）待 OA/税控集成后补充。</li>
 * </ul>
 * <p>处理矩阵（HTTP 状态 → body.code）：</p>
 * <ul>
 *     <li>400 参数类：校验失败/绑定失败 10001、请求体不可读 10001、参数类型转换失败 10001</li>
 *     <li>403 鉴权类：方法级越权 10003</li>
 *     <li>404 路由类：资源/路径不存在 10004</li>
 *     <li>409 冲突类：乐观锁 10601、唯一约束 10602</li>
 *     <li>422 业务类：BusinessException 携带业务码（11001-18999）</li>
 *     <li>500 兜底类：未预期系统错误 10999（日志含堆栈，响应不回传）</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：可预期的业务规则失败（唯一性守卫、库存不足、单据不存在等业务代码抛出）。
     * <p>HTTP 422，body.code 取异常携带的模块业务码（11001-18999），message 直接透出业务标识。
     * 仅记录 warn 级别日志，不打堆栈。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        // 业务规则失败 → HTTP 422，响应体 code 携带精确业务码（如 14001 库存不足）
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(Result.failure(e.getCode(), e.getMessage()));
    }

    /**
     * 请求体校验失败（@Valid 修饰 @RequestBody DTO，字段违反 @NotNull/@Size 等注解约束）。
     * <p>HTTP 400，body.code=10001，message 取第一条字段错误的注解提示。
     * GET 查询对象缺参不触发本处理器——DTO 字段为 null 时由 Service normalize 兜底。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = resolveFieldErrorMessage(e);
        log.warn("参数校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 表单绑定校验失败（@Valid 修饰非请求体参数对象，如 ModelAttribute 场景）。
     * <p>HTTP 400，body.code=10001，message 取第一条字段错误的注解提示，与请求体校验口径一致。</p>
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = resolveFieldErrorMessage(e);
        log.warn("参数绑定校验失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 静态资源或接口路径不存在（无匹配路由/资源，Spring Boot 3.2+ 默认抛出本异常）。
     * <p>HTTP 404，body.code=10004。注意与参数类型转换失败区分：路径对不上任何路由走这里，
     * 路由匹配但值转不了（如 /users/abc）走 MethodArgumentTypeMismatchException（400）。</p>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源不存在：{}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Result.failure(CommonErrorCode.NOT_FOUND.getCode(), CommonErrorCode.NOT_FOUND.getMessage()));
    }

    /**
     * 方法级约束校验失败（@Validated 类上校验 @RequestParam/@PathVariable 的 @Min 等约束）。
     * <p>HTTP 400，body.code=10001，message 直接透出异常描述（含属性路径与提示）。</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束校验失败：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), e.getMessage()));
    }

    /**
     * 方法级鉴权失败（@PreAuthorize/@PostAuthorize 抛出 Access Denied）：返回 HTTP 403。
     * <p>@EnableMethodSecurity 的拒绝发生在 MVC 层，Advice 可捕获；过滤器层的 403 仍由
     * SecurityConfig 的 AccessDeniedHandler 负责，二者响应体结构一致。</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("方法级鉴权拒绝：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Result.failure(CommonErrorCode.FORBIDDEN.getCode(), CommonErrorCode.FORBIDDEN.getMessage()));
    }

    /**
     * 请求体缺失或 JSON 格式非法（未携带 body / Content-Type 不符 / 反序列化失败）。
     * <p>HTTP 400，body.code=10001（文档旧口径 10002 已被 UNAUTHORIZED 占用，统一归参数段），
     * message 固定文案，不透出反序列化细节。</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体不可读：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), "请求体缺失或格式错误"));
    }

    /**
     * 路径参数/查询参数类型转换失败（如 /users/abc 绑定 Long id、?pageNum=abc 绑定 Integer）。
     * <p>路由模式能匹配但值无法转换为目标类型时抛出；HTTP 400，body.code=10001，
     * message 提示具体参数名（如「参数 id 类型错误」）。</p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = "参数 " + e.getName() + " 类型错误";
        log.warn("参数类型转换失败：{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * HTTP 请求方法不支持（如对 GET 接口发 POST）。
     * <p>HTTP 405，body.code=10001（归参数段），message 固定文案，不透出支持的方法列表。</p>
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HTTP 方法不支持：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Result.failure(CommonErrorCode.PARAM_ERROR.getCode(), "请求方法不被支持"));
    }

    /**
     * 乐观锁冲突异常：返回 HTTP 409。
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Result<Void>> handleOptimisticLockingFailureException(OptimisticLockingFailureException e) {
        log.warn("乐观锁冲突：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Result.failure(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(), CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessage()));
    }

    /**
     * 唯一约束冲突异常（单据编号/编码重复等）：返回 HTTP 409。
     * <p>由数据库唯一索引冲突经 Spring 异常翻译抛出，与乐观锁冲突（并发修改）同属数据冲突族，
     * 响应体 code 固定 10602，提示客户端更换编号后重试。</p>
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Result.failure(CommonErrorCode.DUPLICATE_KEY.getCode(), CommonErrorCode.DUPLICATE_KEY.getMessage()));
    }

    /**
     * 兜底异常：未预期的系统错误，返回 HTTP 500，body.code=10999。
     * <p>error 级别日志携带完整堆栈用于排查，响应体固定文案，不回传堆栈与内部细节。
     * 新增具体异常处理器后应确保其优先级在本方法之前生效（精确类型优先于 Exception）。</p>
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
