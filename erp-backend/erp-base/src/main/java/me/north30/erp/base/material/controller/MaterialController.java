package me.north30.erp.base.material.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.base.material.dto.MaterialAuditDTO;
import me.north30.erp.base.material.dto.MaterialBatchDeleteDTO;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.dto.MaterialDisableDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.dto.MaterialUpdateDTO;
import me.north30.erp.base.material.service.MaterialApplicationService;
import me.north30.erp.base.material.vo.MaterialBatchDeleteVO;
import me.north30.erp.base.material.vo.MaterialCreateVO;
import me.north30.erp.base.material.vo.MaterialDeleteVO;
import me.north30.erp.base.material.vo.MaterialDetailVO;
import me.north30.erp.base.material.vo.MaterialImportResultVO;
import me.north30.erp.base.material.vo.MaterialStatusVO;
import me.north30.erp.base.material.vo.MaterialVO;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;

/**
 * 物料主数据管理接口（/api/base/materials 与 /api/base/material-code-maps，
 * 接口文档 6.2，71-74 号及物料接口区间）。
 * <p>导入/导出/模板为简化口径：CSV 文件流（UTF-8 BOM），对齐 system 用户列表导出既有实现；
 * XLSX 模板与 EasyExcel 导入待 D2 导入任务体系落地后升级。</p>
 */
@Tag(name = "物料主数据", description = "/api/base/materials，接口文档 6.2")
@RestController
@RequestMapping("/api/base/materials")
@RequiredArgsConstructor
public class MaterialController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 11002, "message": "物料 M0001 不存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    /** 导出/模板文件名时间格式 */
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MaterialApplicationService materialApplicationService;

    /**
     * 6.2.1 物料分页查询（base:material:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('base:material:list')")
    @Operation(summary = "物料分页查询", description = "权限点 base:material:list；编码/名称模糊，分类/类型/状态等精确匹配")
    public Result<PageResult<MaterialVO>> page(@ParameterObject @Valid MaterialQueryDTO query) {
        return Result.success(materialApplicationService.page(query));
    }

    /**
     * 6.2.2 物料详情（base:material:detail）。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('base:material:detail')")
    @Operation(summary = "物料详情", description = "权限点 base:material:detail；不存在返回 422/11002")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialDetailVO> detail(@PathVariable Long id) {
        return Result.success(materialApplicationService.getDetail(id));
    }

    /**
     * 6.2.3 按编码查询物料（base:material:detail）。
     */
    @GetMapping("/by-code")
    @PreAuthorize("hasAuthority('base:material:detail')")
    @Operation(summary = "按编码查询物料", description = "权限点 base:material:detail；不存在返回 422/11002")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialDetailVO> getByCode(@Parameter(description = "物料编码（必填）") @RequestParam String materialCode) {
        return Result.success(materialApplicationService.getByCode(materialCode));
    }

    /**
     * 6.2.4 新增物料（base:material:create）：编码服务端生成，状态 0-新建。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('base:material:create')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.CREATE)
    @Operation(summary = "新增物料", description = "权限点 base:material:create；编码由服务端按分类编码前 4 位 + 6 位流水生成，初始状态 0-新建")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11001 编码存在/11005 分类非叶子/11008 四要素重复/11014 编码流水用尽/11016 单位非法/11017 分类停用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialCreateVO> create(@Valid @RequestBody MaterialCreateDTO dto) {
        return Result.success(materialApplicationService.create(dto));
    }

    /**
     * 6.2.5 修改物料（base:material:update，乐观锁）。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('base:material:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改物料", description = "权限点 base:material:update；部分更新语义，null 表示不修改；已启用物料变更关键字段须先停用；version 不一致返回 422")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在/11003 已停用/11005 分类非叶子/11012 计价方法不可变更/11016 单位非法/11017 分类停用/10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody MaterialUpdateDTO dto) {
        return Result.success(materialApplicationService.update(id, dto));
    }

    /**
     * 6.2.6 删除物料（base:material:delete，仅 0-新建可删除，逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('base:material:delete')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.DELETE)
    @Operation(summary = "删除物料", description = "权限点 base:material:delete；仅 0-新建可删除，逻辑删除")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在/11009 已启用不可删除/11011 已被业务单据引用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialDeleteVO> delete(@PathVariable Long id) {
        return Result.success(materialApplicationService.delete(id));
    }

    /**
     * 6.2.7 物料批量删除（base:material:delete，部分失败语义）。
     */
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('base:material:delete')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.DELETE)
    @Operation(summary = "物料批量删除", description = "权限点 base:material:delete；单次 ≤200 条，部分失败语义（成功项照常删除，失败项在响应中逐条返回原因）")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 参数错误；逐条失败原因见响应体，如 11002/11009/11011）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialBatchDeleteVO> batchDelete(@Valid @RequestBody MaterialBatchDeleteDTO dto) {
        return Result.success(materialApplicationService.batchDelete(dto));
    }

    /**
     * 6.2.8 物料审核（base:material:approve，状态机守卫）。
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('base:material:approve')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.APPROVE)
    @Operation(summary = "物料审核", description = "权限点 base:material:approve；状态机守卫：仅 0-新建可审核，通过置 1-审核、驳回答 0-新建")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在/11010 当前状态不允许审核）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialStatusVO> audit(@PathVariable Long id, @Valid @RequestBody MaterialAuditDTO dto) {
        return Result.success(materialApplicationService.audit(id, dto));
    }

    /**
     * 6.2.9 物料启用（base:material:update，状态机守卫）。
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('base:material:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "物料启用", description = "权限点 base:material:update；状态机守卫：仅 1-审核可启用，2-启用幂等返回成功")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在/11003 已停用/11010 未审核不可启用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialStatusVO> enable(@PathVariable Long id) {
        return Result.success(materialApplicationService.enable(id));
    }

    /**
     * 6.2.10 物料停用（base:material:update，状态机守卫，停用原因落 remark）。
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('base:material:update')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "物料停用", description = "权限点 base:material:update；状态机守卫：0-新建须先审核，3-停用幂等返回成功；停用原因落 remark")
    @ApiResponse(responseCode = "422", description = "业务校验失败（11002 物料不存在/11010 当前状态不允许停用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialStatusVO> disable(@PathVariable Long id, @Valid @RequestBody MaterialDisableDTO dto) {
        return Result.success(materialApplicationService.disable(id, dto));
    }

    /**
     * 6.2.11 物料导入（base:material:import，CSV 简化口径，ALL_OR_NOTHING/SKIP_ERROR）。
     */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('base:material:import')")
    @AuditLog(module = AuditModuleEnum.MATERIAL, operateType = OperateTypeEnum.CREATE)
    @Operation(summary = "物料导入", description = "权限点 base:material:import；CSV 文件（UTF-8），mode 缺省 ALL_OR_NOTHING（任一行错误整批回滚），SKIP_ERROR 跳过错误行")
    @ApiResponse(responseCode = "422", description = "业务校验失败（文件为空/CSV 解析失败/行校验失败；ALL_OR_NOTHING 模式整批回滚并汇总错误行）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MaterialImportResultVO> importMaterials(
        @Parameter(description = "物料导入文件（必填，CSV，UTF-8）") @RequestPart("file") MultipartFile file,
        @Parameter(description = "导入模式：ALL_OR_NOTHING-全部成功才提交（缺省）/SKIP_ERROR-跳过错误行")
        @RequestParam(required = false) String mode) {
        return Result.success(materialApplicationService.importMaterials(file, mode));
    }

    /**
     * 6.2.12 物料导入模板下载（base:material:import，CSV 简化口径）。
     */
    @GetMapping("/import-template")
    @PreAuthorize("hasAuthority('base:material:import')")
    @Operation(summary = "物料导入模板下载", description = "权限点 base:material:import；返回 CSV 文件流（UTF-8 BOM），文件名在 Content-Disposition 头")
    @ApiResponse(responseCode = "200", description = "CSV 文件流",
        content = @Content(mediaType = "text/csv"))
    public ResponseEntity<byte[]> importTemplate() {
        return csvResponse(materialApplicationService.downloadImportTemplate(), "物料导入模板.csv");
    }

    /**
     * 6.2.13 物料列表导出（base:material:export，CSV 文件流）。
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('base:material:export')")
    @Operation(summary = "物料列表导出", description = "权限点 base:material:export；按与分页查询一致的筛选条件导出，返回 CSV 文件流（UTF-8 BOM），文件名在 Content-Disposition 头")
    @ApiResponse(responseCode = "200", description = "CSV 文件流",
        content = @Content(mediaType = "text/csv"))
    public ResponseEntity<byte[]> export(@ParameterObject @Valid MaterialQueryDTO query) {
        return csvResponse(materialApplicationService.exportCsv(query),
            "物料主数据_" + FILENAME_FORMATTER.format(LocalDateTime.now()) + ".csv");
    }

    /**
     * CSV 文件流响应（UTF-8 BOM，Content-Disposition 文件名 UTF-8 编码）。
     */
    private static ResponseEntity<byte[]> csvResponse(byte[] csv, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }
}
