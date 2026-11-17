package me.north30.erp.base.material.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.base.material.dto.MaterialCodeMapCreateDTO;
import me.north30.erp.base.material.dto.MaterialCodeMapQueryDTO;
import me.north30.erp.base.material.service.MaterialApplicationService;
import me.north30.erp.base.material.vo.MaterialCodeMapCreateVO;
import me.north30.erp.base.material.vo.MaterialCodeMapVO;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;

/**
 * 物料旧码映射接口（/api/base/material-code-maps，接口文档 6.2.14/6.2.15，73/74 号接口）。
 * <p>一物多码合并追溯（迁移用）：old_code + source_dept 唯一（11015），新物料必须存在（11002）。</p>
 */
@Tag(name = "物料旧码映射", description = "/api/base/material-code-maps，接口文档 6.2.14-6.2.15")
@RestController
@RequestMapping("/api/base/material-code-maps")
@RequiredArgsConstructor
public class MaterialCodeMapController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 11015, "message": "旧物料编码 IC-001（采购部）映射已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final MaterialApplicationService materialApplicationService;

    /**
     * 6.2.14 物料旧码映射分页查询（73 号接口，base:material:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('base:material:list')")
    @Operation(summary = "旧码映射分页查询", description = "权限点 base:material:list；旧编码/新物料编码模糊，来源部门/合并组/迁移批次精确匹配")
    public Result<PageResult<MaterialCodeMapVO>> page(@ParameterObject @Valid MaterialCodeMapQueryDTO query) {
        return Result.success(materialApplicationService.pageCodeMaps(query));
    }

    /**
     * 6.2.15 新增物料旧码映射（74 号接口，base:material:update）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('base:material:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.CREATE)
    @Operation(summary = "新增旧码映射", description = "权限点 base:material:update；old_code + source_dept 唯一，新物料须已存在并由服务端回填新物料编码")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 新物料不存在/11015 旧码映射已存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialCodeMapCreateVO> create(@Valid @RequestBody MaterialCodeMapCreateDTO dto) {
        return Result.success(materialApplicationService.createCodeMap(dto));
    }
}
