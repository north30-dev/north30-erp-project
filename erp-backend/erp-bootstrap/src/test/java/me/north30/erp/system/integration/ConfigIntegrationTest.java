package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * 参数配置域集成测试（/api/system/configs，接口文档 5.6）。
 * <p>每个接口一个 @Nested 分组；key 格式校验（@Pattern 400）、内置参数保护（is_system=1 只测负向）；
 * 分页 VO 不含 version，修改用例以乐观锁冲突（version=999）口径覆盖。</p>
 */
@DisplayName("参数配置接口集成测试")
class ConfigIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/configs";

    /** key 随机后缀（固定 it 前缀保证小写字母开头，满足 @Pattern 校验） */
    private static String keySuffix() {
        return "it" + suffix();
    }

    /** 创建临时参数（随机后缀 key），返回 id */
    private long createConfig(String configKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("configKey", configKey);
        body.put("configName", "集成测试参数_" + configKey);
        body.put("configValue", "10");
        body.put("valueType", 2);
        body.put("configGroup", "it");
        body.put("isSystem", 0);
        body.put("status", 1);
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON).body(body)
            .when().post(BASE_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");
    }

    @AfterEach
    void cleanup() {
        // 兜底清理：按 key 前缀检索并删除（is_system 只创建 0，可安全删除）
        List<Map<String, Object>> list = given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("configKey", "it.config.").queryParam("pageSize", 200)
            .when().get(BASE_PATH)
            .then().extract().jsonPath().getList("data.list");
        if (list != null) {
            for (Map<String, Object> item : list) {
                given()
                    .port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + item.get("id"))
                    .then().statusCode(200);
            }
        }
    }

    @Nested
    @DisplayName("分页查询")
    class PageTest {

        @Test
        @DisplayName("按参数键过滤，返回分页结果")
        void shouldReturnPage_whenConfigKeyFilterPresent() {
            // Given
            createConfig("it.config.pagesize." + keySuffix());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("configKey", "it.config.").queryParam("pageNum", 1).queryParam("pageSize", 10)
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
    @DisplayName("创建参数")
    class CreateTest {

        @Test
        @DisplayName("创建参数成功，返回参数id")
        void shouldCreateConfig_whenBodyValid() {
            // When / Then
            long id = createConfig("it.config.valid." + keySuffix());
            org.assertj.core.api.Assertions.assertThat(id).isPositive();
        }

        @Test
        @DisplayName("参数键格式非法，返回400参数错误")
        void shouldReturn400ParamError_whenConfigKeyFormatInvalid() {
            // Given：大写/无点号违反 @Pattern
            Map<String, Object> body = Map.of("configKey", "BadKey", "configName", "非法键",
                "configValue", "1", "valueType", 2, "configGroup", "it");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("configKey", "it.config.missing." + keySuffix());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("参数键重复，返回422唯一键冲突")
        void shouldReturn422DuplicateKey_whenConfigKeyDuplicated() {
            // Given
            String key = "it.config.dup." + keySuffix();
            createConfig(key);
            Map<String, Object> body = Map.of("configKey", key, "configName", "重复键",
                "configValue", "1", "valueType", 2, "configGroup", "it");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH)
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.DUPLICATE_KEY.getCode()));
        }
    }

    @Nested
    @DisplayName("修改参数")
    class UpdateTest {

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldReturn422OptimisticLockConflict_whenVersionStale() {
            // Given（分页 VO 不含 version，用 version=999 触发冲突）
            long id = createConfig("it.config.upd." + keySuffix());
            Map<String, Object> body = Map.of("configValue", "20", "version", 999);

            // When / Then（乐观锁 10601 统一返回 HTTP 409）
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/" + id)
                .then().statusCode(409)
                .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("修改不存在的参数id，返回422参数不存在")
        void shouldReturn422ConfigNotFound_whenIdNotExist() {
            // Given
            Map<String, Object> body = Map.of("configValue", "1", "version", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.CONFIG_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("删除参数")
    class DeleteTest {

        @Test
        @DisplayName("删除非内置参数，删除后不可见")
        void shouldDeleteConfig_whenNotBuiltin() {
            // Given
            long id = createConfig("it.config.del." + keySuffix());

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then：分页查不到
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("configKey", "it.config.del.").queryParam("pageSize", 10)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .body("data.total", equalTo(0));
        }

        @Test
        @DisplayName("删除系统内置参数，返回422内置不可删除")
        void shouldReturn422BuiltinUndeletable_whenConfigIsSystem() {
            // Given：内置参数经分页检索 isSystem=1；请求被拒，环境不变
            long builtinId = given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageSize", 200)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.list.find { it.isSystem == 1 }.id");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + builtinId)
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.BUILTIN_CONFIG_UNDELETABLE.getCode()));
        }

        @Test
        @DisplayName("删除不存在的参数id，返回422参数不存在")
        void shouldReturn422ConfigNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.CONFIG_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问参数配置，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessConfig() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(BASE_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
