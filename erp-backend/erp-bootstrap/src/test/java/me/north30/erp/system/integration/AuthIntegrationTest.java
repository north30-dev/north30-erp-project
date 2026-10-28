package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 认证授权域集成测试（/api/auth，接口文档 4）。
 * <p>每个接口一个 @Nested 分组；错误密码用例对自建低权限用户执行，
 * 避免对 admin 账号累计登录失败计数（锁定阈值 5 次/30 分钟）。</p>
 */
@DisplayName("认证授权接口集成测试")
class AuthIntegrationTest extends ApiIntegrationTest {

    private static final String LOGIN_PATH = "/api/auth/login";

    /** 直写验证码到 Redis 并返回 captchaKey（与后端 CaptchaUtil 写入规则一致） */
    private String prepareCaptcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue()
            .set(RedisKeyConstants.captchaKey(captchaKey), "IT7A", Duration.ofMinutes(5));
        return captchaKey;
    }

    /** 登录并返回双令牌 */
    private record LoginResult(String accessToken, String refreshToken) {
    }

    private LoginResult login(String username, String password) {
        Response response = given()
            .port(port).contentType(ContentType.JSON)
            .body(Map.of("username", username, "password", password,
                "captcha", "IT7A", "captchaKey", prepareCaptcha()))
            .when().post(LOGIN_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().response();
        return new LoginResult(response.jsonPath().getString("data.accessToken"),
            response.jsonPath().getString("data.refreshToken"));
    }

    @Nested
    @DisplayName("获取验证码接口")
    class CaptchaTest {

        @Test
        @DisplayName("获取验证码成功，返回captchaKey与Base64图片")
        void shouldReturnKeyAndBase64Image_whenCaptchaCreated() {
            // When / Then
            given().port(port)
                .when().post("/api/auth/captcha")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.captchaKey", notNullValue())
                .body("data.captchaImage", notNullValue());
        }
    }

    @Nested
    @DisplayName("登录接口")
    class LoginTest {

        @Test
        @DisplayName("登录成功，返回双令牌")
        void shouldReturnDualTokensAndUserInfo_whenLoginSuccess() {
            // When / Then
            LoginResult result = login(ADMIN_USERNAME, ADMIN_PASSWORD);
            assertThat(result.accessToken()).isNotBlank();
            assertThat(result.refreshToken()).isNotBlank();
        }

        @Test
        @DisplayName("验证码错误，返回422与验证码错误码")
        void shouldReturn422CaptchaError_whenCaptchaWrong() {
            // Given
            Map<String, Object> body = Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD,
                "captcha", "ZZZZ", "captchaKey", UUID.randomUUID().toString().replace("-", ""));

            // When / Then
            given().port(port).contentType(ContentType.JSON)
                .body(body)
                .when().post(LOGIN_PATH)
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.CAPTCHA_ERROR.getCode()));
        }

        @Test
        @DisplayName("密码错误，返回422与用户名或密码错误码")
        void shouldReturn422UsernamePasswordError_whenPasswordWrong() {
            // Given（对自建用户，不污染 admin 失败计数）
            Map<String, Object> body = Map.of("username", limitedUsername(), "password", "Wrong@999999",
                "captcha", "IT7A", "captchaKey", prepareCaptcha());

            // When / Then
            given().port(port).contentType(ContentType.JSON)
                .body(body)
                .when().post(LOGIN_PATH)
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.USERNAME_PASSWORD_ERROR.getCode()));
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("username", ADMIN_USERNAME);

            // When / Then
            given().port(port).contentType(ContentType.JSON)
                .body(body)
                .when().post(LOGIN_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("刷新令牌接口")
    class RefreshTest {

        @Test
        @DisplayName("刷新令牌有效，签发新的accessToken")
        void shouldIssueNewAccessToken_whenRefreshTokenValid() {
            // Given
            LoginResult result = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            // When / Then
            given().port(port).contentType(ContentType.JSON)
                .body(Map.of("refreshToken", result.refreshToken()))
                .when().post("/api/auth/refresh")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.accessToken", notNullValue());
        }

        @Test
        @DisplayName("伪造刷新令牌，返回422与刷新令牌无效码")
        void shouldReturn422RefreshTokenInvalid_whenTokenForged() {
            // Given
            Map<String, Object> body = Map.of("refreshToken", "invalid.token.value");

            // When / Then
            given().port(port).contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/auth/refresh")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.REFRESH_TOKEN_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("登出接口")
    class LogoutTest {

        @Test
        @DisplayName("登出成功，原accessToken失效")
        void shouldRevokeAccessToken_whenLogoutSuccess() {
            // Given
            LoginResult result = login(ADMIN_USERNAME, ADMIN_PASSWORD);

            // When
            given().port(port)
                .header("Authorization", "Bearer " + result.accessToken())
                .when().post("/api/auth/logout")
                .then().statusCode(200).body("code", equalTo(200));

            // Then：原 access token 已失效
            given().port(port)
                .header("Authorization", "Bearer " + result.accessToken())
                .when().get("/api/auth/me")
                .then().statusCode(401);
        }
    }

    @Nested
    @DisplayName("当前用户信息接口")
    class MeTest {

        @Test
        @DisplayName("admin已登录，返回当前用户信息")
        void shouldReturnAdminInfo_whenAdminLoggedIn() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + adminToken())
                .when().get("/api/auth/me")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.username", equalTo(ADMIN_USERNAME));
        }

        @Test
        @DisplayName("未携带令牌，返回401未认证")
        void shouldReturn401_whenTokenAbsent() {
            // When / Then
            given().port(port)
                .when().get("/api/auth/me")
                .then().statusCode(401)
                .body("code", equalTo(CommonErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @DisplayName("伪造令牌，返回401未认证")
        void shouldReturn401_whenTokenForged() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer forged.invalid.token")
                .when().get("/api/auth/me")
                .then().statusCode(401);
        }

        @Test
        @DisplayName("访问不存在的路径，返回404")
        void shouldReturn404_whenPathNotExist() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + adminToken())
                .when().get("/api/system/__not_exist__")
                .then().statusCode(404);
        }
    }

    @Nested
    @DisplayName("用户权限集合接口")
    class PermsTest {

        @Test
        @DisplayName("admin已登录，返回权限集合")
        void shouldReturnPermSet_whenAdminLoggedIn() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + adminToken())
                .when().get("/api/auth/perms")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
        }
    }

    @Nested
    @DisplayName("用户菜单接口")
    class MenusTest {

        @Test
        @DisplayName("admin已登录，返回非空菜单树")
        void shouldReturnNonEmptyTree_whenAdminLoggedIn() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + adminToken())
                .when().get("/api/auth/menus")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThan(0));
        }
    }

    @Nested
    @DisplayName("修改密码接口")
    class PasswordTest {

        @Test
        @DisplayName("旧密码错误，返回422与旧密码错误码")
        void shouldReturn422OldPasswordError_whenOldPasswordWrong() {
            // Given（对自建用户，不改 admin 环境）
            Map<String, Object> body = Map.of("oldPassword", "Wrong@111111",
                "newPassword", "NewPass@2026", "confirmPassword", "NewPass@2026");

            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/api/auth/password")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.OLD_PASSWORD_ERROR.getCode()));
        }

        @Test
        @DisplayName("两次新密码不一致，返回400参数错误")
        void shouldReturn422ParamError_whenConfirmPasswordMismatch() {
            // Given
            Map<String, Object> body = Map.of("oldPassword", LIMITED_PASSWORD,
                "newPassword", "NewPass@2026", "confirmPassword", "Other@999999");

            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/api/auth/password")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }
}
