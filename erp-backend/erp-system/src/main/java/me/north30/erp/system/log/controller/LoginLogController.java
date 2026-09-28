package me.north30.erp.system.log.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.log.dto.LoginLogQueryDTO;
import me.north30.erp.system.log.service.SysLogQueryService;
import me.north30.erp.system.log.vo.LoginLogVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志接口（/api/system/login-logs，接口文档 5.8.3）。
 */
@RestController
@RequestMapping("/api/system/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final SysLogQueryService logQueryService;

    /**
     * 5.8.3 登录日志分页查询（权限：system:loginlog:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:loginlog:list')")
    public Result<PageResult<LoginLogVO>> page(@Valid LoginLogQueryDTO query) {
        return Result.success(logQueryService.pageLoginLogs(query));
    }
}
