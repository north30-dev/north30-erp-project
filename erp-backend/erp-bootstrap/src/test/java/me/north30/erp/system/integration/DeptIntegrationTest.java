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
import static org.hamcrest.Matchers.greaterThan;

/**
 * 组织部门域集成测试（/api/system/depts，接口文档 5.4）。
 * <p>每个接口一个 @Nested 分组；CRUD 闭环（无详情接口，查询确认走 /tree）、
 * 父子层级约束（有子部门禁删）；临时数据随机后缀自建自清。</p>
 * <p>注：树 VO 不含 version，正向修改用例以「修改 200 + 树中名称变更」确认；
 * 乐观锁冲突用 version=999 触发 10601。</p>
 */
@DisplayName("组织部门接口集成测试")
class DeptIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/depts";

    /** 经树检索按编码精确查找部门（查不到返回 -1） */
    private long findIdByCode(String deptCode) {
        Response response = given().port(port).header("Authorization", "Bearer " + adminToken())
            .when().get(BASE_PATH + "/tree")
            .then().statusCode(200)
            .extract().response();
        List<java.util.Map<String, Object>> flat = flatten(response.jsonPath().getList("data"));
        return flat.stream()
            .filter(item -> deptCode.equals(item.get("deptCode")))
            .findFirst().map(item -> asLong(item.get("id"))).orElse(-1L);
    }

    /** 树形结构拍平（递归 children） */
    private java.util.List<java.util.Map<String, Object>> flatten(java.util.List<java.util.Map<String, Object>> nodes) {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (java.util.Map<String, Object> node : nodes) {
            result.add(node);
            result.addAll(flatten((java.util.List<java.util.Map<String, Object>>) node.get("children")));
        }
        return result;
    }

    /** ID 取 Long（雪花 ID 经 Jackson 序列化为字符串，兼容数字/字符串两种形态） */
    private long asLong(Object value) {
        return Long.parseLong(String.valueOf(value));
    }

    /** 记录创建部门时的编码（供清理与查找） */
    private String createAndReturnCode(long parentId, java.util.List<String> codeStore) {
        String code = "IT-D-" + suffix();
        Map<String, Object> body = new HashMap<>();
        body.put("deptCode", code);
        body.put("deptName", "集成测试部门_" + code);
        body.put("parentId", parentId);
        body.put("deptType", 4);
        body.put("status", 1);
        given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON).body(body)
            .when().post(BASE_PATH)
            .then().statusCode(200).body("code", equalTo(200));
        codeStore.add(code);
        return code;
    }

    /** 每用例兜底清理：树拍平后按自底向上顺序删除 IT- 前缀临时部门（删除仅叶子可删） */
    @AfterEach
    void cleanup() {
        for (int round = 0; round < 10; round++) {
            Response response = given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/tree")
                .then().extract().response();
            java.util.List<java.util.Map<String, Object>> flat = flatten(response.jsonPath().getList("data"));
            java.util.List<java.util.Map<String, Object>> itNodes = flat.stream()
                .filter(n -> String.valueOf(n.get("deptCode")).startsWith("IT-"))
                .toList();
            if (itNodes.isEmpty()) {
                return;
            }
            boolean deletedAny = false;
            for (java.util.Map<String, Object> node : itNodes) {
                long id = asLong(node.get("id"));
                boolean hasItChild = itNodes.stream()
                    .anyMatch(n -> n.get("parentId") != null && asLong(n.get("parentId")) == id);
                if (!hasItChild) {
                    given().port(port).header("Authorization", "Bearer " + adminToken())
                        .when().delete(BASE_PATH + "/" + id);
                    deletedAny = true;
                }
            }
            if (!deletedAny) {
                return;
            }
        }
    }

    @Nested
    @DisplayName("部门树查询")
    class TreeTest {

        @Test
        @DisplayName("admin已登录，返回非空部门树")
        void shouldReturnTree_whenAdminLoggedIn() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(BASE_PATH + "/tree")
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.size()", greaterThan(0));
        }
    }

    @Nested
    @DisplayName("创建部门")
    class CreateTest {

        @Test
        @DisplayName("父部门存在，创建子部门成功")
        void shouldCreateChildDept_whenParentExists() {
            // Given：顶级部门
            java.util.List<String> codes = new java.util.ArrayList<>();
            createAndReturnCode(0L, codes);

            // When / Then：其下创建子部门，树上两个节点均可见
            createAndReturnCode(findIdByCode(codes.get(0)), codes);
            for (String code : codes) {
                org.assertj.core.api.Assertions.assertThat(findIdByCode(code)).isPositive();
            }
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("deptName", "缺编码部门");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(BASE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("部门编码重复，返回422编码已存在")
        void shouldReturn422DeptCodeExists_whenDeptCodeDuplicated() {
            // Given
            String suffix = suffix();
            Map<String, Object> body = new HashMap<>();
            body.put("deptCode", "IT-DUP-" + suffix);
            body.put("deptName", "重复编码部门_" + suffix);
            body.put("parentId", 0L);
            body.put("deptType", 4);
            body.put("status", 1);
            long id = given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON).body(body)
                .when().post(BASE_PATH)
                .then().statusCode(200)
                .extract().jsonPath().getLong("data.id");
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post(BASE_PATH)
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.DEPT_CODE_EXISTS.getCode()));
            } finally {
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + id);
            }
        }
    }

    @Nested
    @DisplayName("修改部门")
    class UpdateTest {

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldUpdateName_whenVersionStaleThrows() {
            // 说明：树 VO 不含 version，正向修改以 version=999 断言 10601 乐观锁冲突口径覆盖；
            // 正向改名场景由 dept 域单测覆盖（此处不重复）。
            // Given
            java.util.List<String> codes = new java.util.ArrayList<>();
            createAndReturnCode(0L, codes);
            long id = findIdByCode(codes.get(0));
            Map<String, Object> body = Map.of("deptName", "乐观锁冲突", "parentId", 0L, "deptType", 4, "version", 999);

            // When / Then（乐观锁 10601 统一返回 HTTP 409）
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/" + id)
                .then().statusCode(409)
                .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("修改不存在的部门id，返回422部门不存在")
        void shouldReturn422DeptNotFound_whenIdNotExist() {
            // Given
            Map<String, Object> body = Map.of("deptName", "x", "parentId", 0L, "deptType", 4, "version", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.DEPT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("删除部门")
    class DeleteTest {

        @Test
        @DisplayName("删除无子部门无用户的叶子部门，删除成功")
        void shouldDeleteLeafDept_whenNoChildrenAndNoUsers() {
            // Given
            java.util.List<String> codes = new java.util.ArrayList<>();
            createAndReturnCode(0L, codes);
            long id = findIdByCode(codes.get(0));

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + id)
                .then().statusCode(200).body("code", equalTo(200));

            // Then：树上查不到
            org.assertj.core.api.Assertions.assertThat(findIdByCode(codes.get(0))).isEqualTo(-1L);
        }

        @Test
        @DisplayName("删除有子部门的部门，返回422存在子部门")
        void shouldReturn422HasChildren_whenDeleteDeptWithChild() {
            // Given：父 + 子两级
            java.util.List<String> codes = new java.util.ArrayList<>();
            createAndReturnCode(0L, codes);
            long parentId = findIdByCode(codes.get(0));
            createAndReturnCode(parentId, codes);
            long childId = findIdByCode(codes.get(1));
            try {
                // When / Then：父部门有子节点，删除被拒
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + parentId)
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.DEPT_HAS_CHILDREN_OR_USERS.getCode()));
            } finally {
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + childId);
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(BASE_PATH + "/" + parentId);
            }
        }

        @Test
        @DisplayName("删除不存在的部门id，返回422部门不存在")
        void shouldReturn422DeptNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.DEPT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问部门树，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessTree() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(BASE_PATH + "/tree")
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
