package me.north30.erp.system.integration;

import me.north30.erp.common.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * 导入任务域集成测试（/api/system/imports，D2 空占位口径）。
 * <p>无任务表：分页恒返回空分页；结果查询返回 10503 导入任务不存在。</p>
 */
@DisplayName("导入任务接口集成测试")
class ImportTaskIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/imports";

    @Nested
    @DisplayName("分页查询")
    class PageTest {

        @Test
        @DisplayName("无导入任务，返回空分页")
        void shouldReturnEmptyPage_whenNoTasks() {
            // When / Then：D2 无任务表，恒返回空分页
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", equalTo(0))
                .body("data.list.size()", equalTo(0));
        }
    }

    @Nested
    @DisplayName("导入结果查询")
    class ResultTest {

        @Test
        @DisplayName("查询不存在的任务id，返回422导入任务不存在")
        void shouldReturn422ImportTaskNotFound_whenTaskNotExist() {
            // Given
            String taskId = UUID.randomUUID().toString().replace("-", "");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/" + taskId + "/result")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.IMPORT_TASK_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问导入任务，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessImports() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(BASE_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
