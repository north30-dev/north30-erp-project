package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 用户管理域集成测试（/api/system/users，接口文档 5.1）。
 * <p>每个接口一个 @Nested 分组；正向 CRUD 闭环 + 负向（缺必填/重复唯一键/不存在 ID/乐观锁冲突/越权）；
 * 临时数据随机后缀自建自清，@AfterEach 按前缀兜底清理。</p>
 */
@DisplayName("用户管理接口集成测试")
class UserIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/users";

    /** 创建临时用户（随机后缀），可按 overrides 覆盖字段，返回 id */
    private long createUser(Map<String, Object> overrides) {
        String suffix = suffix();
        Map<String, Object> body = new HashMap<>();
        body.put("userCode", "IT-" + suffix);
        body.put("username", "it_user_" + suffix);
        body.put("password", "Test@123456");
        body.put("realName", "集成测试用户_" + suffix);
        body.put("status", 1);
        body.putAll(overrides);
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON).body(body)
            .when().post(BASE_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data");
    }

    /** 查询用户详情（返回完整响应体便于取 version/username） */
    private Response getDetail(long id) {
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .when().get(BASE_PATH + "/" + id)
            .then().extract().response();
    }

    /** 经分页检索取 admin 用户 id（不假设 admin 主键值；username 为模糊匹配，需精确比对） */
    private long adminId() {
        List<Map<String, Object>> list = given().port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("username", "admin").queryParam("pageSize", 10)
            .when().get(BASE_PATH)
            .then().statusCode(200)
            .extract().jsonPath().getList("data.list");
        return list.stream()
            .filter(user -> "admin".equals(user.get("username")))
            .findFirst().map(user -> Long.parseLong(String.valueOf(user.get("id"))))
            .orElseThrow(() -> new IllegalStateException("测试库应存在内置 admin 用户"));
    }

    @AfterEach
    void cleanup() {
        // 兜底清理：按测试用户名前缀检索并逻辑删除（删除失败由断言暴露）
        List<Map<String, Object>> list = given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("username", "it_user_").queryParam("pageSize", 200)
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
        @DisplayName("按用户名过滤，返回分页结果")
        void shouldReturnPageWithFilteredResult_whenUsernameFilterPresent() {
            // Given
            createUser(Map.of());

            // When / Then：三点断言 total/list/pages
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("username", "it_user_").queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(BASE_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list.size()", greaterThanOrEqualTo(1))
                .body("data.pages", greaterThanOrEqualTo(1));
        }

        @Test
        @DisplayName("页码小于1，返回400参数错误")
        void shouldReturn400ParamError_whenPageSizeZero() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("pageSize", 0)
                .when().get(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
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
    @DisplayName("用户导出")
    class ExportTest {

        @Test
        @DisplayName("导出用户，返回CSV文件流")
        void shouldReturnCsvFileStream_whenExportCalled() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/export")
                .then().statusCode(200)
                .contentType("text/csv");
        }
    }

    @Nested
    @DisplayName("用户详情")
    class DetailTest {

        @Test
        @DisplayName("查询存在的用户，返回含审计字段的详情")
        void shouldReturnDetailWithAuditFields_whenUserExists() {
            // Given
            long id = createUser(Map.of());

            // When / Then（详情 VO 审计口径：createTime 必有；createBy 不在出参契约内）
            getDetail(id).then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.username", notNullValue())
                .body("data.createTime", notNullValue());
        }

        @Test
        @DisplayName("查询不存在的用户id，返回422用户不存在")
        void shouldReturn422UserNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.USER_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("创建用户")
    class CreateTest {

        @Test
        @DisplayName("创建用户成功，返回用户id")
        void shouldCreateUser_whenBodyValid() {
            // When / Then
            long id = createUser(Map.of());
            getDetail(id).then().statusCode(200);
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("userCode", "IT-X");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("用户名重复，返回422用户名已存在")
        void shouldReturn422UsernameExists_whenUsernameDuplicated() {
            // Given
            long id = createUser(Map.of());
            String username = getDetail(id).jsonPath().getString("data.username");
            Map<String, Object> body = Map.of("userCode", "IT-DUP-" + suffix(), "username", username,
                "password", "Test@123456", "realName", "重复用户", "status", 1);
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post(BASE_PATH)
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.USERNAME_EXISTS.getCode()));
            } finally {
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + id);
            }
        }
    }

    @Nested
    @DisplayName("修改用户")
    class UpdateTest {

        @Test
        @DisplayName("版本号匹配，修改成功且变更可见")
        void shouldUpdateAndVisible_whenVersionMatch() {
            // Given（新建行乐观锁版本为 0；详情 VO 不含 version 字段）
            long id = createUser(Map.of());

            // When（update 不含 status 字段，状态变更统一走 /status 接口）
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("realName", "集成测试用户_已修改", "version", 0))
                .when().put(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then（姓名已改、状态保持新建时的启用）
            getDetail(id).then().body("data.realName", equalTo("集成测试用户_已修改"))
                .body("data.status", equalTo(1));
        }

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldReturnOptimisticLockConflict_whenVersionStale() {
            // Given
            long id = createUser(Map.of());
            Map<String, Object> body = Map.of("realName", "乐观锁冲突", "version", 999);

            // When / Then（乐观锁 10601 统一返回 HTTP 409）
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/" + id)
                .then().statusCode(409)
                .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("修改不存在的用户id，返回422用户不存在")
        void shouldReturn422UserNotFound_whenIdNotExist() {
            // Given
            Map<String, Object> body = Map.of("realName", "x", "version", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.USER_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("删除用户")
    class DeleteTest {

        @Test
        @DisplayName("删除存在的用户，删除后不可见")
        void shouldDeleteAndInvisible_whenUserExists() {
            // Given
            long id = createUser(Map.of());

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then：确认查不到
            getDetail(id).then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.USER_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("删除不存在的用户id，返回422用户不存在")
        void shouldReturn422UserNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.USER_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("启用停用用户")
    class ChangeStatusTest {

        @Test
        @DisplayName("停用自建用户，状态变更成功")
        void shouldDisableUser_whenStatusChangedOnSelfCreatedUser() {
            // Given（新建行乐观锁版本为 0；详情 VO 不含 version 字段）
            long id = createUser(Map.of());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", 0, "version", 0))
                .when().post(BASE_PATH + "/" + id + "/status")
                .then().statusCode(200).body("code", equalTo(200));
        }

        @Test
        @DisplayName("停用admin账号，返回422管理员保护")
        void shouldReturn422AdminProtected_whenDisableAdmin() {
            // Given（admin id 经分页检索确认；请求被拒，环境不变）
            Map<String, Object> body = Map.of("status", 0, "version", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH + "/" + adminId() + "/status")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ADMIN_PROTECTED.getCode()));
        }
    }

    @Nested
    @DisplayName("重置密码")
    class ResetPasswordTest {

        @Test
        @DisplayName("新密码符合复杂度，重置成功")
        void shouldResetPassword_whenNewPasswordValid() {
            // Given
            long id = createUser(Map.of());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("newPassword", "Reset@123456"))
                .when().post(BASE_PATH + "/" + id + "/reset-password")
                .then().statusCode(200).body("code", equalTo(200));
        }

        @Test
        @DisplayName("新密码过于简单，返回422密码复杂度不足")
        void shouldReturn422ComplexityError_whenNewPasswordTooSimple() {
            // Given
            long id = createUser(Map.of());
            Map<String, Object> body = Map.of("newPassword", "123");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH + "/" + id + "/reset-password")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.PASSWORD_COMPLEXITY_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("分配角色")
    class AssignRolesTest {

        @Test
        @DisplayName("分配空角色列表，清空用户角色")
        void shouldClearRoles_whenRoleIdsEmpty() {
            // Given
            long id = createUser(Map.of());

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("roleIds", List.of()))
                .when().put(BASE_PATH + "/" + id + "/roles")
                .then().statusCode(200).body("code", equalTo(200));
        }

        @Test
        @DisplayName("分配不存在的角色id，返回422角色不存在")
        void shouldReturn422RoleNotFound_whenRoleIdNotExist() {
            // Given
            long id = createUser(Map.of());
            Map<String, Object> body = Map.of("roleIds", List.of(999999999L));

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/" + id + "/roles")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.ROLE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("提权保护（18040）")
    class AssignAdminGuardTest {

        /** 递归收集指定 perms 的全部菜单 id（内置权限点按钮），无匹配返回空集 */
        @SuppressWarnings("unchecked")
        private List<Long> collectMenuIdsByPerms(List<Map<String, Object>> nodes, String perms) {
            List<Long> acc = new ArrayList<>();
            if (nodes == null) {
                return acc;
            }
            for (Map<String, Object> node : nodes) {
                if (perms.equals(node.get("perms"))) {
                    acc.add(Long.parseLong(String.valueOf(node.get("id"))));
                }
                acc.addAll(collectMenuIdsByPerms(
                    (List<Map<String, Object>>) node.get("children"), perms));
            }
            return acc;
        }

        @Test
        @DisplayName("非超管用户创建持有 ADMIN 角色的用户，返回422仅超管可分配（18040）")
        void shouldReturn422OnlySuperAdmin_whenNonSuperAdminAssignsAdmin() {
            // Given：创建含 system:user:create + system:user:assignrole 权限点的角色 + 持有该角色的普通用户
            List<Map<String, Object>> tree = given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .when().get("/api/system/menus/tree")
                .then().statusCode(200)
                .extract().jsonPath().getList("data");
            List<Long> menuIds = new ArrayList<>(collectMenuIdsByPerms(tree, "system:user:create"));
            menuIds.addAll(collectMenuIdsByPerms(tree, "system:user:assignrole"));
            org.assertj.core.api.Assertions.assertThat(menuIds)
                .as("应存在 system:user:create / system:user:assignrole 权限点菜单").isNotEmpty();

            String suffix = suffix();
            long roleId = given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("roleCode", "it_role_" + suffix, "roleName", "提权守卫角色_" + suffix, "status", 1))
                .when().post("/api/system/roles")
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.id");
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("menuIds", menuIds))
                .when().put("/api/system/roles/" + roleId + "/menus")
                .then().statusCode(200);

            String username = "it_guard_" + suffix;
            long userId = given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("userCode", "IT-GUARD-" + suffix, "username", username,
                    "password", LIMITED_PASSWORD, "realName", "提权守卫用户", "status", 1,
                    "roleIds", List.of(roleId)))
                .when().post(BASE_PATH)
                .then().statusCode(200)
                .extract().jsonPath().getLong("data");
            try {
                // Given：该普通用户登录；ADMIN 角色经分页检索（roleCode 为模糊匹配，需精确比对编码）
                String guardToken = doLogin(username, LIMITED_PASSWORD);
                List<Map<String, Object>> adminRoles = given().port(port).header("Authorization", "Bearer " + adminToken())
                    .queryParam("roleCode", "ADMIN").queryParam("pageSize", 10)
                    .when().get("/api/system/roles")
                    .then().statusCode(200)
                    .extract().jsonPath().getList("data.list");
                long adminRoleId = adminRoles.stream()
                    .filter(role -> "ADMIN".equals(role.get("roleCode")))
                    .findFirst().map(role -> Long.parseLong(String.valueOf(role.get("id"))))
                    .orElseThrow(() -> new IllegalStateException("测试库应存在内置 ADMIN 角色"));

                // When / Then：非超管创建持有 ADMIN 角色的用户被拒（请求体带齐必填字段，避免 400 先行）
                given().port(port).header("Authorization", "Bearer " + guardToken)
                    .contentType(ContentType.JSON)
                    .body(Map.of("userCode", "IT-ADMIN-" + suffix, "username", "it_admin_" + suffix,
                        "password", LIMITED_PASSWORD, "realName", "被提权用户", "status", 1,
                        "roleIds", List.of(adminRoleId)))
                    .when().post(BASE_PATH)
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.ONLY_SUPER_ADMIN_ASSIGN_ADMIN.getCode()));

                // When / Then：非超管给自己分配 ADMIN 角色同样被拒
                given().port(port).header("Authorization", "Bearer " + guardToken)
                    .contentType(ContentType.JSON)
                    .body(Map.of("roleIds", List.of(adminRoleId)))
                    .when().put(BASE_PATH + "/" + userId + "/roles")
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.ONLY_SUPER_ADMIN_ASSIGN_ADMIN.getCode()));
            } finally {
                // 清理：解绑角色 → 删用户 → 删角色
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(Map.of("roleIds", List.of()))
                    .when().put(BASE_PATH + "/" + userId + "/roles");
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + userId);
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete("/api/system/roles/" + roleId);
            }
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问用户列表，返回403禁止访问")
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
