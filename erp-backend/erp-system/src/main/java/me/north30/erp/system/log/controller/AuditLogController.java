package me.north30.erp.system.log.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.log.dto.AuditLogQueryDTO;
import me.north30.erp.system.log.service.SysLogQueryService;
import me.north30.erp.system.log.vo.AuditLogVO;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志接口（/api/system/audit-logs，接口文档 5.8.1/5.8.2）。
 * <p>日志只增不改不删，仅提供只读查询；@PreAuthorize 生效需主配置开启 @EnableMethodSecurity。</p>
 */
@Tag(name = "审计日志", description = "/api/system/audit-logs，接口文档 5.8.1/5.8.2")
@RestController
@RequestMapping("/api/system/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18037, "message": "审计日志不存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysLogQueryService logQueryService;

    /**
     * 5.8.1 审计日志分页查询（权限：system:auditlog:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:auditlog:list')")
    @Operation(summary = "审计日志分页查询", description = "权限点 system:auditlog:list；时间参数支持 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss，区间左闭右开")
    public Result<PageResult<AuditLogVO>> page(@ParameterObject @Valid AuditLogQueryDTO query) {
        return Result.success(logQueryService.pageAuditLogs(query));
    }

    /**
     * 5.8.2 审计日志详情（权限：system:auditlog:detail），含变更前后 JSON 与变更字段清单。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:auditlog:detail')")
    @Operation(summary = "审计日志详情", description = "权限点 system:auditlog:detail；含变更前后 JSON 与变更字段清单")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18037 审计日志不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<AuditLogVO> detail(@PathVariable Long id) {
        return Result.success(logQueryService.getAuditLogDetail(id));
    }
}
