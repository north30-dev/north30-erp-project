package me.north30.erp.system.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * 字典域集成测试（/api/system/dict-types、/api/system/dict-items，接口文档 5.5）。
 * <p>每个接口一个 @Nested 分组；类型+项两级、删除保护（类型下有项禁删）；
 * 分页 VO 不含 version，修改用例以乐观锁冲突（version=999）与不存在 ID 负向口径覆盖。</p>
 */
@DisplayName("数据字典接口集成测试")
class DictIntegrationTest extends ApiIntegrationTest {

    private static final String TYPE_PATH = "/api/system/dict-types";
    private static final String ITEM_PATH = "/api/system/dict-items";

    /** 每用例兜底清理：先删 it_dict_type_ 前缀类型下的项，再删类型（自建自清） */
    @AfterEach
    void cleanup() {
        List<Map<String, Object>> list = given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("dictType", "it_dict_type_").queryParam("pageSize", 200)
            .when().get(TYPE_PATH)
            .then().extract().jsonPath().getList("data.list");
        if (list != null) {
            for (Map<String, Object> type : list) {
                String dictType = String.valueOf(type.get("dictType"));
                cleanupItems(dictType);
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.get("id"));
            }
        }
    }

    /** 创建临时字典类型（随机后缀编码），返回 dictType 编码与 id */
    private record CreatedType(String dictType, long id) {
    }

    private CreatedType createType() {
        String dictType = "it_dict_type_" + suffix();
        long id = given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON)
            .body(Map.of("dictType", dictType, "dictName", "集成测试字典类型_" + dictType, "status", 1))
            .when().post(TYPE_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");
        return new CreatedType(dictType, id);
    }

    /** 创建临时字典项，返回 id */
    private long createItem(String dictType, String itemValue) {
        return given()
            .port(port).header("Authorization", "Bearer " + adminToken())
            .contentType(ContentType.JSON)
            .body(Map.of("dictType", dictType, "itemLabel", "集成测试字典项_" + itemValue,
                "itemValue", itemValue, "lang", "zh-CN", "status", 1))
            .when().post(ITEM_PATH)
            .then().statusCode(200).body("code", equalTo(200))
            .extract().jsonPath().getLong("data.id");
    }

    /** 按类型编码删除该类型下全部字典项（兜底清理） */
    private void cleanupItems(String dictType) {
        Response response = given().port(port).header("Authorization", "Bearer " + adminToken())
            .queryParam("dictType", dictType).queryParam("pageSize", 200)
            .when().get(ITEM_PATH)
            .then().extract().response();
        List<Map<String, Object>> list = response.jsonPath().getList("data.list");
        if (list != null) {
            for (Map<String, Object> item : list) {
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(ITEM_PATH + "/" + item.get("id"))
                    .then().statusCode(200);
            }
        }
    }

    @Nested
    @DisplayName("字典类型分页查询")
    class TypePageTest {

        @Test
        @DisplayName("按类型编码过滤，返回分页结果")
        void shouldReturnPageWithFilteredResult_whenDictTypeFilterPresent() {
            // Given
            CreatedType type = createType();

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("dictType", type.dictType()).queryParam("pageNum", 1).queryParam("pageSize", 10)
                .when().get(TYPE_PATH)
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
                .when().get(TYPE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("创建字典类型")
    class TypeCreateTest {

        @Test
        @DisplayName("创建字典类型成功")
        void shouldCreateType_whenBodyValid() {
            // When / Then
            createType();
        }

        @Test
        @DisplayName("类型编码重复，返回422字典类型已存在")
        void shouldReturn422DictTypeExists_whenDictTypeDuplicated() {
            // Given
            CreatedType type = createType();
            Map<String, Object> body = Map.of("dictType", type.dictType(), "dictName", "重复类型", "status", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(TYPE_PATH)
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.DICT_TYPE_EXISTS.getCode()));
        }

        @Test
        @DisplayName("类型编码格式非法，返回400参数错误")
        void shouldReturn400ParamError_whenDictTypeFormatInvalid() {
            // Given：大写字母开头违反 @Pattern
            Map<String, Object> body = Map.of("dictType", "Bad_Type", "dictName", "非法编码", "status", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(TYPE_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("修改字典类型")
    class TypeUpdateTest {

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldReturn422OptimisticLockConflict_whenVersionStale() {
            // Given（分页 VO 不含 version，用 version=999 触发冲突）
            CreatedType type = createType();
            Map<String, Object> body = Map.of("dictName", "乐观锁冲突", "version", 999);

            // When / Then（乐观锁 10601 统一返回 HTTP 409）
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(TYPE_PATH + "/" + type.id())
                .then().statusCode(409)
                .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("修改不存在的类型id，返回422字典类型不存在")
        void shouldReturn422DictTypeNotFound_whenIdNotExist() {
            // Given
            Map<String, Object> body = Map.of("dictName", "x", "version", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(TYPE_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("删除字典类型")
    class TypeDeleteTest {

        @Test
        @DisplayName("删除无字典项的类型，删除成功")
        void shouldDeleteType_whenNoItems() {
            // Given
            CreatedType type = createType();

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(TYPE_PATH + "/" + type.id())
                .then().statusCode(200).body("code", equalTo(200));

            // Then：分页查不到
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("dictType", type.dictType()).queryParam("pageSize", 10)
                .when().get(TYPE_PATH)
                .then().statusCode(200)
                .body("data.total", equalTo(0));
        }

        @Test
        @DisplayName("删除有字典项的类型，返回422被引用禁删")
        void shouldReturn422ItemReferenced_whenTypeHasItems() {
            // Given：类型 + 字典项
            CreatedType type = createType();
            createItem(type.dictType(), "V1");
            try {
                // When / Then：类型下有字典项，删除被拒
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.id())
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.DICT_ITEM_REFERENCED.getCode()));
            } finally {
                cleanupItems(type.dictType());
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.id());
            }
        }
    }

    @Nested
    @DisplayName("字典项查询")
    class ItemQueryTest {

        @Test
        @DisplayName("按类型编码过滤，返回字典项列表")
        void shouldReturnItems_whenDictTypeFilterPresent() {
            // Given
            CreatedType type = createType();
            createItem(type.dictType(), "V1");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("dictType", type.dictType()).queryParam("pageSize", 10)
                .when().get(ITEM_PATH)
                .then().statusCode(200)
                .body("code", equalTo(200))
                .body("data.total", greaterThanOrEqualTo(1));
        }

        @Test
        @DisplayName("缺少dictType参数，返回422参数错误")
        void shouldReturn10001ParamError_whenDictTypeMissing() {
            // When / Then（dictType 非注解必填，由 Service 校验，业务异常统一 422 + body.code=10001）
            Response response = given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().get(ITEM_PATH)
                .then().statusCode(422)
                .extract().response();
            assertThat(response.jsonPath().getInt("code")).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("创建字典项")
    class ItemCreateTest {

        @Test
        @DisplayName("类型编码不存在，返回422字典类型不存在")
        void shouldReturn422DictTypeNotFound_whenDictTypeNotExist() {
            // Given
            Map<String, Object> body = Map.of("dictType", "no_such_type_" + suffix(),
                "itemLabel", "x", "itemValue", "V1", "lang", "zh-CN", "status", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(ITEM_PATH)
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("同类型下项值重复，返回422字典项已存在")
        void shouldReturn422DictItemExists_whenItemValueDuplicated() {
            // Given
            CreatedType type = createType();
            createItem(type.dictType(), "V1");
            Map<String, Object> body = Map.of("dictType", type.dictType(),
                "itemLabel", "重复项", "itemValue", "V1", "lang", "zh-CN", "status", 1);
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post(ITEM_PATH)
                    .then().statusCode(422)
                    .body("code", equalTo(SystemErrorCode.DICT_ITEM_EXISTS.getCode()));
            } finally {
                cleanupItems(type.dictType());
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.id());
            }
        }

        @Test
        @DisplayName("缺少必填字段，返回400参数错误")
        void shouldReturn400ParamError_whenRequiredFieldsMissing() {
            // Given
            Map<String, Object> body = Map.of("dictType", "some_type_" + suffix(), "itemLabel", "缺 itemValue");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post(ITEM_PATH)
                .then().statusCode(400)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }
    }

    @Nested
    @DisplayName("修改字典项")
    class ItemUpdateTest {

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldReturn422OptimisticLockConflict_whenVersionStale() {
            // Given（分页 VO 不含 version，用 version=999 触发冲突）
            CreatedType type = createType();
            long itemId = createItem(type.dictType(), "V1");
            Map<String, Object> body = Map.of("itemLabel", "乐观锁冲突", "version", 999);
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().put(ITEM_PATH + "/" + itemId)
                    .then().statusCode(409)
                    .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
            } finally {
                cleanupItems(type.dictType());
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.id());
            }
        }

        @Test
        @DisplayName("修改不存在的字典项id，返回422资源不存在")
        void shouldReturn422NotFound_whenIdNotExist() {
            // Given
            Map<String, Object> body = Map.of("itemLabel", "x", "version", 1);

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().put(ITEM_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("删除字典项")
    class ItemDeleteTest {

        @Test
        @DisplayName("删除存在的字典项，删除后不可见")
        void shouldDeleteItem_whenItemExists() {
            // Given
            CreatedType type = createType();
            long itemId = createItem(type.dictType(), "V1");

            // When
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(ITEM_PATH + "/" + itemId)
                .then().statusCode(200).body("code", equalTo(200));

            // Then：确认查不到
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("dictType", type.dictType()).queryParam("pageSize", 10)
                .when().get(ITEM_PATH)
                .then().statusCode(200)
                .body("data.total", equalTo(0));

            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(TYPE_PATH + "/" + type.id());
        }

        @Test
        @DisplayName("删除不存在的字典项id，返回422资源不存在")
        void shouldReturn422NotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .when().delete(ITEM_PATH + "/999999999")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("变更字典类型状态")
    class TypeChangeStatusTest {

        @Test
        @DisplayName("停用后重新启用自建类型，返回变更后状态")
        void shouldToggleStatus_whenTypeCreated() {
            // Given（新建行乐观锁版本为 0）
            CreatedType type = createType();

            // When / Then：停用
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", 0, "version", 0))
                .when().post(TYPE_PATH + "/" + type.id() + "/status")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.status", equalTo(0));

            // When / Then：启用
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", 1, "version", 1))
                .when().post(TYPE_PATH + "/" + type.id() + "/status")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.status", equalTo(1));
        }

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldReturn409Conflict_whenVersionStale() {
            // Given
            CreatedType type = createType();

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", 0, "version", 999))
                .when().post(TYPE_PATH + "/" + type.id() + "/status")
                .then().statusCode(409)
                .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
        }

        @Test
        @DisplayName("停用不存在的类型id，返回422字典类型不存在")
        void shouldReturn422DictTypeNotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", 0, "version", 1))
                .when().post(TYPE_PATH + "/999999999/status")
                .then().statusCode(422)
                .body("code", equalTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("变更字典项状态")
    class ItemChangeStatusTest {

        @Test
        @DisplayName("停用后重新启用自建字典项，返回变更后状态")
        void shouldToggleStatus_whenItemCreated() {
            // Given
            CreatedType type = createType();
            long itemId = createItem(type.dictType(), "V1");
            try {
                // When / Then：停用
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(Map.of("status", 0, "version", 0))
                    .when().post(ITEM_PATH + "/" + itemId + "/status")
                    .then().statusCode(200).body("code", equalTo(200))
                    .body("data.status", equalTo(0));

                // When / Then：启用
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(Map.of("status", 1, "version", 1))
                    .when().post(ITEM_PATH + "/" + itemId + "/status")
                    .then().statusCode(200).body("code", equalTo(200))
                    .body("data.status", equalTo(1));
            } finally {
                cleanupItems(type.dictType());
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.id());
            }
        }

        @Test
        @DisplayName("版本号过期，返回409乐观锁冲突")
        void shouldReturn409Conflict_whenVersionStale() {
            // Given
            CreatedType type = createType();
            long itemId = createItem(type.dictType(), "V1");
            try {
                // When / Then
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .contentType(ContentType.JSON)
                    .body(Map.of("status", 0, "version", 999))
                    .when().post(ITEM_PATH + "/" + itemId + "/status")
                    .then().statusCode(409)
                    .body("code", equalTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()));
            } finally {
                cleanupItems(type.dictType());
                given().port(port).header("Authorization", "Bearer " + adminToken())
                    .when().delete(TYPE_PATH + "/" + type.id());
            }
        }

        @Test
        @DisplayName("停用不存在的字典项id，返回422资源不存在")
        void shouldReturn422NotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", 0, "version", 1))
                .when().post(ITEM_PATH + "/999999999/status")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("字典项上移/下移")
    class ItemMoveTest {

        @Test
        @DisplayName("上移成功，同分组内顺序交换")
        void shouldMoveUpAndSwapOrder_whenMoveUp() {
            // Given：同类型下创建两项，创建顺序 A→B（B sort 为 max+10 排在 A 之后）
            CreatedType type = createType();
            long idA = createItem(type.dictType(), "IT_A");
            long idB = createItem(type.dictType(), "IT_B");

            // When：B 上移
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("direction", "up"))
                .when().post(ITEM_PATH + "/" + idB + "/move")
                .then().statusCode(200).body("code", equalTo(200));

            // Then：分页列表中 B 排在 A 之前
            List<Map<String, Object>> list = given().port(port).header("Authorization", "Bearer " + adminToken())
                .queryParam("dictType", type.dictType()).queryParam("pageSize", 50)
                .when().get(ITEM_PATH)
                .then().statusCode(200)
                .extract().jsonPath().getList("data.list");
            List<Long> ids = list.stream().map(i -> Long.parseLong(String.valueOf(i.get("id")))).toList();
            assertThat(ids.indexOf(idB)).isLessThan(ids.indexOf(idA));
        }

        @Test
        @DisplayName("已处于分组底部时下移，返回422参数错误")
        void shouldReturn422ParamError_whenMoveDownAtBottom() {
            // Given：新建字典项 sort 为分组 max+10，必处于底部
            CreatedType type = createType();
            long id = createItem(type.dictType(), "IT_BOTTOM");

            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("direction", "down"))
                .when().post(ITEM_PATH + "/" + id + "/move")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.PARAM_ERROR.getCode()));
        }

        @Test
        @DisplayName("移动不存在的字典项id，返回422资源不存在")
        void shouldReturn422NotFound_whenIdNotExist() {
            // When / Then
            given().port(port).header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("direction", "up"))
                .when().post(ITEM_PATH + "/999999999/move")
                .then().statusCode(422)
                .body("code", equalTo(CommonErrorCode.NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("鉴权校验")
    class AuthRoundTest {

        @Test
        @DisplayName("低权限用户访问字典类型，返回403禁止访问")
        void shouldReturn403Forbidden_whenLimitedUserAccessDictType() {
            // When / Then
            given().port(port)
                .header("Authorization", "Bearer " + limitedToken())
                .when().get(TYPE_PATH)
                .then().statusCode(403)
                .body("code", equalTo(CommonErrorCode.FORBIDDEN.getCode()));
        }
    }
}
