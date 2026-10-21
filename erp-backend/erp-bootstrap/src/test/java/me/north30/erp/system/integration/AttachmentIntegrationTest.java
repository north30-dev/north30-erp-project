package me.north30.erp.system.integration;

import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

/**
 * 附件域集成测试（/api/system/attachments，接口文档 5.9）。
 * <p>上传/列表/下载/删除闭环；文件类型白名单负向（10401）；bizType+bizId 必填校验；
 * 大小/数量上限走 sys_config 现值，不做真实改配置的危险操作。
 * 临时附件上传后立即删除（自建自清）。</p>
 */
@DisplayName("附件管理接口集成测试")
class AttachmentIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/attachments";

    /** 最小合法 PNG 文件字节（1x1 透明像素） */
    private static final byte[] PNG_BYTES = new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06,
        0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00, 0x0A,
        0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00,
        0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
        (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    /** 本 JVM 内上传成功的附件 id（供 @AfterEach 兜底删除，自建自清） */
    private static final List<Long> UPLOADED_IDS = new CopyOnWriteArrayList<>();

    /** 上传附件并返回 id */
    private long uploadPng(long bizId) {
        long id = given().port(port).header("Authorization", "Bearer " + adminToken())
            .multiPart("file", "it_test.png", PNG_BYTES, "image/png")
            .queryParam("bizType", "it_biz").queryParam("bizId", bizId)
            .when().post(BASE_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");
        UPLOADED_IDS.add(id);
        return id;
    }

    /** 每用例兜底清理：删除上传过的附件（容忍已删除） */
    @AfterEach
    void cleanup() {
        for (Long id : UPLOADED_IDS) {
            deleteAttachment(id);
        }
        UPLOADED_IDS.clear();
    }

    /** 删除附件（兜底清理，容忍已删除） */
    private void deleteAttachment(long id) {
        given().port(port).header("Authorization", "Bearer " + adminToken())
            .when().delete(BASE_PATH + "/" + id);
    }

    @Nested
    @DisplayName("上传附件")
    class UploadTest {

        @Test
        @DisplayName("上传合法PNG文件，返回附件id")
        void shouldUploadAndReturnId_whenFileValid() {
            // Given
            long bizId = System.currentTimeMillis();

            // When / Then
            long id = uploadPng(bizId);
            deleteAttachment(id);
        }

        @Test
        @DisplayName("上传非白名单类型文件，返回422类型不支持")
        void shouldReturn422TypeNotSupported_whenFileTypeNotAllowed() {
            // Given：txt 不在白名单（pdf/jpg/jpeg/png/xlsx/docx）
            long bizId = System.currentTimeMillis();

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .multiPart("file", "it_test.txt", "hello".getBytes(), "text/plain")
                .queryParam("bizType", "it_biz").queryParam("bizId", bizId)
                .when().post(BASE_PATH)
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.ATTACHMENT_TYPE_NOT_SUPPORTED.getCode()));
        }

        @Test
        @DisplayName("缺少bizType参数，返回400参数错误")
        void shouldReturn400ParamError_whenBizTypeMissing() {
            // Given：bizType/bizId 必填（@Valid 入口校验）
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .multiPart("file", "it_test.png", PNG_BYTES, "image/png")
                .when().post(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("附件列表查询")
    class ListTest {

        @Test
        @DisplayName("按业务类型与业务id查询，返回已上传附件列表")
        void shouldReturnUploadedList_whenBizTypeAndBizIdPresent() {
            // Given
            long bizId = System.currentTimeMillis();
            long id = uploadPng(bizId);
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .queryParam("bizType", "it_biz").queryParam("bizId", bizId)
                    .when().get(BASE_PATH)
                    .then().statusCode(200)
                    .body("code", equalTo(200))
                    .body("data.size()", equalTo(1));
            } finally {
                deleteAttachment(id);
            }
        }

        @Test
        @DisplayName("缺少bizId参数，返回400参数错误")
        void shouldReturn400ParamError_whenBizIdMissing() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("bizType", "it_biz")
                .when().get(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("下载附件")
    class DownloadTest {

        @Test
        @DisplayName("下载存在的附件，返回文件流")
        void shouldReturnFileStream_whenAttachmentExists() {
            // Given
            long bizId = System.currentTimeMillis();
            long id = uploadPng(bizId);
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().get(BASE_PATH + "/" + id + "/download")
                    .then().statusCode(200)
                    .contentType("application/octet-stream");
            } finally {
                deleteAttachment(id);
            }
        }

        @Test
        @DisplayName("下载不存在的附件id，返回422附件不存在")
        void shouldReturn422AttachmentNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/999999999/download")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ATTACHMENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("删除附件")
    class DeleteTest {

        @Test
        @DisplayName("删除存在的附件，删除后不可见")
        void shouldDeleteAndInvisible_whenAttachmentExists() {
            // Given
            long bizId = System.currentTimeMillis();
            long id = uploadPng(bizId);

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then：确认查不到
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("bizType", "it_biz").queryParam("bizId", bizId)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .body("data", empty());
        }

        @Test
        @DisplayName("删除不存在的附件id，返回422附件不存在")
        void shouldReturn422AttachmentNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ATTACHMENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问附件列表，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessAttachment() {
            // When / Then（带合法查询参数越过 @Valid 校验，只验证鉴权 403；缺参会先 400）
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .queryParam("bizType", "it_biz").queryParam("bizId", "1")
                .when().get(BASE_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
