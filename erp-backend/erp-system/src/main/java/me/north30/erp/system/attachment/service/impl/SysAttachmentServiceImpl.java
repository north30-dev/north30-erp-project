package me.north30.erp.system.attachment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.attachment.converter.AttachmentConverter;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.mapper.SysAttachmentMapper;
import me.north30.erp.system.attachment.service.SysAttachmentService;
import me.north30.erp.system.attachment.strategy.FileStorageStrategy;
import me.north30.erp.system.attachment.vo.AttachmentDownloadVO;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.security.SecurityUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(MultipartFile file, String bizType, Long bizId, String bizCode) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (!StringUtils.hasText(bizType) || bizId == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "业务类型与业务主键不能为空");
        }
        String fileType = fileStorageStrategy.resolveFileType(file.getOriginalFilename());
        if (file.getSize() > FileStorageStrategy.MAX_FILE_SIZE) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_SIZE_EXCEEDED);
        }
        Long count = sysAttachmentMapper.selectCount(new LambdaQueryWrapper<SysAttachment>()
            .eq(SysAttachment::getBizType, bizType.trim())
            .eq(SysAttachment::getBizId, bizId));
        if (count != null && count >= MAX_COUNT_PER_BIZ) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_COUNT_EXCEEDED);
        }
        String fileName = fileStorageStrategy.sanitizeFileName(file.getOriginalFilename());
        String relativePath = fileStorageStrategy.buildRelativePath(fileType);
        fileStorageStrategy.writeFile(relativePath, file);

        SysAttachment attachment = new SysAttachment();
        attachment.setBizType(bizType.trim());
        attachment.setBizId(bizId);
        attachment.setBizCode(bizCode);
        attachment.setFileName(fileName);
        attachment.setFilePath(relativePath);
        attachment.setFileType(fileType);
        attachment.setFileSize(file.getSize());
        attachment.setStorageType(1);
        LoginUser user = SecurityUtils.getCurrentUser();
        attachment.setUploadBy(user != null ? user.username() : "system");
        attachment.setUploadTime(LocalDateTime.now());
        sysAttachmentMapper.insert(attachment);
        return attachmentConverter.toVO(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentVO> listByBiz(String bizType, Long bizId) {
        if (!StringUtils.hasText(bizType) || bizId == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "业务类型与业务主键不能为空");
        }
        List<SysAttachment> list = sysAttachmentMapper.selectList(new LambdaQueryWrapper<SysAttachment>()
            .eq(SysAttachment::getBizType, bizType.trim())
            .eq(SysAttachment::getBizId, bizId)
            .orderByDesc(SysAttachment::getId));
        return list.stream().map(attachmentConverter::toVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadVO download(Long id) {
        SysAttachment attachment = requireAttachment(id);
        Path target = fileStorageStrategy.resolveStoredPath(attachment.getFilePath());
        if (!Files.exists(target)) {
            log.warn("附件物理文件缺失：id={}, path={}", id, attachment.getFilePath());
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return new AttachmentDownloadVO(attachment.getFileName(), new FileSystemResource(target));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysAttachment attachment = requireAttachment(id);
        LoginUser user = SecurityUtils.requireCurrentUser();
        // 横向数据守卫：仅上传人可删除（接口文档 5.9.4 错误码 10202）
        if (!user.username().equals(attachment.getUploadBy())) {
            throw new BusinessException(SystemManageErrorCode.DATA_SCOPE_DENIED, "仅上传人可删除该附件");
        }
        sysAttachmentMapper.deleteById(id);
        return new DeleteResultVO(id, 1);
    }

    @Override
    public SysAttachment requireAttachment(Long id) {
        SysAttachment attachment = sysAttachmentMapper.selectById(id);
        if (attachment == null || Integer.valueOf(1).equals(attachment.getIsDeleted())) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }
}
