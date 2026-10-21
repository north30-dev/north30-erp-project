package me.north30.erp.system.integration;

import io.restassured.response.Response;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * 日志域集成测试（/api/system/login-logs、/api/system/audit-logs，接口文档 5.8）。
 * <p>只读域：分页、过滤、详情；登录日志数据由本类登录行为自然产生（自建自读，不造数）。</p>
 */
@DisplayName("日志接口集成测试")
class LogIntegrationTest extends ApiIntegrationTest {

    private static final String LOGIN_LOG_PATH = "/api/system/login-logs";
    private static final String AUDIT_LOG_PATH = "/api/system/audit-logs";

    @Nested
    @DisplayName("登录日志分页查询")
    class LoginLogPageTest {

        @Test
        @DisplayName("admin已登录，返回登录日志分页结果")
        void shouldReturnPage_whenAdminLoggedIn() {
            // Given：登录行为已产生登录日志（本测试 adminToken() 触发登录）
            String token = adminToken();

            // When / Then
            given().port(port).header("Authorization", "Bearer " + token)
                .queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(LOGIN_LOG_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list.size()", greaterThanOrEqualTo(1))
                .body("data.pages", greaterThanOrEqualTo(1));
        }

        @Test
        @DisplayName("按用户名过滤，返回过滤后的分页结果")
        void shouldReturnFilteredPage_whenUsernameFilterPresent() {
            // Given
            String token = adminToken();

            // When / Then
            given().port(port).header("Authorization", "Bearer " + token)
                .queryParam("username", "admin").queryParam("pageSize", 10)
                .when().get(LOGIN_LOG_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", greaterThanOrEqualTo(1));
        }

        @Test
        @DisplayName("页大小超过200，返回400参数错误")
        void shouldReturn400ParamError_whenPageSizeOver200() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageSize", 201)
                .when().get(LOGIN_LOG_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("审计日志分页查询")
    class AuditLogPageTest {

        @Test
        @DisplayName("admin已登录，返回审计日志分页结果")
        void shouldReturnPage_whenAdminLoggedIn() {
            // Given：角色创建等写操作产生审计日志（RoleIntegrationTest 已触发，本域只读断言结构）
            String token = adminToken();

            // When / Then
            Response response = given().port(port).header("Authorization", "Bearer " + token)
                .queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(AUDIT_LOG_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .extract().response();
            // 分页结构三点断言（审计日志可能为空，total>=0 即可）
            org.assertj.core.api.Assertions.assertThat(response.jsonPath().getLong("data.total")).isGreaterThanOrEqualTo(0);
            org.assertj.core.api.Assertions.assertThat(response.jsonPath().getList("data.list")).isNotNull();
            org.assertj.core.api.Assertions.assertThat(response.jsonPath().getLong("data.pages")).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("按时间区间过滤，返回过滤后的分页结果")
        void shouldReturnFilteredPage_whenTimeRangePresent() {
            // Given：时间区间（2026-01-01 起）
            String token = adminToken();

            // When / Then
            given().port(port).header("Authorization", "Bearer " + token)
                .queryParam("startTime", "2026-01-01").queryParam("pageSize", 10)
                .when().get(AUDIT_LOG_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200));
        }
    }

    @Nested
    @DisplayName("审计日志详情")
    class AuditLogDetailTest {

        @Test
        @DisplayName("查询不存在的审计日志id，返回422审计日志不存在")
        void shouldReturn422AuditLogNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(AUDIT_LOG_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.AUDIT_LOG_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问登录日志，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessLoginLogs() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(LOGIN_LOG_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
