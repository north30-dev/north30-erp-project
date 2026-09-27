package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.entity.SysAttachment;
import me.north30.erp.system.core.mapper.SysAttachmentMapper;
import me.north30.erp.system.core.security.LoginUser;
import me.north30.erp.system.core.vo.AttachmentDeleteVO;
import me.north30.erp.system.core.vo.AttachmentDownloadVO;
import me.north30.erp.system.core.vo.AttachmentVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysAttachmentServiceImpl 单元测试：上传类型/大小/数量校验、写盘、
 * 下载存在性与逻辑删除守卫、删除创建人校验。
 */
@ExtendWith(MockitoExtension.class)
class SysAttachmentServiceImplTest {

    private static final LoginUser ALICE = new LoginUser(1L, "alice", "jti", "refresh-jti");

    @Mock
    private SysAttachmentMapper sysAttachmentMapper;

    @TempDir
    Path tempDir;

    private SysAttachmentServiceImpl attachmentService;

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper 解析 SysAttachment::getXxx 需要实体元数据缓存（无 Spring 容器时手工初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, SysAttachment.class);
    }

    @BeforeEach
    void setUp() {
        attachmentService = new SysAttachmentServiceImpl(sysAttachmentMapper, tempDir.toString());
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(ALICE, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("上传：非 PDF/JPG/PNG/XLSX/DOCX 类型被拒（10401）")
    void uploadRejectsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile("file", "脚本.exe", "application/octet-stream", new byte[]{1});

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.upload(file, "PURCHASE_RECEIPT", 100L, null));
        assertEquals(10401, exception.getCode());
    }

    @Test
    @DisplayName("上传：文件超过 20MB 被拒（10402）")
    void uploadRejectsOversizeFile() {
        byte[] oversized = new byte[20 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "大文件.pdf", "application/pdf", oversized);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.upload(file, "PURCHASE_RECEIPT", 100L, null));
        assertEquals(10402, exception.getCode());
    }

    @Test
    @DisplayName("上传：单单据附件超过 20 个被拒（10403）")
    void uploadRejectsExceededCount() {
        MockMultipartFile file = new MockMultipartFile("file", "回单.pdf", "application/pdf", new byte[]{1});
        when(sysAttachmentMapper.selectCount(any())).thenReturn(20L);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.upload(file, "PURCHASE_RECEIPT", 100L, null));
        assertEquals(10403, exception.getCode());
    }

    @Test
    @DisplayName("上传：空文件被拒（10001）")
    void uploadRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "空.pdf", "application/pdf", new byte[0]);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.upload(file, "PURCHASE_RECEIPT", 100L, null));
        assertEquals(10001, exception.getCode());
    }

    @Test
    @DisplayName("上传：文件名安全处理、写盘与落库成功")
    void uploadWritesFileAndRecord() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "../../回单 副本.pdf",
            "application/pdf", new byte[]{1, 2, 3});
        when(sysAttachmentMapper.selectCount(any())).thenReturn(0L);
        when(sysAttachmentMapper.insert(any(SysAttachment.class))).thenReturn(1);

        AttachmentVO vo = attachmentService.upload(file, "PURCHASE_RECEIPT", 100L, "GR2026090001");

        ArgumentCaptor<SysAttachment> captor = ArgumentCaptor.forClass(SysAttachment.class);
        verify(sysAttachmentMapper).insert(captor.capture());
        SysAttachment saved = captor.getValue();
        // 文件名剥离路径分隔符，保留原始名称与扩展名
        assertEquals("回单 副本.pdf", saved.getFileName());
        assertEquals("PDF", saved.getFileType());
        assertEquals(3L, saved.getFileSize());
        assertEquals("PURCHASE_RECEIPT", saved.getBizType());
        assertEquals(100L, saved.getBizId());
        assertEquals("GR2026090001", saved.getBizCode());
        assertEquals(1, saved.getStorageType());
        assertEquals("alice", saved.getUploadBy());
        // 相对路径服务端生成（yyyyMM/uuid.ext），物理文件真实落盘
        assertTrue(saved.getFilePath().matches("\\d{6}/[a-f0-9]{32}\\.pdf"));
        assertTrue(Files.exists(tempDir.resolve(saved.getFilePath())));
        assertEquals(vo.filePath(), saved.getFilePath());
        assertTrue(vo.downloadUrl().endsWith("/api/system/attachments/" + saved.getId() + "/download"));
    }

    @Test
    @DisplayName("下载：附件不存在被拒（18038）")
    void downloadRejectsMissingAttachment() {
        when(sysAttachmentMapper.selectById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.download(404L));
        assertEquals(18038, exception.getCode());
    }

    @Test
    @DisplayName("下载：已逻辑删除的附件被拒（18038）")
    void downloadRejectsDeletedAttachment() {
        when(sysAttachmentMapper.selectById(1L)).thenReturn(deletedAttachment());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.download(1L));
        assertEquals(18038, exception.getCode());
    }

    @Test
    @DisplayName("下载：正常返回文件流与原始文件名")
    void downloadReturnsFileContent() throws Exception {
        Path stored = tempDir.resolve("202609").resolve("abc123.pdf");
        Files.createDirectories(stored.getParent());
        Files.write(stored, new byte[]{9, 8, 7});
        SysAttachment attachment = new SysAttachment();
        attachment.setId(2L);
        attachment.setFileName("收货回单.pdf");
        attachment.setFilePath("202609/abc123.pdf");
        when(sysAttachmentMapper.selectById(2L)).thenReturn(attachment);

        AttachmentDownloadVO download = attachmentService.download(2L);

        assertEquals("收货回单.pdf", download.fileName());
        assertArrayEquals(new byte[]{9, 8, 7}, download.file().getContentAsByteArray());
    }

    @Test
    @DisplayName("下载：存储路径越出根目录视为附件不存在（18038，防路径穿越）")
    void downloadRejectsEscapedPath() {
        SysAttachment attachment = new SysAttachment();
        attachment.setId(3L);
        attachment.setFileName("evil.pdf");
        attachment.setFilePath("../evil.pdf");
        when(sysAttachmentMapper.selectById(3L)).thenReturn(attachment);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.download(3L));
        assertEquals(18038, exception.getCode());
    }

    @Test
    @DisplayName("删除：非上传人被拒（10202 数据范围越界）")
    void deleteRejectsNonUploader() {
        SysAttachment attachment = new SysAttachment();
        attachment.setId(4L);
        attachment.setUploadBy("bob");
        attachment.setIsDeleted(0);
        when(sysAttachmentMapper.selectById(4L)).thenReturn(attachment);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.delete(4L));
        assertEquals(10202, exception.getCode());
    }

    @Test
    @DisplayName("删除：上传人本人逻辑删除成功")
    void deleteByUploaderSucceeds() {
        SysAttachment attachment = new SysAttachment();
        attachment.setId(5L);
        attachment.setUploadBy("alice");
        attachment.setIsDeleted(0);
        when(sysAttachmentMapper.selectById(5L)).thenReturn(attachment);

        AttachmentDeleteVO vo = attachmentService.delete(5L);

        assertEquals(5L, vo.id());
        assertEquals(1, vo.isDeleted());
        verify(sysAttachmentMapper).deleteById(5L);
    }

    @Test
    @DisplayName("删除：附件不存在被拒（18038）")
    void deleteRejectsMissingAttachment() {
        when(sysAttachmentMapper.selectById(404L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> attachmentService.delete(404L));
        assertEquals(18038, exception.getCode());
    }

    @Test
    @DisplayName("列表：按业务查询并派生下载地址（id 倒序由查询保证）")
    void listByBizReturnsVosWithDownloadUrl() {
        SysAttachment first = new SysAttachment();
        first.setId(2L);
        first.setFileName("b.pdf");
        first.setFileType("PDF");
        first.setFileSize(3L);
        first.setUploadBy("alice");
        SysAttachment second = new SysAttachment();
        second.setId(1L);
        second.setFileName("a.pdf");
        second.setFileType("PDF");
        second.setFileSize(4L);
        second.setUploadBy("alice");
        when(sysAttachmentMapper.selectList(any())).thenReturn(List.of(first, second));

        List<AttachmentVO> list = attachmentService.listByBiz("PURCHASE_RECEIPT", 100L);

        assertEquals(2, list.size());
        assertEquals("/api/system/attachments/2/download", list.get(0).downloadUrl());
        assertEquals("/api/system/attachments/1/download", list.get(1).downloadUrl());
    }

    private SysAttachment deletedAttachment() {
        SysAttachment attachment = new SysAttachment();
        attachment.setId(1L);
        attachment.setFileName("已删除.pdf");
        attachment.setIsDeleted(1);
        return attachment;
    }
}
