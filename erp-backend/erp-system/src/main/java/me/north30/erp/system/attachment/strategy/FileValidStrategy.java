package me.north30.erp.system.attachment.strategy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.attachment.common.AttachmentConfigKeys;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.mapper.SysAttachmentMapper;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.config.service.SysConfigDomainService;

/**
 * 附件校验策略：上传文件非空/大小上限、单据附件数量上限、附件存在性与物理文件存在性守卫。
 * <p>大小与数量上限消费 sys_config 运行时参数，经 config 域只读准入接口
 * {@link SysConfigDomainService} 获取（跨域走 Service 不直插 Mapper，key 随消费域见
 * {@link AttachmentConfigKeys}），缺失/停用/非法时静默回落兜底值。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidStrategy {

    private final SysAttachmentMapper sysAttachmentMapper;
    private final SysConfigDomainService sysConfigDomainService;

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

    /**
     * 校验上传文件大小不超过配置上限（sys_config：attachment.max.file.size.mb，兜底 20MB）。
     *
     * @param file 上传文件
     */
    public void validateFileSize(MultipartFile file) {
        long maxFileSizeMb = sysConfigDomainService.longValue(AttachmentConfigKeys.MAX_FILE_SIZE_MB,
            AttachmentConfigKeys.DEFAULT_MAX_FILE_SIZE_MB);
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new BusinessException(CommonErrorCode.ATTACHMENT_SIZE_EXCEEDED,
                "上传文件大小不能超过 " + maxFileSizeMb + "MB");
        }
    }

    /**
     * 校验单据当前有效附件数未达配置上限（sys_config：attachment.max.count.per.biz，兜底 20 个；
     * 逻辑删除记录不计入）。单据由 bizType + bizId 圈定。
     *
     * @param bizType 业务单据类型
     * @param bizId 业务单据 ID
     */
    public void validateAttachmentCount(String bizType, Long bizId) {
        long maxCountPerBiz = sysConfigDomainService.longValue(AttachmentConfigKeys.MAX_COUNT_PER_BIZ,
            AttachmentConfigKeys.DEFAULT_MAX_COUNT_PER_BIZ);
        Long count = sysAttachmentMapper.selectCount(new LambdaQueryWrapper<SysAttachment>()
            .eq(SysAttachment::getBizType, bizType)
            .eq(SysAttachment::getBizId, bizId));
        if (count != null && count >= maxCountPerBiz) {
            throw new BusinessException(CommonErrorCode.ATTACHMENT_COUNT_EXCEEDED,
                "附件数量超出上限（单据最多 " + maxCountPerBiz + " 个附件）");
        }
    }

    /**
     * 校验附件物理文件是否存在
     * @param attachmentPath 附件物理文件路径
     * @param id 附件 ID
     */
    public void requireAttachmentFileExist(Path attachmentPath, Long id) {
        if (!Files.exists(attachmentPath)) {
            log.warn("附件物理文件缺失：id={}, path={}", id, attachmentPath);
            throw new BusinessException(SystemErrorCode.ATTACHMENT_NOT_FOUND);
        }
    }

}
