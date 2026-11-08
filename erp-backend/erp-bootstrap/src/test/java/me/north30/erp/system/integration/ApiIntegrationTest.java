package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.north30.erp.common.constant.RedisKeyConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * API 集成测试基类（docs/phase3-develop-and-test/05 号文档）。
 * <p>铁律一：测试类不出现任何主机名/连接串——端口经 {@code @LocalServerPort} 注入，连接信息经 application-test.yml 注入。</p>
 * <p>铁律二：不假设测试库存在既有数据——所有用例数据自建自清（随机后缀），容器化后零成本过渡。</p>
 * <p>验证码绕过：登录校验强依赖 Redis（GETDEL），基类直接向 Redis 写入已知验证码再走真实登录接口，零后端改动。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
public abstract class ApiIntegrationTest {

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "Admin@123456";
    /** 低权限用户初始口令（满足口令复杂度 18009 校验） */
    protected static final String LIMITED_PASSWORD = "Limited@123456";

    /** 测试直写的验证码明文（CaptchaUtil 生成的为 4 位字母数字，自写值保持同量级） */
    private static final String TEST_CAPTCHA = "IT7A";

    @LocalServerPort
    protected int port;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    private static String adminToken;
    /** 随机端口静态镜像：供 @AfterAll 静态清理使用（首次发起请求时捕获） */
    private static int serverPort;

    private static String limitedToken;
    private static Long limitedUserId;
    private static Long limitedRoleId;
    /** 低权限用户名（类加载时生成一次，确保建用户与登录使用同一用户名） */
    private static final String LIMITED_USERNAME = "it_limited_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /** 非内置超管但持有 ADMIN 角色的操作者：is_admin=0，用于验证自我降权/自我清权守卫（18041） */
    private static String adminRoleOperatorToken;
    private static Long adminRoleOperatorUserId;
    /** 操作者持有的自定义角色（assignMenus 自我清权场景） */
    private static Long adminRoleOperatorRoleId;
    private static final String ADMIN_ROLE_OPERATOR_USERNAME =
        "it_roleop_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    /** 随机后缀（uuid hex 前 8 位）：临时数据自建自清，重复执行不冲突 */
    protected static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** admin token（惰性获取，全类复用） */
    protected String adminToken() {
        if (adminToken == null) {
            serverPort = port;
            adminToken = doLogin(ADMIN_USERNAME, ADMIN_PASSWORD);
        }
        return adminToken;
    }

    /** 低权限（无任何权限点）用户 token（惰性创建角色+用户并登录） */
    protected String limitedToken() {
        if (limitedToken == null) {
            ensureLimitedUser();
            limitedToken = doLogin(limitedUsername(), LIMITED_PASSWORD);
        }
        return limitedToken;
    }

    protected String limitedUsername() {
        return LIMITED_USERNAME;
    }

    private static String staticSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** 低权限用户清理 ID（供子类 @AfterAll 或断言使用） */
    protected static Long limitedUserId() {
        return limitedUserId;
    }

    protected static Long limitedRoleId() {
        return limitedRoleId;
    }

    /** 非内置超管但持有 ADMIN 角色的操作者 token（惰性创建，用于 18041 自我操作场景） */
    protected String adminRoleOperatorToken() {
        if (adminRoleOperatorToken == null) {
            ensureAdminRoleOperator();
            adminRoleOperatorToken = doLogin(ADMIN_ROLE_OPERATOR_USERNAME, LIMITED_PASSWORD);
        }
        return adminRoleOperatorToken;
    }

    protected static Long adminRoleOperatorUserId() {
        return adminRoleOperatorUserId;
    }

    /** 操作者持有的自定义角色 ID（assignMenus 自我清权断言使用） */
    protected static Long adminRoleOperatorRoleId() {
        return adminRoleOperatorRoleId;
    }

    /** 惰性创建：自定义角色 + 持有 ADMIN 角色与自定义角色的普通账号（is_admin=0） */
    private void ensureAdminRoleOperator() {
        if (adminRoleOperatorUserId != null) {
            return;
        }
        // 先取内置 ADMIN 角色 id（roleCode 为模糊匹配，需按精确编码过滤）
        List<Map<String, Object>> roles = given().port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("roleCode", "ADMIN").queryParam("pageNum", 1).queryParam("pageSize", 20)
            .when().get("/api/system/roles")
            .then().log().ifValidationFails().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getList("data.list");
        Long adminRoleId = roles.stream()
            .filter(role -> "ADMIN".equals(role.get("roleCode")))
            .map(role -> ((Number) role.get("id")).longValue())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("内置 ADMIN 角色不存在"));

        String suffix = staticSuffix();
        Map<String, Object> roleBody = new HashMap<>();
        roleBody.put("roleCode", "it_op_role_" + suffix);
        roleBody.put("roleName", "集成测试自我操作角色_" + suffix);
        adminRoleOperatorRoleId = given().port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON)
            .body(roleBody)
            .when().post("/api/system/roles")
            .then().log().ifValidationFails().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("userCode", "IT-OP-" + suffix);
        userBody.put("username", ADMIN_ROLE_OPERATOR_USERNAME);
        userBody.put("password", LIMITED_PASSWORD);
        userBody.put("realName", "集成测试自我操作账号");
        userBody.put("status", 1);
        userBody.put("roleIds", List.of(adminRoleId, adminRoleOperatorRoleId));
        adminRoleOperatorUserId = given().port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/system/users")
            .then().log().ifValidationFails().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data");
    }

    /** 通用登录：直写验证码到 Redis（key 与后端 CaptchaUtil 写入规则一致），再走真实登录接口 */
    protected String doLogin(String username, String password) {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue()
            .set(RedisKeyConstants.captchaKey(captchaKey), TEST_CAPTCHA, Duration.ofMinutes(5));
        Response response = given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(Map.of("username", username, "password", password, "captcha", TEST_CAPTCHA, "captchaKey", captchaKey))
            .when()
            .post("/api/auth/login")
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("code", equalTo(200))
            .extract().response();
        return response.jsonPath().getString("data.accessToken");
    }

    /** 惰性创建无权限角色 + 低权限用户（自建自清，@AfterAll 兜底清理） */
    private void ensureLimitedUser() {
        if (limitedUserId != null) {
            return;
        }
        String suffix = staticSuffix();
        Map<String, Object> roleBody = new HashMap<>();
        roleBody.put("roleCode", "it_ltd_role_" + suffix);
        roleBody.put("roleName", "集成测试低权限角色_" + suffix);
        limitedRoleId = given()
            .port(port)
            .header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON)
            .body(roleBody)
            .when().post("/api/system/roles")
            .then().log().ifValidationFails().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");

        Map<String, Object> userBody = new HashMap<>();
        userBody.put("userCode", "IT-LTD-" + suffix);
        userBody.put("username", limitedUsername());
        userBody.put("password", LIMITED_PASSWORD);
        userBody.put("realName", "集成测试低权限用户");
        userBody.put("status", 1);
        userBody.put("roleIds", List.of(limitedRoleId));
        limitedUserId = given()
            .port(port)
            .header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON)
            .body(userBody)
            .when().post("/api/system/users")
            .then().log().ifValidationFails().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data");
    }

    /** 类级清理：解绑角色后删除低权限用户与角色（清理失败让测试变红；先清 roleIds 避免 18013，先删用户再删角色避免 18016） */
    @AfterAll
    static void cleanupLimitedUser() {
        if (adminToken == null) {
            return;
        }
        // 立即失效 token 缓存：@AfterAll 会随各嵌套类多次执行，避免后续用例复用已删除用户的旧 token
        limitedToken = null;
        if (limitedUserId != null) {
            given().port(serverPort).header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("roleIds", List.of()))
                .when().put("/api/system/users/" + limitedUserId + "/roles")
                .then().log().ifValidationFails().statusCode(200);
            given().port(serverPort).header("Authorization", "Bearer " + adminToken)
                .when().delete("/api/system/users/" + limitedUserId)
                .then().log().ifValidationFails().statusCode(200);
            limitedUserId = null;
        }
        if (limitedRoleId != null) {
            given().port(serverPort).header("Authorization", "Bearer " + adminToken)
                .when().delete("/api/system/roles/" + limitedRoleId)
                .then().log().ifValidationFails().statusCode(200);
            limitedRoleId = null;
        }
        // 自我操作账号清理：先解绑全部角色（含 ADMIN）再删用户，最后删自定义角色
        adminRoleOperatorToken = null;
        if (adminRoleOperatorUserId != null) {
            given().port(serverPort).header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("roleIds", List.of()))
                .when().put("/api/system/users/" + adminRoleOperatorUserId + "/roles")
                .then().log().ifValidationFails().statusCode(200);
            given().port(serverPort).header("Authorization", "Bearer " + adminToken)
                .when().delete("/api/system/users/" + adminRoleOperatorUserId)
                .then().log().ifValidationFails().statusCode(200);
            adminRoleOperatorUserId = null;
        }
        if (adminRoleOperatorRoleId != null) {
            given().port(serverPort).header("Authorization", "Bearer " + adminToken)
                .when().delete("/api/system/roles/" + adminRoleOperatorRoleId)
                .then().log().ifValidationFails().statusCode(200);
            adminRoleOperatorRoleId = null;
        }
    }
}
