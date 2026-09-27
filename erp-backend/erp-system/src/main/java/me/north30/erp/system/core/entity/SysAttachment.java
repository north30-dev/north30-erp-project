package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 附件表（sys_attachment：SYS-10，PDF/JPG/PNG/XLSX/DOCX，单文件 ≤20MB、单单据 ≤20 个）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_attachment")
public class SysAttachment extends BaseEntity {

    /** 关联业务类型 PURCHASE_RECEIPT/SALES_RETURN/BOM 等 */
    private String bizType;

    /** 关联业务主键 */
    private Long bizId;

    /** 关联单据号 */
    private String bizCode;

    /** 原始文件名（服务器端安全处理，防路径穿越） */
    private String fileName;

    /** 存储路径（相对存储根目录，不含用户可控路径） */
    private String filePath;

    /** 文件类型 PDF/JPG/PNG/XLSX/DOCX */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 存储类型 1-本地目录 */
    private Integer storageType;

    /** 上传人 */
    private String uploadBy;

    /** 上传时间 */
    private LocalDateTime uploadTime;
}
