package me.north30.erp.system.importtask.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.importtask.service.SysImportTaskService;
import me.north30.erp.system.importtask.vo.ImportResultVO;
import me.north30.erp.system.importtask.vo.ImportTaskVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 批量导入接口（/api/system/imports，接口文档 5.10）。
 * <p>D2 仅查询能力：任务列表 + 结果查询；导入执行（POST）由后续模块按业务域落地。
 * 任务列表接口文档未单列权限点，沿用 system:import:detail。</p>
 */
@Tag(name = "批量导入", description = "/api/system/imports，接口文档 5.10")
@RestController
@RequestMapping("/api/system/imports")
@RequiredArgsConstructor
public class ImportTaskController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 10503, "message": "导入任务不存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysImportTaskService importTaskService;

    /**
     * 导入任务分页查询（D2 无任务表，返回空分页）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:import:detail')")
    @Operation(summary = "导入任务分页查询", description = "权限点 system:import:detail（接口文档未单列任务列表权限点，沿用 detail）；D2 无任务表，恒返回空分页")
    public Result<PageResult<ImportTaskVO>> page(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                 @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(importTaskService.pageTasks(pageNum, pageSize));
    }

    /**
     * 5.10.2 导入结果查询（权限：system:import:detail）。
     */
    @GetMapping("/{taskId}/result")
    @PreAuthorize("hasAuthority('system:import:detail')")
    @Operation(summary = "导入结果查询", description = "权限点 system:import:detail")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 导入任务号为空、10503 导入任务不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<ImportResultVO> result(@PathVariable String taskId) {
        return Result.success(importTaskService.getResult(taskId));
    }
}
