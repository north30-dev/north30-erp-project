package me.north30.erp.system.core.vo;

import java.time.LocalDateTime;

/**
 * 附件 VO（接口文档 5.9.1 上传 / 5.9.3 列表）。
 * <p>filePath 为相对存储根目录路径；downloadUrl 为列表接口派生字段。</p>
 */
public record AttachmentVO(
    Long id,
    String fileName,
    String filePath,
    String fileType,
    Long fileSize,
    String bizType,
    Long bizId,
    String bizCode,
    String uploadBy,
    LocalDateTime uploadTime,
    String downloadUrl
) {
}
