package me.north30.erp.system.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.config.dto.ConfigCreateDTO;
import me.north30.erp.system.config.dto.ConfigQueryDTO;
import me.north30.erp.system.config.dto.ConfigStatusDTO;
import me.north30.erp.system.config.dto.ConfigUpdateDTO;
import me.north30.erp.system.config.service.SysConfigApplicationService;
import me.north30.erp.system.config.vo.ConfigStatusVO;
import me.north30.erp.system.config.vo.ConfigUpdateVO;
import me.north30.erp.system.config.vo.ConfigVO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统参数管理接口（/api/system/configs，接口文档 5.6，41-44 号接口）。
 */
@Tag(name = "系统参数", description = "/api/system/configs，接口文档 5.6")
@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
public class SysConfigController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 10602, "message": "参数键已存在，请检查后重试", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysConfigApplicationService sysConfigApplicationService;

    /**
     * 5.6.1 系统参数分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:config:list')")
    @Operation(summary = "系统参数分页查询", description = "权限点 system:config:list")
    public Result<PageResult<ConfigVO>> page(@ParameterObject @Valid ConfigQueryDTO query) {
        return Result.success(sysConfigApplicationService.page(query));
    }

    /**
     * 5.6.2 新增系统参数。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:config:create')")
    @Operation(summary = "新增系统参数", description = "权限点 system:config:create")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 参数值类型非法、10602 参数键已存在、18034 参数值与值类型不匹配）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> create(@Valid @RequestBody ConfigCreateDTO dto) {
        return Result.success(sysConfigApplicationService.create(dto));
    }

    /**
     * 5.6.3 修改系统参数。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:update')")
    @Operation(summary = "修改系统参数", description = "权限点 system:config:update")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18032 系统参数不存在、18034 参数值与值类型不匹配、10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<ConfigUpdateVO> update(@PathVariable Long id, @Valid @RequestBody ConfigUpdateDTO dto) {
        return Result.success(sysConfigApplicationService.update(id, dto));
    }

    /**
     * 5.6.4 删除系统参数。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:delete')")
    @Operation(summary = "删除系统参数", description = "权限点 system:config:delete")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18032 系统参数不存在、18033 内置系统参数不可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeleteResultVO> delete(@PathVariable Long id) {
        return Result.success(sysConfigApplicationService.delete(id));
    }

    /**
     * 变更系统参数状态（启用/停用，08 文档阶段四状态开关统一）。
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:config:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "变更系统参数状态", description = "权限点 system:config:update；内置参数（is_system=1）不可停用（18033），乐观锁冲突返回 409/10601")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18032 系统参数不存在、18033 内置系统参数不可停用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<ConfigStatusVO> changeStatus(@PathVariable Long id, @Valid @RequestBody ConfigStatusDTO dto) {
        return Result.success(sysConfigApplicationService.changeStatus(id, dto));
    }
}
