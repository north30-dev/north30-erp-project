package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * 编号序列域集成测试（/api/system/code-sequences，接口文档 5.7）。
 * <p>分页查询 + 重置（同值重置幂等跳过、不存在 18035、格式非法 400）；不触碰种子序列当前值。</p>
 */
@DisplayName("编号序列接口集成测试")
class CodeSequenceIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/code-sequences";

    @Nested
    @DisplayName("分页查询")
    class PageTest {

        @Test
        @DisplayName("admin已登录，返回序列分页结果")
        void shouldReturnPage_whenAdminLoggedIn() {
            // When / Then：种子序列已存在，三点断言
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list.size()", greaterThanOrEqualTo(1))
                .body("data.pages", greaterThanOrEqualTo(1));
        }

        @Test
        @DisplayName("页大小超过200，返回400参数错误")
        void shouldReturn400ParamError_whenPageSizeOver200() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageSize", 201)
                .when().get(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("重置序列")
    class ResetTest {

        @Test
        @DisplayName("重置为当前流水号同值，幂等跳过")
        void shouldIdempotentlySkip_whenResetToSameCurrentNo() {
            // Given：取种子序列首行，以当前流水号同值重置（幂等跳过，不改数据）
            Response row = given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageNum", 1).queryParam("pageSize", 1)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .extract().response();
            String bizType = row.jsonPath().getString("data.list[0].bizType");
            String period = row.jsonPath().getString("data.list[0].period");
            int currentNo = row.jsonPath().getInt("data.list[0].currentNo");

            // When / Then：同值重置幂等成功
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("bizType", bizType, "period", period, "currentNo", currentNo, "reason", "集成测试：同值重置幂等"))
                .when().post(BASE_PATH + "/reset")
                .then().statusCode(200)
                .body("code", equalTo(200));
        }

        @Test
        @DisplayName("业务类型不存在，返回422序列不存在")
        void shouldReturn422SequenceNotFound_whenBizTypeNotExist() {
            // Given
            Map<String, Object> body = Map.of("bizType", "no_such_biz_" + suffix(),
                "period", "202601", "currentNo", 1, "reason", "集成测试：序列不存在");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH + "/reset")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.SEQUENCE_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("期间格式非法，返回400参数错误")
        void shouldReturn400ParamError_whenPeriodFormatInvalid() {
            // Given：period 非 yyyyMM 违反 @Pattern
            Map<String, Object> body = Map.of("bizType", "whatever",
                "period", "2026-01", "currentNo", 1, "reason", "集成测试：期间格式非法");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH + "/reset")
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("bizType", "whatever");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH + "/reset")
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问编号序列，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessSequence() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(BASE_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
