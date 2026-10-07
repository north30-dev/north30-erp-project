package me.north30.erp.system.attachment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.attachment.service.SysAttachmentService;
import me.north30.erp.system.attachment.vo.AttachmentDeleteVO;
import me.north30.erp.system.attachment.vo.AttachmentDownloadVO;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 附件接口（/api/system/attachments，接口文档 5.9.1-5.9.4）。
 * <p>下载经 Content-Disposition RFC 5987 输出中文文件名；@PreAuthorize 生效需主配置开启 @EnableMethodSecurity。</p>
 */
@Tag(name = "附件管理", description = "/api/system/attachments，接口文档 5.9.1-5.9.4")
@RestController
@RequestMapping("/api/system/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 10401, "message": "文件类型不支持", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysAttachmentService attachmentService;

    /**
     * 5.9.1 附件上传（权限：system:attachment:upload）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:attachment:upload')")
    @Operation(summary = "附件上传", description = "权限点 system:attachment:upload；单文件不超过 20MB，单单据不超过 20 个，支持 pdf/jpg/jpeg/png/xlsx/docx")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 参数错误/10401 文件类型不支持/10402 文件大小超出上限/10403 附件数量超出上限/10999 附件保存失败）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<AttachmentVO> upload(@Parameter(description = "附件文件（必填，支持 pdf/jpg/jpeg/png/xlsx/docx，不超过 20MB）")
                                       @RequestPart("file") MultipartFile file,
                                       @Parameter(description = "业务类型（必填，附件所属业务类型，服务端按其 + 业务主键统计附件数量）")
                                       @RequestParam("bizType") String bizType,
                                       @Parameter(description = "业务主键（必填，附件所属业务记录 ID）")
                                       @RequestParam("bizId") Long bizId,
                                       @Parameter(description = "单据号（可选，附件所属业务单据编码）")
                                       @RequestParam(value = "bizCode", required = false) String bizCode) {
        return Result.success(attachmentService.upload(file, bizType, bizId, bizCode));
    }

    /**
     * 5.9.3 附件列表查询（权限：system:attachment:list），按业务类型 + 业务主键，不分页。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:attachment:list')")
    @Operation(summary = "附件列表查询", description = "权限点 system:attachment:list；按业务类型 + 业务主键查询，不分页，按 ID 倒序")
    public Result<List<AttachmentVO>> list(@Parameter(description = "业务类型（必填）")
                                           @RequestParam("bizType") String bizType,
                                           @Parameter(description = "业务主键（必填）")
                                           @RequestParam("bizId") Long bizId) {
        return Result.success(attachmentService.listByBiz(bizType, bizId));
    }

    /**
     * 5.9.2 附件下载（权限：system:attachment:download）：文件流 + RFC 5987 中文文件名。
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('system:attachment:download')")
    @Operation(summary = "附件下载", description = "权限点 system:attachment:download；返回文件流，文件名经 Content-Disposition RFC 5987 输出中文名")
    @ApiResponse(responseCode = "200", description = "文件流（文件名在 Content-Disposition 头）",
        content = @Content(mediaType = "application/octet-stream"))
    @ApiResponse(responseCode = "422", description = "业务校验失败（18038 附件不存在，含物理文件缺失）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        AttachmentDownloadVO download = attachmentService.download(id);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(download.fileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(download.file());
    }

    /**
     * 5.9.4 附件删除（权限：system:attachment:delete）：逻辑删除。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:attachment:delete')")
    @Operation(summary = "附件删除", description = "权限点 system:attachment:delete；逻辑删除，仅上传人可删除")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18038 附件不存在/10202 无权操作该数据，仅上传人可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<AttachmentDeleteVO> delete(@PathVariable Long id) {
        return Result.success(attachmentService.delete(id));
    }
}
