package me.north30.erp.system.attachment.service.impl;

import me.north30.erp.common.exception.CommonErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.attachment.converter.AttachmentConverter;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.mapper.SysAttachmentMapper;
import me.north30.erp.system.attachment.service.SysAttachmentService;
import me.north30.erp.system.attachment.strategy.FileStorageStrategy;
import me.north30.erp.system.attachment.strategy.FileValidStrategy;
import me.north30.erp.system.attachment.dto.AttachmentBizQueryDTO;
import me.north30.erp.system.attachment.vo.AttachmentDownloadVO;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.security.SecurityUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 附件服务实现：本地目录存储（storage_type=1），文件系统操作委托 {@link FileStorageStrategy}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAttachmentServiceImpl implements SysAttachmentService {

    /** 单单据附件数量上限 20 个（接口文档 5.9.1） */
    private static final long MAX_COUNT_PER_BIZ = 20;

    private final SysAttachmentMapper sysAttachmentMapper;
    private final AttachmentConverter attachmentConverter;
    private final FileStorageStrategy fileStorageStrategy;
    private final FileValidStrategy fileValidStrategy;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(MultipartFile file, AttachmentBizQueryDTO dto) {
        fileValidStrategy.requireFileNotEmpty(file);
        String bizType = dto.bizTypeNormalized();
        String fileType = fileStorageStrategy.resolveFileType(file.getOriginalFilename());
        if (file.getSize() > FileStorageStrategy.MAX_FILE_SIZE) {
            throw new BusinessException(CommonErrorCode.ATTACHMENT_SIZE_EXCEEDED, "上传文件大小不能超过 20MB");
        }
        Long count = sysAttachmentMapper.selectCount(new LambdaQueryWrapper<SysAttachment>()
                .eq(SysAttachment::getBizType, bizType)
                .eq(SysAttachment::getBizId, dto.bizId()));
        if (count != null && count >= MAX_COUNT_PER_BIZ) {
            throw new BusinessException(CommonErrorCode.ATTACHMENT_COUNT_EXCEEDED);
        }
        String fileName = fileStorageStrategy.sanitizeFileName(file.getOriginalFilename());
        String relativePath = fileStorageStrategy.buildRelativePath(fileType);
        fileStorageStrategy.writeFile(relativePath, file);

        LoginUser user = SecurityUtils.getCurrentUser();
        SysAttachment attachment = attachmentConverter.createSysAttachment(dto, bizType, file,
            fileName, relativePath, fileType, user != null ? user.username() : "system");
        sysAttachmentMapper.insert(attachment);
        return attachmentConverter.toAttachmentVO(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentVO> listByBiz(AttachmentBizQueryDTO dto) {
        List<SysAttachment> list = sysAttachmentMapper.selectList(new LambdaQueryWrapper<SysAttachment>()
                .eq(SysAttachment::getBizType, dto.bizType().trim())
                .eq(SysAttachment::getBizId, dto.bizId())
                .orderByDesc(SysAttachment::getId));
        return list.stream().map(attachmentConverter::toAttachmentVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadVO download(Long id) {
        SysAttachment attachment = fileValidStrategy.requireAttachment(id);
        Path target = fileStorageStrategy.resolveStoredPath(attachment.getFilePath());
        if (!Files.exists(target)) {
            log.warn("附件物理文件缺失：id={}, path={}", id, attachment.getFilePath());
            throw new BusinessException(SystemErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return new AttachmentDownloadVO(attachment.getFileName(), new FileSystemResource(target));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysAttachment attachment = fileValidStrategy.requireAttachment(id);
        LoginUser user = SecurityUtils.requireCurrentUser();
        // 横向数据守卫：仅上传人可删除（接口文档 5.9.4 错误码 10202）
        attachment.validateUploadBy(user);
        sysAttachmentMapper.deleteById(id);
        return new DeleteResultVO(id, 1);
    }

}
