package me.north30.erp.system.dept.controller;

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
import me.north30.erp.common.result.Result;
import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptMoveDTO;
import me.north30.erp.system.dept.dto.DeptStatusDTO;
import me.north30.erp.system.dept.dto.DeptTreeQueryDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.service.SysDeptApplicationService;
import me.north30.erp.system.dept.vo.DeptMutationVO;
import me.north30.erp.system.dept.vo.DeptStatusVO;
import me.north30.erp.system.dept.vo.DeptTreeVO;
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

import java.util.List;

/**
 * 组织与部门管理接口（/api/system/depts，接口文档 5.4，29-32 号接口）。
 */
@Tag(name = "组织部门管理", description = "/api/system/depts，接口文档 5.4")
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class SysDeptController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18026, "message": "组织编码已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysDeptApplicationService sysDeptApplicationService;

    /**
     * 5.4.1 组织部门树查询。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    @Operation(summary = "组织部门树查询", description = "权限点 system:dept:list；一次查全量后内存组树，按 deptSort 排序")
    public Result<List<DeptTreeVO>> tree(@ParameterObject DeptTreeQueryDTO query) {
        return Result.success(sysDeptApplicationService.listTree(query));
    }

    /**
     * 5.4.2 新增组织部门。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:create')")
    @Operation(summary = "新增组织部门", description = "权限点 system:dept:create；层级上限 5 级，自动计算祖级路径与层级")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18026 组织编码已存在/18024 上级组织不存在/18027 组织层级超过上限）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeptMutationVO> create(@Valid @RequestBody DeptCreateDTO dto) {
        return Result.success(sysDeptApplicationService.create(dto));
    }

    /**
     * 5.4.3 修改组织部门。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:update')")
    @Operation(summary = "修改组织部门", description = "权限点 system:dept:update；父级变更时环检测并级联重算下级祖级路径与层级，乐观锁按 version 校验")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18024 组织不存在/10001 上级不可选自身或自身下级/18027 组织层级超过上限/10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeptMutationVO> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        return Result.success(sysDeptApplicationService.update(id, dto));
    }

    /**
     * 5.4.4 删除组织部门。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @Operation(summary = "删除组织部门", description = "权限点 system:dept:delete；逻辑删除，删除前校验下级组织与挂靠用户")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18024 组织不存在/18025 存在下级组织或已绑定用户不可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeptMutationVO> delete(@PathVariable Long id) {
        return Result.success(sysDeptApplicationService.delete(id));
    }

    /**
     * 变更组织部门状态（启用/停用，08 文档阶段四状态开关统一）。
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:dept:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "变更组织部门状态", description = "权限点 system:dept:update；乐观锁冲突返回 409/10601")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18024 组织不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeptStatusVO> changeStatus(@PathVariable Long id, @Valid @RequestBody DeptStatusDTO dto) {
        return Result.success(sysDeptApplicationService.changeStatus(id, dto));
    }

    /**
     * 组织上移/下移（08 文档阶段四排序统一）。
     */
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('system:dept:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "组织上移/下移", description = "权限点 system:dept:update；同级规范化重排（10,20,30…）后相邻交换，乐观锁冲突返回 409/10601，已处于顶部/底部返回 422/10001")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18024 组织不存在/10001 无法移动）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<Void> move(@PathVariable Long id, @Valid @RequestBody DeptMoveDTO dto) {
        sysDeptApplicationService.move(id, dto);
        return Result.success();
    }
}
