package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.core.entity.SysAttachment;
import me.north30.erp.system.core.enums.SystemManageErrorCode;
import me.north30.erp.system.core.mapper.SysAttachmentMapper;
import me.north30.erp.system.core.security.LoginUser;
import me.north30.erp.system.core.security.SecurityUtils;
import me.north30.erp.system.core.service.ISysAttachmentService;
import me.north30.erp.system.core.vo.AttachmentDeleteVO;
import me.north30.erp.system.core.vo.AttachmentDownloadVO;
import me.north30.erp.system.core.vo.AttachmentVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 附件服务实现：本地目录存储（storage_type=1），文件名与相对路径服务端生成，防路径穿越。
 */
@Slf4j
@Service
public class SysAttachmentServiceImpl implements ISysAttachmentService {

    /** 单文件大小上限 20MB（接口文档 5.9.1） */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    /** 单单据附件数量上限 20 个（接口文档 5.9.1） */
    private static final long MAX_COUNT_PER_BIZ = 20;

    /** 文件名长度上限（file_name VARCHAR(200)） */
    private static final int MAX_FILE_NAME_LENGTH = 200;

    /** 支持的扩展名 → file_type 映射（接口文档 5.9.1） */
    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
        "pdf", "PDF",
        "jpg", "JPG",
        "jpeg", "JPG",
        "png", "PNG",
        "xlsx", "XLSX",
        "docx", "DOCX");

    private static final DateTimeFormatter MONTH_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private static final String DOWNLOAD_URL_TEMPLATE = "/api/system/attachments/%d/download";

    private final SysAttachmentMapper sysAttachmentMapper;

    /** 附件存储根目录（绝对路径，server 端控制，不含用户可控路径） */
    private final Path storeRoot;

    public SysAttachmentServiceImpl(SysAttachmentMapper sysAttachmentMapper,
                                    @Value("${erp.attachment.store-path:/tmp/erp-attachments}") String storePath) {
        this.sysAttachmentMapper = sysAttachmentMapper;
        this.storeRoot = Paths.get(storePath).toAbsolutePath().normalize();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentVO upload(MultipartFile file, String bizType, Long bizId, String bizCode) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (!StringUtils.hasText(bizType) || bizId == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "业务类型与业务主键不能为空");
        }
        String fileType = resolveFileType(file.getOriginalFilename());
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_SIZE_EXCEEDED);
        }
        Long count = sysAttachmentMapper.selectCount(new LambdaQueryWrapper<SysAttachment>()
            .eq(SysAttachment::getBizType, bizType.trim())
            .eq(SysAttachment::getBizId, bizId));
        if (count != null && count >= MAX_COUNT_PER_BIZ) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_COUNT_EXCEEDED);
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String relativePath = buildRelativePath(fileType);
        writeFile(relativePath, file);

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
        return toVO(attachment);
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
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadVO download(Long id) {
        SysAttachment attachment = requireAttachment(id);
        Path target = resolveStoredPath(attachment.getFilePath());
        if (!Files.exists(target)) {
            log.warn("附件物理文件缺失：id={}, path={}", id, attachment.getFilePath());
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return new AttachmentDownloadVO(attachment.getFileName(), new FileSystemResource(target));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentDeleteVO delete(Long id) {
        SysAttachment attachment = requireAttachment(id);
        LoginUser user = SecurityUtils.requireCurrentUser();
        // 横向数据守卫：仅上传人可删除（接口文档 5.9.4 错误码 10202）
        if (!user.username().equals(attachment.getUploadBy())) {
            throw new BusinessException(SystemManageErrorCode.DATA_SCOPE_DENIED, "仅上传人可删除该附件");
        }
        sysAttachmentMapper.deleteById(id);
        return new AttachmentDeleteVO(id, 1);
    }

    private SysAttachment requireAttachment(Long id) {
        SysAttachment attachment = sysAttachmentMapper.selectById(id);
        if (attachment == null || Integer.valueOf(1).equals(attachment.getIsDeleted())) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }

    /**
     * 按扩展名判定文件类型（PDF/JPG/PNG/XLSX/DOCX），不支持则抛 10401。
     */
    private String resolveFileType(String originalFilename) {
        String ext = extensionOf(originalFilename);
        String fileType = SUPPORTED_TYPES.get(ext);
        if (fileType == null) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_TYPE_NOT_SUPPORTED);
        }
        return fileType;
    }

    private String extensionOf(String originalFilename) {
        String safeName = sanitizeFileName(originalFilename);
        int dot = safeName.lastIndexOf('.');
        return dot < 0 ? "" : safeName.substring(dot + 1).toLowerCase();
    }

    /**
     * 文件名安全处理：剥离路径分隔符与控制字符，防止路径穿越，超长截断。
     */
    private String sanitizeFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "unnamed";
        }
        String name = originalFilename.replace('\\', '/').trim();
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\x00-\\x1f]", "").trim();
        if (!StringUtils.hasText(name)) {
            return "unnamed";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(0, MAX_FILE_NAME_LENGTH) : name;
    }

    /**
     * 存储相对路径：yyyyMM/{uuid}.{ext}，目录与文件名均为服务端生成。
     */
    private String buildRelativePath(String fileType) {
        String monthDir = LocalDate.now().format(MONTH_DIR_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return monthDir + "/" + uuid + "." + fileType.toLowerCase();
    }

    /**
     * 写盘：先建目录再写入；写盘失败转为系统错误（不吞异常）。
     */
    private void writeFile(String relativePath, MultipartFile file) {
        Path target = storeRoot.resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("附件写盘失败：path={}, 原因：{}", target, e.getMessage());
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "附件保存失败，请稍后重试");
        }
    }

    /**
     * 相对路径 → 物理路径；路径逃逸（越出存储根目录）视为附件不存在。
     */
    private Path resolveStoredPath(String relativePath) {
        Path target = storeRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storeRoot)) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return target;
    }

    private AttachmentVO toVO(SysAttachment attachment) {
        return new AttachmentVO(attachment.getId(), attachment.getFileName(), attachment.getFilePath(),
            attachment.getFileType(), attachment.getFileSize(), attachment.getBizType(), attachment.getBizId(),
            attachment.getBizCode(), attachment.getUploadBy(), attachment.getUploadTime(),
            String.format(DOWNLOAD_URL_TEMPLATE, attachment.getId()));
    }
}
