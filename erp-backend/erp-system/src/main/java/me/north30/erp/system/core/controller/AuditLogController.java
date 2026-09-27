package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.AuditLogQueryDTO;
import me.north30.erp.system.core.service.ISysLogQueryService;
import me.north30.erp.system.core.vo.AuditLogVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志接口（/api/system/audit-logs，接口文档 5.8.1/5.8.2）。
 * <p>日志只增不改不删，仅提供只读查询；@PreAuthorize 生效需主配置开启 @EnableMethodSecurity。</p>
 */
@RestController
@RequestMapping("/api/system/audit-logs")
public class AuditLogController {

    private final ISysLogQueryService logQueryService;

    public AuditLogController(ISysLogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    /**
     * 5.8.1 审计日志分页查询（权限：system:auditlog:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:auditlog:list')")
    public Result<PageResult<AuditLogVO>> page(@Valid AuditLogQueryDTO query) {
        return Result.success(logQueryService.pageAuditLogs(query));
    }

    /**
     * 5.8.2 审计日志详情（权限：system:auditlog:detail），含变更前后 JSON 与变更字段清单。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:auditlog:detail')")
    public Result<AuditLogVO> detail(@PathVariable Long id) {
        return Result.success(logQueryService.getAuditLogDetail(id));
    }
}
