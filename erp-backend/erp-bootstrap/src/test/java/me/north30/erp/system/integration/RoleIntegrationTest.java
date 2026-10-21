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
import static org.hamcrest.Matchers.notNullValue;

/**
 * 角色管理域集成测试（/api/system/roles，接口文档 5.2）。
 * <p>每个接口一个 @Nested 分组；正向 CRUD 闭环、菜单分配、数据范围 + 负向（缺必填/重复编码/内置保护/越权）。</p>
 */
@DisplayName("角色管理接口集成测试")
class RoleIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/roles";

    /** 创建临时角色（随机后缀），可按 overrides 覆盖字段，返回 id */
    private long createRole(Map<String, Object> overrides) {
        String suffix = suffix();
        Map<String, Object> body = new HashMap<>();
        body.put("roleCode", "it_role_" + suffix);
        body.put("roleName", "集成测试角色_" + suffix);
        body.put("status", 1);
        body.putAll(overrides);
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON).body(body)
            .when().post(BASE_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");
    }

    /** 每用例兜底清理：删除本类创建的 it_role_ 前缀角色（自建自清） */
    @AfterEach
    void cleanup() {
        List<Map<String, Object>> list = given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("roleCode", "it_role_").queryParam("pageSize", 200)
            .when().get(BASE_PATH)
            .then().extract().jsonPath().getList("data.list");
        if (list != null) {
            for (Map<String, Object> item : list) {
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + item.get("id"));
            }
        }
    }

    @Nested
    @DisplayName("分页查询")
    class PageTest {

        @Test
        @DisplayName("按角色编码过滤，返回分页结果")
        void shouldReturnPageWithFilteredResult_whenRoleCodeFilterPresent() {
            // Given
            String roleCode = "it_role_" + suffix();
            createRole(Map.of("roleCode", roleCode));

            // When / Then：三点断言 total/list/pages
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("roleCode", roleCode).queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list.size()", greaterThanOrEqualTo(1))
                .body("data.pages", greaterThanOrEqualTo(1));
        }
    }

    @Nested
    @DisplayName("创建角色")
    class CreateTest {

        @Test
        @DisplayName("创建角色成功，可通过数据范围查询确认")
        void shouldCreateRole_whenBodyValid() {
            // When / Then
            long id = createRole(Map.of());
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/" + id + "/data-scopes")
                .then().statusCode(200);
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("roleName", "缺编码角色");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("角色编码重复，返回422编码已存在")
        void shouldReturn422RoleCodeExists_whenRoleCodeDuplicated() {
            // Given
            String roleCode = "it_role_" + suffix();
            long id = createRole(Map.of("roleCode", roleCode));
            Map<String, Object> body = Map.of("roleCode", roleCode, "roleName", "重复编码角色", "status", 1);
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post(BASE_PATH)
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.ROLE_CODE_EXISTS.getCode()));
            } finally {
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + id);
            }
        }
    }

    @Nested
    @DisplayName("修改角色")
    class UpdateTest {

        @Test
        @DisplayName("版本号匹配，修改成功且变更可见")
        void shouldUpdateAndVisible_whenVersionMatch() {
            // Given（新建行乐观锁版本为 0；分页 VO 不含 version 字段）
            String roleCode = "it_role_" + suffix();
            long id = createRole(Map.of("roleCode", roleCode));

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("roleName", "集成测试角色_已修改", "version", 0, "status", 1))
                .when().put(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("roleCode", roleCode).queryParam("pageSize", 10)
                .when().get(BASE_PATH)
                .then().body("data.list[0].roleName", equalTo("集成测试角色_已修改"));
        }
    }

    @Nested
    @DisplayName("删除角色")
    class DeleteTest {

        @Test
        @DisplayName("删除未被引用的角色，删除后不可见")
        void shouldDeleteRole_whenRoleNotReferenced() {
            // Given
            long id = createRole(Map.of());

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then：确认查不到
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/" + id + "/data-scopes")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ROLE_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("删除内置角色，返回422内置保护")
        void shouldReturn422RoleBuiltin_whenDeleteBuiltinRole() {
            // Given（内置角色经分页检索 isBuiltin=1；请求被拒，环境不变）
            long builtinId = given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageSize", 200)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.list.find { it.isBuiltin == 1 }.id");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + builtinId)
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ROLE_BUILTIN.getCode()));
        }

        @Test
        @DisplayName("删除不存在的角色id，返回422角色不存在")
        void shouldReturn422RoleNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ROLE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("分配菜单")
    class AssignMenusTest {

        @Test
        @DisplayName("分配空菜单列表，清空角色菜单")
        void shouldClearMenus_whenMenuIdsEmpty() {
            // Given
            long id = createRole(Map.of());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("menuIds", List.of()))
                .when().put(BASE_PATH + "/" + id + "/menus")
                .then().statusCode(200).body("code", equalTo(200));
        }

        @Test
        @DisplayName("分配不存在的菜单id，返回422菜单不存在")
        void shouldReturn422MenuNotFound_whenMenuIdNotExist() {
            // Given
            long id = createRole(Map.of());
            Map<String, Object> body = Map.of("menuIds", List.of(999999999L));

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/" + id + "/menus")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.MENU_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("数据范围查询")
    class DataScopeTest {

        @Test
        @DisplayName("查询已创建角色的数据范围，返回范围结构")
        void shouldReturnScopeStructure_whenRoleCreated() {
            // Given
            long id = createRole(Map.of());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/" + id + "/data-scopes")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
        }

        @Test
        @DisplayName("查询不存在角色的数据范围，返回422角色不存在")
        void shouldReturn422RoleNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/999999999/data-scopes")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ROLE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问角色列表，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessList() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(BASE_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
