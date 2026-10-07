package me.north30.erp.system.attachment.vo;

/**
 * 附件删除结果 VO（接口文档 5.9.4）。
 * 
 * @param id 附件 ID
 * @param isDeleted 是否删除成功（0：失败，1：成功）
 */
public record AttachmentDeleteVO(
    Long id,
    Integer isDeleted
) {
}
