package me.north30.erp.system.attachment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 附件上传业务参数。
 *
 * @param bizType 业务类型（必填，附件所属业务类型，服务端按其 + 业务主键统计附件数量）
 * @param bizId   业务主键（必填，附件所属业务记录 ID）
 * @param bizCode 单据号（可选，附件所属业务单据编码）
 */
public record AttachmentBizQueryDTO(

    @NotBlank(message = "业务类型不能为空") 
    String bizType,

    @NotNull(message = "业务主键不能为空") 
    Long bizId, 
    
    @Size(max = 64, message = "单据号长度不能超过 64") 
    String bizCode) {
}
