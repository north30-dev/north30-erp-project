package me.north30.erp.system.attachment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.system.attachment.converter.AttachmentConverter;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.mapper.SysAttachmentMapper;
import me.north30.erp.system.attachment.service.SysAttachmentService;
import me.north30.erp.system.attachment.strategy.FileStorageStrategy;
import me.north30.erp.system.attachment.strategy.FileValidStrategy;
import me.north30.erp.system.attachment.dto.AttachmentBizQueryDTO;
import me.north30.erp.system.attachment.vo.AttachmentDownloadVO;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.security.SecurityUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * 附件服务实现：本地目录存储（storage_type=1），文件系统操作委托 {@link FileStorageStrategy}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAttachmentServiceImpl implements SysAttachmentService {

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
        // 大小/数量上限走 sys_config 运行时参数，校验逻辑收敛在 FileValidStrategy（兜底 20MB/20 个）
        fileValidStrategy.requireFileSizeWithinLimit(file);
        fileValidStrategy.requireAttachmentCountWithinLimit(bizType, dto.bizId());
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
        fileValidStrategy.requireAttachmentFileExist(target, id);
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
