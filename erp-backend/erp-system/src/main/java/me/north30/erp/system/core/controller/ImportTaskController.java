package me.north30.erp.system.core.controller;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.service.SysImportTaskService;
import me.north30.erp.system.core.vo.ImportResultVO;
import me.north30.erp.system.core.vo.ImportTaskVO;
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
@RestController
@RequestMapping("/api/system/imports")
@RequiredArgsConstructor
public class ImportTaskController {

    private final SysImportTaskService importTaskService;

    /**
     * 导入任务分页查询（D2 无任务表，返回空分页）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:import:detail')")
    public Result<PageResult<ImportTaskVO>> page(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                 @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(importTaskService.pageTasks(pageNum, pageSize));
    }

    /**
     * 5.10.2 导入结果查询（权限：system:import:detail）。
     */
    @GetMapping("/{taskId}/result")
    @PreAuthorize("hasAuthority('system:import:detail')")
    public Result<ImportResultVO> result(@PathVariable String taskId) {
        return Result.success(importTaskService.getResult(taskId));
    }
}
