package me.north30.erp.system.core.controller;

import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.service.SysAttachmentService;
import me.north30.erp.system.core.vo.AttachmentDeleteVO;
import me.north30.erp.system.core.vo.AttachmentDownloadVO;
import me.north30.erp.system.core.vo.AttachmentVO;
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
@RestController
@RequestMapping("/api/system/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final SysAttachmentService attachmentService;

    /**
     * 5.9.1 附件上传（权限：system:attachment:upload）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:attachment:upload')")
    public Result<AttachmentVO> upload(@RequestPart("file") MultipartFile file,
                                       @RequestParam("bizType") String bizType,
                                       @RequestParam("bizId") Long bizId,
                                       @RequestParam(value = "bizCode", required = false) String bizCode) {
        return Result.success(attachmentService.upload(file, bizType, bizId, bizCode));
    }

    /**
     * 5.9.3 附件列表查询（权限：system:attachment:list），按业务类型 + 业务主键，不分页。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:attachment:list')")
    public Result<List<AttachmentVO>> list(@RequestParam("bizType") String bizType,
                                           @RequestParam("bizId") Long bizId) {
        return Result.success(attachmentService.listByBiz(bizType, bizId));
    }

    /**
     * 5.9.2 附件下载（权限：system:attachment:download）：文件流 + RFC 5987 中文文件名。
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('system:attachment:download')")
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
    public Result<AttachmentDeleteVO> delete(@PathVariable Long id) {
        return Result.success(attachmentService.delete(id));
    }
}
