package me.north30.erp.system.attachment.strategy;

import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 附件文件存储策略：文件类型判定、文件名安全处理、相对路径生成、写盘与物理路径解析。
 * <p>本地目录存储（storage_type=1），文件名与相对路径均由服务端生成，防路径穿越。
 * 仅做文件系统侧纯操作，不触碰数据库与事务。</p>
 */
@Slf4j
@Component
public class FileStorageStrategy {

    /** 单文件大小上限 20MB（接口文档 5.9.1） */
    public static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

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

    /** 附件存储根目录（绝对路径，server 端控制，不含用户可控路径） */
    private final Path storeRoot;

    public FileStorageStrategy(@Value("${erp.attachment.store-path:/tmp/erp-attachments}") String storePath) {
        this.storeRoot = Paths.get(storePath).toAbsolutePath().normalize();
    }

    /**
     * 按扩展名判定文件类型（PDF/JPG/PNG/XLSX/DOCX），不支持则抛 10401。
     */
    public String resolveFileType(String originalFilename) {
        String ext = extensionOf(originalFilename);
        String fileType = SUPPORTED_TYPES.get(ext);
        if (fileType == null) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_TYPE_NOT_SUPPORTED);
        }
        return fileType;
    }

    /**
     * 文件名安全处理：剥离路径分隔符与控制字符，防止路径穿越，超长截断。
     */
    public String sanitizeFileName(String originalFilename) {
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
    public String buildRelativePath(String fileType) {
        String monthDir = LocalDate.now().format(MONTH_DIR_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return monthDir + "/" + uuid + "." + fileType.toLowerCase();
    }

    /**
     * 写盘：先建目录再写入；写盘失败转为系统错误（不吞异常）。
     */
    public void writeFile(String relativePath, MultipartFile file) {
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
    public Path resolveStoredPath(String relativePath) {
        Path target = storeRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storeRoot)) {
            throw new BusinessException(SystemManageErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return target;
    }

    private String extensionOf(String originalFilename) {
        String safeName = sanitizeFileName(originalFilename);
        int dot = safeName.lastIndexOf('.');
        return dot < 0 ? "" : safeName.substring(dot + 1).toLowerCase();
    }
}
