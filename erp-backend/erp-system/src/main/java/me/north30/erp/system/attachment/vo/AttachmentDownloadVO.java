package me.north30.erp.system.attachment.vo;

import org.springframework.core.io.Resource;

/**
 * 附件下载结果 VO：文件名用于 Content-Disposition（RFC 5987 中文文件名），file 为流式资源。
 */
public record AttachmentDownloadVO(
    String fileName,
    Resource file
) {
}
