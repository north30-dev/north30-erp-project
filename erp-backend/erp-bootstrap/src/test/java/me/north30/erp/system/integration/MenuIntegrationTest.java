package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 菜单管理域集成测试（/api/system/menus，接口文档 5.3）。
 * <p>每个接口一个 @Nested 分组；正向 CRUD 闭环 + 负向（父子类型配对/格式非法/深度超限/防环/perms 重复/乐观锁冲突）；
 * 临时数据随机后缀自建自清，@AfterEach 按 it_menu_ 前缀沿树兜底清理。</p>
 */
@DisplayName("菜单管理接口集成测试")
class MenuIntegrationTest extends ApiIntegrationTest {

    private static final String BASE_PATH = "/api/system/menus";

    /** 创建临时菜单（随机后缀），可按 overrides 覆盖字段，返回 id */
    private long createMenu(Map<String, Object> overrides) {
        String suffix = suffix();
        Map<String, Object> body = new HashMap<>();
        body.put("menuName", "it_menu_" + suffix);
        body.put("parentId", 0L);
        body.put("menuType", 1);
        body.put("path", "/it-" + suffix);
        body.putAll(overrides);
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON).body(body)
            .when().post(BASE_PATH)
            .then().log().ifValidationFails().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");
    }

    /** 创建按钮请求体字段（perms 必填且需唯一） */
    private Map<String, Object> buttonOverrides(long parentId, String perms) {
        Map<String, Object> body = new HashMap<>();
        body.put("menuName", "it_menu_" + suffix());
        body.put("menuType", 3);
        body.put("parentId", parentId);
        body.put("perms", perms);
        return body;
    }

    /** 创建页面（菜单）请求体字段（path/component 必填） */
    private Map<String, Object> pageOverrides(long parentId, String component) {
        Map<String, Object> body = new HashMap<>();
        body.put("menuName", "it_menu_" + suffix());
        body.put("menuType", 2);
        body.put("parentId", parentId);
        body.put("path", "/it-page-" + suffix());
        body.put("component", component);
        return body;
    }

    /** 查询完整菜单树（返回响应体便于按 id 取 version） */
    private Response getTree() {
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .when().get(BASE_PATH + "/tree")
            .then().statusCode(200)
            .extract().response();
    }

    @AfterEach
    void cleanup() {
        // 兜底清理：按 it_menu_ 前缀沿树递归删除（先子后父，避免 18020）
        List<Map<String, Object>> roots = getTree().jsonPath().getList("data");
        if (roots != null) {
            roots.forEach(this::deleteIfPrefixed);
        }
    }

    @SuppressWarnings("unchecked")
    private void deleteIfPrefixed(Map<String, Object> node) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null) {
            children.forEach(this::deleteIfPrefixed);
        }
        if (String.valueOf(node.get("menuName")).startsWith("it_menu_")) {
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + node.get("id"))
                .then().statusCode(200);
        }
    }

    @Nested
    @DisplayName("新增菜单")
    class CreateTest {

        @Test
        @DisplayName("顶级创建目录与菜单、目录下建菜单、菜单下建按钮（三级链）成功")
        void shouldCreateMenus_whenParentChildTypePairingValid() {
            // Given：顶级目录 + 顶级菜单
            long dirId = createMenu(Map.of());
            createMenu(pageOverrides(0L, "system/role/index"));

            // When / Then：目录下建菜单、菜单下建按钮（三级全链）
            long pageId = createMenu(pageOverrides(dirId, "system/user/index"));
            long buttonId = createMenu(buttonOverrides(pageId, "it:test:create" + suffix()));
            assertThat(pageId).isPositive();
            assertThat(buttonId).isPositive();
        }

        @Test
        @DisplayName("大驼峰 component 创建成功（与前端页面组件命名规范一致）")
        void shouldCreateMenu_whenComponentCamelCase() {
            // When / Then：component 允许大驼峰段
            long pageId = createMenu(pageOverrides(0L, "system/loginLog/index"));
            assertThat(pageId).isPositive();
        }

        @Test
        @DisplayName("顶级创建按钮，返回422参数错误")
        void shouldReturn422ParamError_whenCreateButtonAtRoot() {
            // When / Then：顶级仅支持目录/菜单
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(buttonOverrides(0L, "system:it:create"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("目录下创建按钮，返回422参数错误")
        void shouldReturn422ParamError_whenCreateButtonUnderDir() {
            // Given
            long dirId = createMenu(Map.of());

            // When / Then：按钮仅可挂菜单下
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(buttonOverrides(dirId, "system:it:create"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("菜单下创建目录（类型倒挂），返回422参数错误")
        void shouldReturn422ParamError_whenCreateDirUnderMenu() {
            // Given
            long pageId = createMenu(pageOverrides(0L, "system/user/index"));

            // When / Then：menuType=1 目录挂在菜单下
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("menuName", "it_menu_" + suffix(), "parentId", pageId,
                    "menuType", 1, "path", "/it-" + suffix()))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("按钮下创建子节点，返回422层级超限（18023）")
        void shouldReturn422LevelExceeded_whenCreateChildUnderButton() {
            // Given：菜单下建按钮
            String suffix = suffix();
            long pageId = createMenu(pageOverrides(0L, "system/user/index"));
            long buttonId = createMenu(buttonOverrides(pageId, "it:test:create" + suffix));

            // When / Then：按钮下不可再建子（按钮守卫与深度上限共用 18023）
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(pageOverrides(buttonId, "system/user/index"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()));
        }

        @Test
        @DisplayName("第4层菜单下再建子节点构成5层，返回422层级超限（18023）")
        void shouldReturn422LevelExceeded_whenDepthOverFourLevels() {
            // Given：目录(1) → 目录(2) → 菜单(3) → 菜单(4)
            long l1 = createMenu(Map.of());
            long l2 = createMenu(Map.of("parentId", l1, "path", "/it-l2-" + suffix()));
            long l3 = createMenu(pageOverrides(l2, "system/user/index"));
            long l4 = createMenu(pageOverrides(l3, "system/role/index"));

            // When / Then：第4层菜单下再建菜单 → 深度 5 > 4
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(pageOverrides(l4, "system/user/index"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()));
        }

        @Test
        @DisplayName("path 缺少开头斜杠，返回422参数错误")
        void shouldReturn422ParamError_whenPathWithoutLeadingSlash() {
            // When / Then
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("menuName", "it_menu_" + suffix(), "parentId", 0,
                    "menuType", 1, "path", "system/menu"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("component 以 .vue 结尾，返回422参数错误")
        void shouldReturn422ParamError_whenComponentWithVueSuffix() {
            // When / Then
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(pageOverrides(0L, "system/user.vue"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("perms 两段式，返回422参数错误")
        void shouldReturn422ParamError_whenPermsTwoSegments() {
            // When / Then
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(buttonOverrides(0L, "system:create"))
                .when().post(BASE_PATH)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("菜单树查询")
    class TreeTest {

        @Test
        @DisplayName("新建三级链后查询树，返回含新建节点及 version 字段")
        void shouldReturnTreeContainingCreatedNodes_whenNodesExist() {
            // Given：顶级目录 → 页面 → 按钮
            long dirId = createMenu(Map.of());
            long pageId = createMenu(pageOverrides(dirId, "system/user/index"));
            createMenu(buttonOverrides(pageId, "it:test:create" + suffix()));

            // When / Then：树查询含新建目录（按 id 查找，规避各节点随机后缀不同），且携带乐观锁 version
            Map<String, Object> node = findNodeById(getTree().jsonPath().getList("data"), dirId);
            assertThat(node).isNotNull();
            assertThat(String.valueOf(node.get("id"))).isEqualTo(String.valueOf(dirId));
            assertThat(node.get("version")).isNotNull();
        }
    }

    @Nested
    @DisplayName("修改菜单")
    class UpdateTest {

        @Test
        @DisplayName("更新菜单名称与排序，返回更新时间")
        void shouldReturnUpdateTime_whenUpdateWithValidVersion() {
            // Given
            long dirId = createMenu(Map.of());
            int version = nodeVersion(dirId);

            // When / Then
            Map<String, Object> body = new HashMap<>();
            body.put("menuName", "it_menu_改名_" + suffix());
            body.put("menuSort", 9);
            body.put("version", version);
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON).body(body)
                .when().put(BASE_PATH + "/" + dirId)
                .then().log().ifValidationFails().statusCode(200)
                .body("code", equalTo(200))
                .body("data.updateTime", notNullValue());
        }

        @Test
        @DisplayName("version 过期（乐观锁冲突），返回409")
        void shouldReturn409Conflict_whenVersionStale() {
            // Given
            long dirId = createMenu(Map.of());

            // When / Then
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("menuName", "it_menu_版本冲突_" + suffix(), "version", 999))
                .when().put(BASE_PATH + "/" + dirId)
                .then().log().ifValidationFails().statusCode(409)
                .body("code", equalTo(10601));
        }

        @Test
        @DisplayName("有子节点的菜单修改类型，返回422参数错误")
        void shouldReturn422ParamError_whenChangeTypeWithChildren() {
            // Given：目录 + 子页面
            long dirId = createMenu(Map.of());
            createMenu(pageOverrides(dirId, "system/user/index"));

            // When / Then：改类型 → 10001
            Map<String, Object> body = new HashMap<>();
            body.put("menuType", 2);
            body.put("path", "/it-type-" + suffix());
            body.put("component", "system/user/index");
            body.put("version", nodeVersion(dirId));
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON).body(body)
                .when().put(BASE_PATH + "/" + dirId)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("换父级指向自身下级（防环），返回422参数错误")
        void shouldReturn422ParamError_whenMoveUnderOwnDescendant() {
            // Given：目录 → 子目录
            long dirId = createMenu(Map.of());
            long childId = createMenu(Map.of("parentId", dirId, "path", "/it-child-" + suffix()));

            // When / Then：父级指向自身子节点
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("parentId", childId, "version", nodeVersion(dirId)))
                .when().put(BASE_PATH + "/" + dirId)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("删除菜单")
    class DeleteTest {

        @Test
        @DisplayName("存在子节点的菜单删除，返回422（18020）")
        void shouldReturn422HasChildren_whenDeleteWithChildren() {
            // Given：目录 + 子页面
            long dirId = createMenu(Map.of());
            createMenu(pageOverrides(dirId, "system/user/index"));

            // When / Then
            given()
                .port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + dirId)
                .then().log().ifValidationFails().statusCode(422)
                .body("code", equalTo(SystemErrorCode.MENU_HAS_CHILDREN.getCode()));
        }

        @Test
        @DisplayName("自底向上删除三级链，全部成功")
        void shouldDeleteChainFromLeafToRoot_whenNoChildrenLeft() {
            // Given：目录 → 页面 → 按钮
            long dirId = createMenu(Map.of());
            long pageId = createMenu(pageOverrides(dirId, "system/user/index"));
            long buttonId = createMenu(buttonOverrides(pageId, "it:test:create" + suffix()));

            // When / Then：先按钮 → 页面 → 目录
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + buttonId)
                .then().statusCode(200).body("code", equalTo(200));
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + pageId)
                .then().statusCode(200).body("code", equalTo(200));
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(BASE_PATH + "/" + dirId)
                .then().statusCode(200).body("code", equalTo(200));
        }
    }

    @Nested
    @DisplayName("鉴权")
    class AuthTest {

        @Test
        @DisplayName("无 token 访问菜单树，返回401")
        void shouldReturn401_whenNoToken() {
            // When / Then
            given().port(port)
                .when().get(BASE_PATH + "/tree")
                .then().statusCode(401);
        }

        @Test
        @DisplayName("伪造 token 访问菜单树，返回401")
        void shouldReturn401_whenForgedToken() {
            // When / Then
            given().port(port).header("Authorization", "Bearer forged-token")
                .when().get(BASE_PATH + "/tree")
                .then().statusCode(401);
        }

        @Test
        @DisplayName("低权限用户新增菜单，返回403")
        void shouldReturn403_whenLimitedUserCreates() {
            // Given：低权限角色无 system:menu:create 权限点
            String token = limitedToken();

            // When / Then：请求体合法（否则 400 参数校验先于 403 权限拦截）
            given()
                .port(port).header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("menuName", "it_menu_" + suffix(), "parentId", 0,
                    "menuType", 1, "path", "/it-" + suffix()))
                .when().post(BASE_PATH)
                .then().statusCode(403);
        }
    }

    /** 按 id 查询节点当前乐观锁 version（树查询遍历，JSON 数值字段统一按字符串解析） */
    private int nodeVersion(long id) {
        Map<String, Object> node = findNodeById(getTree().jsonPath().getList("data"), id);
        assertThat(node).as("树中应存在菜单 id=" + id).isNotNull();
        return Integer.parseInt(String.valueOf(node.get("version")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, long id) {
        if (nodes == null) {
            return null;
        }
        for (Map<String, Object> node : nodes) {
            if (Long.parseLong(String.valueOf(node.get("id"))) == id) {
                return node;
            }
            Map<String, Object> inChildren = findNodeById(
                (List<Map<String, Object>>) node.get("children"), id);
            if (inChildren != null) {
                return inChildren;
            }
        }
        return null;
    }
}
