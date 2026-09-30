package me.north30.erp.system.attachment.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.attachment.AttachmentTestFactory;
import me.north30.erp.system.attachment.converter.AttachmentConverterImpl;
import me.north30.erp.system.attachment.entity.SysAttachment;
import me.north30.erp.system.attachment.mapper.SysAttachmentMapper;
import me.north30.erp.system.attachment.vo.AttachmentDeleteVO;
import me.north30.erp.system.attachment.vo.AttachmentDownloadVO;
import me.north30.erp.system.attachment.vo.AttachmentVO;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.support.MpTableInfoInit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysAttachmentServiceImpl} 纯单元测试：上传校验链、文件名安全处理、下载守卫与上传人删除约束。
 * <p>构造器含 @Value 配置参数（存储根目录），Mockito 无法注入 String，故 @BeforeEach 手动构造被测类；
 * 存储根目录使用 JUnit @TempDir 隔离并自动清理，MultipartFile 内容全程 mock，不读写真实业务文件。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysAttachmentServiceImplTest {

    @Mock
    private SysAttachmentMapper sysAttachmentMapper;

    private SysAttachmentServiceImpl service;

    /** 附件存储根目录（每个用例独立的临时目录） */
    @TempDir
    Path tempDir;

    @Captor
    private ArgumentCaptor<SysAttachment> attachmentCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysAttachment.class);
    }

    @BeforeEach
    void setUp() {
        // @Value 配置参数无法由 Mockito 注入，手动构造（同 MP ServiceImpl 特例口径）
        service = new SysAttachmentServiceImpl(sysAttachmentMapper, new AttachmentConverterImpl(), tempDir.toString());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 装载当前登录用户到 SecurityContext（delete/delete 守卫场景使用）。
     */
    private static void loginAs(String username) {
        LoginUser loginUser = new LoginUser(1L, username, "jti-1", "refresh-jti-1");
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(loginUser, null, List.of()));
    }

    @Nested
    @DisplayName("upload：上传附件")
    class UploadTest {
        
        @Test
        @DisplayName("合法 PDF 文件上传成功")
        void shouldUploadAndInsert_whenValidFile() throws IOException {
            // Given：合法 PDF 文件，当前无登录上下文，业务附件数量未达上限
            MultipartFile file = AttachmentTestFactory.file("报价单.pdf", 1024L);
            given(sysAttachmentMapper.selectCount(any())).willReturn(0L);
            given(sysAttachmentMapper.insert(any(SysAttachment.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysAttachment.class).setId(100L);
                return 1;
            });

            // When
            AttachmentVO vo = service.upload(file, " PURCHASE_RECEIPT ", 1L, "PR202609001");

            // Then：VO 字段正确，落库字段与写盘目标路径符合约定
            assertThat(vo.id()).isEqualTo(100L);
            assertThat(vo.fileName()).isEqualTo("报价单.pdf");
            assertThat(vo.fileType()).isEqualTo("PDF");
            assertThat(vo.fileSize()).isEqualTo(1024L);
            assertThat(vo.bizType()).isEqualTo("PURCHASE_RECEIPT");
            assertThat(vo.bizId()).isEqualTo(1L);
            assertThat(vo.bizCode()).isEqualTo("PR202609001");
            assertThat(vo.uploadBy()).isEqualTo("system");
            assertThat(vo.uploadTime()).isNotNull();
            assertThat(vo.downloadUrl()).isEqualTo("/api/system/attachments/100/download");
            verify(sysAttachmentMapper).insert(attachmentCaptor.capture());
            SysAttachment inserted = attachmentCaptor.getValue();
            assertThat(inserted.getStorageType()).isEqualTo(1);
            assertThat(inserted.getFilePath()).matches("\\d{6}/[0-9a-f]{32}\\.pdf");
            verify(file).transferTo(argThat((Path target) ->
                target.startsWith(tempDir) && target.getFileName().toString().endsWith(".pdf")));
        }

        @Test
        @DisplayName("登录用户 alice 上传文件，上传人取 alice")
        void shouldSetUploadByFromLoginUser_whenLoggedIn() {
            // Given：当前登录用户 alice
            loginAs("alice");
            MultipartFile file = AttachmentTestFactory.file("报价单.pdf", 1024L);
            given(sysAttachmentMapper.selectCount(any())).willReturn(0L);
            given(sysAttachmentMapper.insert(any(SysAttachment.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysAttachment.class).setId(101L);
                return 1;
            });

            // When
            service.upload(file, "PURCHASE_RECEIPT", 1L, null);

            // Then：上传人取自安全上下文
            verify(sysAttachmentMapper).insert(attachmentCaptor.capture());
            assertThat(attachmentCaptor.getValue().getUploadBy()).isEqualTo("alice");
        }

        @Test
        @DisplayName("空文件上传抛出异常")
        void shouldThrow_whenFileEmpty() {
            // Given：空文件
            MultipartFile file = AttachmentTestFactory.emptyFile();

            // When + Then
            assertThatThrownBy(() -> service.upload(file, "PURCHASE_RECEIPT", 1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("上传文件不能为空");
            verifyNoInteractions(sysAttachmentMapper);
        }

        @Test
        @DisplayName("业务类型空白且业务主键为空抛出异常")
        void shouldThrow_whenBizParamsMissing() {
            // Given：业务类型空白且业务主键为空
            MultipartFile file = AttachmentTestFactory.fileNamed("报价单.pdf");

            // When + Then
            assertThatThrownBy(() -> service.upload(file, " ", null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("业务类型与业务主键不能为空");
            verifyNoInteractions(sysAttachmentMapper);
        }

        @Test
        @DisplayName("扩展名不在支持列表抛出异常")
        void shouldThrow_whenFileTypeNotSupported() {
            // Given：扩展名不在支持列表
            MultipartFile file = AttachmentTestFactory.fileNamed("malware.exe");

            // When + Then
            assertThatThrownBy(() -> service.upload(file, "PURCHASE_RECEIPT", 1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_TYPE_NOT_SUPPORTED.getCode()))
                .hasMessage("文件类型不支持");
            verifyNoInteractions(sysAttachmentMapper);
        }

        @Test
        @DisplayName("文件大小超过 20MB 上限抛出异常")
        void shouldThrow_whenFileSizeExceeded() {
            // Given：文件大小超过 20MB 上限
            MultipartFile file = AttachmentTestFactory.file("big.pdf", 21L * 1024 * 1024);

            // When + Then
            assertThatThrownBy(() -> service.upload(file, "PURCHASE_RECEIPT", 1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_SIZE_EXCEEDED.getCode()))
                .hasMessage("文件大小超出上限");
            verifyNoInteractions(sysAttachmentMapper);
        }

        @Test
        @DisplayName("业务对象已有 20 个附件（达到上限）抛出异常")
        void shouldThrow_whenCountExceeded() {
            // Given：业务对象已有 20 个附件（达到上限）
            MultipartFile file = AttachmentTestFactory.fileNamed("报价单.pdf");
            given(sysAttachmentMapper.selectCount(any())).willReturn(20L);

            // When + Then
            assertThatThrownBy(() -> service.upload(file, "PURCHASE_RECEIPT", 1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_COUNT_EXCEEDED.getCode()))
                .hasMessage("附件数量超出上限");
            verify(sysAttachmentMapper, never()).insert(any(SysAttachment.class));
        }

        @Test
        @DisplayName("原始文件名携带路径穿越片段，落库文件名剥离路径分隔符，存储相对路径仍由服务端生成")
        void shouldSanitizePathTraversalInFileName() {
            // Given：原始文件名携带路径穿越片段
            MultipartFile file = AttachmentTestFactory.file("..\\..\\secret\\报价单.pdf", 10L);
            given(sysAttachmentMapper.selectCount(any())).willReturn(0L);
            given(sysAttachmentMapper.insert(any(SysAttachment.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysAttachment.class).setId(102L);
                return 1;
            });

            // When
            service.upload(file, "PURCHASE_RECEIPT", 1L, null);

            // Then：落库文件名剥离路径分隔符，存储相对路径仍由服务端生成
            verify(sysAttachmentMapper).insert(attachmentCaptor.capture());
            SysAttachment inserted = attachmentCaptor.getValue();
            assertThat(inserted.getFileName()).isEqualTo("报价单.pdf");
            assertThat(inserted.getFileName()).doesNotContain("/").doesNotContain("\\");
            assertThat(inserted.getFilePath()).matches("\\d{6}/[0-9a-f]{32}\\.pdf");
        }

        @Test
        @DisplayName("写盘抛出 IOException 时，系统错误抛出异常")
        void shouldThrowSystemError_whenWriteFails() throws IOException {
            // Given：写盘抛出 IOException
            MultipartFile file = AttachmentTestFactory.file("报价单.pdf", 10L);
            given(sysAttachmentMapper.selectCount(any())).willReturn(0L);
            willThrow(new IOException("disk full")).given(file).transferTo(any(Path.class));

            // When + Then：写盘失败转系统错误且不落库
            assertThatThrownBy(() -> service.upload(file, "PURCHASE_RECEIPT", 1L, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.SYSTEM_ERROR.getCode()))
                .hasMessage("附件保存失败，请稍后重试");
            verify(sysAttachmentMapper, never()).insert(any(SysAttachment.class));
        }
    }

    @Nested
    @DisplayName("listByBiz：按业务对象查询附件")
    class ListByBizTest {

        @Test
        @DisplayName("按业务对象查询附件，返回 VO 列表")
        void shouldReturnVOList_whenAttachmentsExist() {
            // Given：同一业务对象下两条附件记录
            SysAttachment a1 = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "报价单.pdf", "202609/aaa.pdf", "PDF", 100L, "admin", 0);
            SysAttachment a2 = AttachmentTestFactory.attachment(4L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "验收单.pdf", "202609/bbb.pdf", "PDF", 200L, "alice", 0);
            given(sysAttachmentMapper.selectList(any())).willReturn(List.of(a1, a2));

            // When
            List<AttachmentVO> vos = service.listByBiz("PURCHASE_RECEIPT", 1L);

            // Then：按 Mapper 返回顺序映射并携带下载地址
            assertThat(vos).hasSize(2);
            assertThat(vos.get(0).id()).isEqualTo(5L);
            assertThat(vos.get(0).fileName()).isEqualTo("报价单.pdf");
            assertThat(vos.get(0).filePath()).isEqualTo("202609/aaa.pdf");
            assertThat(vos.get(0).uploadBy()).isEqualTo("admin");
            assertThat(vos.get(0).uploadTime()).isEqualTo(LocalDateTime.of(2026, 9, 28, 9, 30, 0));
            assertThat(vos.get(0).downloadUrl()).isEqualTo("/api/system/attachments/5/download");
            assertThat(vos.get(1).id()).isEqualTo(4L);
            assertThat(vos.get(1).downloadUrl()).isEqualTo("/api/system/attachments/4/download");
        }

        @Test
        @DisplayName("按业务对象查询附件，业务主键为空抛出异常")
        void shouldThrow_whenBizParamsMissing() {
            // Given：业务主键为空
            // When + Then
            assertThatThrownBy(() -> service.listByBiz("PURCHASE_RECEIPT", null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("业务类型与业务主键不能为空");
            verifyNoInteractions(sysAttachmentMapper);
        }
    }

    @Nested
    @DisplayName("download：下载附件")
    class DownloadTest {

        @Test
        @DisplayName("按附件 ID 下载附件，返回资源")
        void shouldReturnResource_whenPhysicalFileExists() throws IOException {
            // Given：附件元数据存在且物理文件已在临时存储目录创建（@TempDir 内、自动清理）
            Files.createDirectories(tempDir.resolve("202609"));
            Files.createFile(tempDir.resolve("202609").resolve("x.pdf"));
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "202609/x.pdf", "PDF", 10L, "admin", 0);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);

            // When
            AttachmentDownloadVO vo = service.download(5L);

            // Then：返回原始文件名与指向物理文件的资源
            assertThat(vo.fileName()).isEqualTo("x.pdf");
            assertThat(vo.file()).isInstanceOf(FileSystemResource.class);
            assertThat(vo.file().exists()).isTrue();
        }

        @Test
        @DisplayName("按不存在的附件 ID 下载附件，抛出异常")
        void shouldThrow_whenAttachmentNotFound() {
            // Given：附件记录不存在
            given(sysAttachmentMapper.selectById(5L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.download(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_NOT_FOUND.getCode()))
                .hasMessage("附件不存在");
        }

        @Test
        @DisplayName("按附件 ID 下载附件，附件已逻辑删除抛出异常")
        void shouldThrow_whenAttachmentLogicallyDeleted() {
            // Given：附件已逻辑删除
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "202609/x.pdf", "PDF", 10L, "admin", 1);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);

            // When + Then
            assertThatThrownBy(() -> service.download(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_NOT_FOUND.getCode()))
                .hasMessage("附件不存在");
        }

        @Test
        @DisplayName("按附件 ID 下载附件，物理文件缺失抛出异常")
        void shouldThrow_whenPhysicalFileMissing() {
            // Given：物理文件缺失（临时目录内未创建）
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "202609/gone.pdf", "PDF", 10L, "admin", 0);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);

            // When + Then
            assertThatThrownBy(() -> service.download(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_NOT_FOUND.getCode()))
                .hasMessage("附件不存在");
        }

        @Test
        @DisplayName("按附件 ID 下载附件，存储相对路径逃逸出存储根目录抛出异常")
        void shouldThrow_whenPathEscapesStoreRoot() {
            // Given：存储相对路径逃逸出存储根目录（路径穿越守卫，先于物理文件存在性检查）
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "../outside.pdf", "PDF", 10L, "admin", 0);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);

            // When + Then
            assertThatThrownBy(() -> service.download(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.ATTACHMENT_NOT_FOUND.getCode()))
                .hasMessage("附件不存在");
        }
    }

    @Nested
    @DisplayName("delete：删除附件")
    class DeleteTest {

        @Test
        @DisplayName("按附件 ID 删除附件，返回删除结果")
        void shouldDelete_whenOperatorIsUploader() {
            // Given：操作人与上传人一致
            loginAs("admin");
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "202609/x.pdf", "PDF", 10L, "admin", 0);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);
            given(sysAttachmentMapper.deleteById(5L)).willReturn(1);

            // When
            AttachmentDeleteVO vo = service.delete(5L);

            // Then
            assertThat(vo.id()).isEqualTo(5L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysAttachmentMapper).deleteById(5L);
        }

        @Test
        void shouldThrow_whenOperatorNotUploader() {
            // Given：操作人 alice 非上传人 admin（横向数据守卫）
            loginAs("alice");
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "202609/x.pdf", "PDF", 10L, "admin", 0);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);

            // When + Then
            assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(SystemManageErrorCode.DATA_SCOPE_DENIED.getCode()))
                .hasMessage("仅上传人可删除该附件");
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("按附件 ID 删除附件，无登录上下文抛出异常")
        void shouldThrow_whenNoLoginContext() {
            // Given：无登录上下文（SecurityContext 为空）
            SysAttachment attachment = AttachmentTestFactory.attachment(5L, "PURCHASE_RECEIPT", 1L, "PR202609001",
                "x.pdf", "202609/x.pdf", "PDF", 10L, "admin", 0);
            given(sysAttachmentMapper.selectById(5L)).willReturn(attachment);

            // When + Then
            assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.AUTH_EXPIRED.getCode()))
                .hasMessage("未认证或登录已过期，请重新登录");
            verify(sysAttachmentMapper, never()).deleteById(any(Long.class));
        }
    }
}
