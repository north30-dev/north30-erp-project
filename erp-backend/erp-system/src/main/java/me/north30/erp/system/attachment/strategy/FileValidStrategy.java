package me.north30.erp.system.attachment.strategy;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.mapper.SysAttachmentMapper;
import me.north30.erp.system.common.enums.SystemErrorCode;

/**
 * 附件校验策略：
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidStrategy {

    private final SysAttachmentMapper sysAttachmentMapper;

    /**
     * 校验附件是否存在
     * @param id 附件 ID
     * @return 附件实体
     */
    public SysAttachment requireAttachment(Long id){
        SysAttachment attachment = sysAttachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException(SystemErrorCode.ATTACHMENT_NOT_FOUND, "附件不存在");
        }
        return attachment;
    }

    /**
     * 校验上传文件是否为空
     * @param file 上传文件
     */
    public void requireFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
    }

}
