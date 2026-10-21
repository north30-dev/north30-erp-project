package me.north30.erp.system.attachment.vo;

import java.time.LocalDateTime;

/**
 * 附件 VO（接口文档 5.9.1 上传 / 5.9.3 列表）。
 * <p>filePath 为相对存储根目录路径；downloadUrl 为列表接口派生字段。</p>
 * 
 * @param id 附件 ID
 * @param fileName 文件名（中文）
 * @param filePath 文件路径（相对存储根目录）
 * @param fileType 文件类型（MIME 类型）
 * @param fileSize 文件大小（字节）
 * @param bizType 业务类型（如：客户、订单等）
 * @param bizId 业务 ID（如：客户 ID、订单 ID 等）
 * @param bizCode 业务编码（如：客户编码、订单编码等）
 * @param uploadBy 上传人（如：张三）
 * @param uploadTime 上传时间
 * @param downloadUrl 下载地址（如：/api/system/attachments/123456/download）
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
