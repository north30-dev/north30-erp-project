package me.north30.erp.base.materialcategory.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

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

import lombok.RequiredArgsConstructor;
import me.north30.erp.base.materialcategory.dto.CategoryCreateDTO;
import me.north30.erp.base.materialcategory.dto.CategoryMoveDTO;
import me.north30.erp.base.materialcategory.dto.CategoryStatusDTO;
import me.north30.erp.base.materialcategory.dto.CategoryTreeQueryDTO;
import me.north30.erp.base.materialcategory.dto.CategoryUpdateDTO;
import me.north30.erp.base.materialcategory.service.MaterialCategoryApplicationService;
import me.north30.erp.base.materialcategory.vo.CategoryMutationVO;
import me.north30.erp.base.materialcategory.vo.CategoryStatusVO;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.Result;

/**
 * 物料分类管理接口（/api/base/material-categories，接口文档 6.1，56-59 号接口）。
 * <p>move（POST /{id}/move）与启停（POST /{id}/status）为接口文档 6.1 未列、spec 要求补齐的接口，
 * 路径与权限对齐组织部门域（08 文档阶段四排序/状态开关统一）模式。</p>
 */
@Tag(name = "物料分类", description = "/api/base/material-categories，接口文档 6.1")
@RestController
@RequestMapping("/api/base/material-categories")
@RequiredArgsConstructor
public class MaterialCategoryController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 11007, "message": "分类编码 0103 已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final MaterialCategoryApplicationService materialCategoryApplicationService;

    /**
     * 6.1.1 物料分类树查询（56 号接口）：一次查全量后内存组两级树。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('base:materialcategory:list')")
    @Operation(summary = "物料分类树查询", description = "权限点 base:materialcategory:list；一次查全量后内存组两级树，按 categorySort 排序")
    public Result<List<CategoryTreeVO>> tree(@ParameterObject CategoryTreeQueryDTO query) {
        return Result.success(materialCategoryApplicationService.listTree(query));
    }

    /**
     * 6.1.2 新增物料分类（57 号接口）：编码唯一 11007、层级上限 2 级与编码联动 10001。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('base:materialcategory:create')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.CREATE)
    @Operation(summary = "新增物料分类", description = "权限点 base:materialcategory:create；层级上限 2 级，编码联动（一级 2 位、二级 4 位且以父级编码为前缀）")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 层级超限或编码格式非法/11007 分类编码已存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<CategoryMutationVO> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success(materialCategoryApplicationService.create(dto));
    }

    /**
     * 6.1.3 修改物料分类（58 号接口）：编码与父级不可修改，乐观锁冲突返回 409/10601。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('base:materialcategory:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改物料分类", description = "权限点 base:materialcategory:update；编码与父级不可修改，部分更新语义（null 表示不修改），乐观锁冲突返回 409/10601")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 分类不存在/10001 参数错误）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "乐观锁冲突（10601）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<CategoryMutationVO> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateDTO dto) {
        return Result.success(materialCategoryApplicationService.update(id, dto));
    }

    /**
     * 6.1.4 删除物料分类（59 号接口）：存在下级分类或已挂载物料返回 11006，逻辑删除。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('base:materialcategory:delete')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.DELETE)
    @Operation(summary = "删除物料分类", description = "权限点 base:materialcategory:delete；逻辑删除，删除前校验下级分类与已挂载物料")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 分类不存在/11006 存在下级分类或已挂载物料不可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<CategoryMutationVO> delete(@PathVariable Long id) {
        return Result.success(materialCategoryApplicationService.delete(id));
    }

    /**
     * 物料分类上移/下移（spec 任务 2.2 补齐）：同级规范化重排后相邻交换，
     * 乐观锁冲突返回 409/10601，已处于顶部/底部返回 422/10001。
     */
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('base:materialcategory:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "物料分类上移/下移", description = "权限点 base:materialcategory:update；同级规范化重排（10,20,30…）后相邻交换，乐观锁冲突返回 409/10601，已处于顶部/底部返回 422/10001")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 分类不存在/10001 无法移动）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "乐观锁冲突（10601）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<Void> move(@PathVariable Long id, @Valid @RequestBody CategoryMoveDTO dto) {
        materialCategoryApplicationService.move(id, dto);
        return Result.success();
    }

    /**
     * 物料分类启用/停用（spec 任务 2.2 补齐）：乐观锁冲突返回 409/10601；
     * 停用分类不可挂载物料（11017）由物料域落地。
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('base:materialcategory:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "变更物料分类状态", description = "权限点 base:materialcategory:update；0-停用 1-启用，乐观锁冲突返回 409/10601；停用分类不可挂载物料（11017）由物料域落地")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 分类不存在/10001 状态值非法）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    @ApiResponse(responseCode = "409", description = "乐观锁冲突（10601）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<CategoryStatusVO> changeStatus(@PathVariable Long id, @Valid @RequestBody CategoryStatusDTO dto) {
        return Result.success(materialCategoryApplicationService.changeStatus(id, dto));
    }
}
