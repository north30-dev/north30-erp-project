package me.north30.erp.system.attachment;

import me.north30.erp.system.attachment.entity.SysAttachment;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;

/**
 * 附件域测试数据静态工厂：集中构造实体与 MultipartFile mock，避免测试方法内堆砌字段。
 * <p>MultipartFile 全部为 Mockito mock，不携带任何真实文件内容；
 * 各工厂方法仅打桩当前用例会触达的方法，避免严格桩（strict stubs）误报。</p>
 */
public final class AttachmentTestFactory {

    private AttachmentTestFactory() {
    }

    /**
     * 构建附件实体（上传时间固定，逻辑删除标记由用例指定）。
     */
    public static SysAttachment attachment(Long id, String bizType, Long bizId, String bizCode, String fileName,
                                           String filePath, String fileType, Long fileSize, String uploadBy,
                                           Integer isDeleted) {
        SysAttachment attachment = new SysAttachment();
        attachment.setId(id);
        attachment.setBizType(bizType);
        attachment.setBizId(bizId);
        attachment.setBizCode(bizCode);
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath);
        attachment.setFileType(fileType);
        attachment.setFileSize(fileSize);
        attachment.setStorageType(1);
        attachment.setUploadBy(uploadBy);
        attachment.setUploadTime(LocalDateTime.of(2026, 9, 28, 9, 30, 0));
        attachment.setIsDeleted(isDeleted);
        return attachment;
    }

    /**
     * 构建非空文件 mock（仅打桩文件名与非空标记，用于不校验大小的异常分支）。
     */
    public static MultipartFile fileNamed(String originalFilename) {
        // lenient：部分调用方在更早的参数校验分支即抛出，不会消费全部桩
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.lenient().when(file.isEmpty()).thenReturn(false);
        Mockito.lenient().when(file.getOriginalFilename()).thenReturn(originalFilename);
        return file;
    }

    /**
     * 构建非空文件 mock（含文件大小，用于上传正常路径）。
     */
    public static MultipartFile file(String originalFilename, long size) {
        MultipartFile file = fileNamed(originalFilename);
        given(file.getSize()).willReturn(size);
        return file;
    }

    /**
     * 构建空文件 mock（仅打桩空标记，用于空文件校验分支）。
     */
    public static MultipartFile emptyFile() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(true);
        return file;
    }
}
