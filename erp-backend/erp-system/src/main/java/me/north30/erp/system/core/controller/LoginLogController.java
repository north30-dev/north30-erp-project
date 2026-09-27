package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.LoginLogQueryDTO;
import me.north30.erp.system.core.service.ISysLogQueryService;
import me.north30.erp.system.core.vo.LoginLogVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志接口（/api/system/login-logs，接口文档 5.8.3）。
 */
@RestController
@RequestMapping("/api/system/login-logs")
public class LoginLogController {

    private final ISysLogQueryService logQueryService;

    public LoginLogController(ISysLogQueryService logQueryService) {
        this.logQueryService = logQueryService;
    }

    /**
     * 5.8.3 登录日志分页查询（权限：system:loginlog:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:loginlog:list')")
    public Result<PageResult<LoginLogVO>> page(@Valid LoginLogQueryDTO query) {
        return Result.success(logQueryService.pageLoginLogs(query));
    }
}
