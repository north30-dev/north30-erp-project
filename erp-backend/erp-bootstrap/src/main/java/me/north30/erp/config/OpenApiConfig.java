package me.north30.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import me.north30.erp.common.exception.CommonErrorCode;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * springdoc-openapi 配置（API 规范文档 1.x：统一响应体 Result 与错误码在文档中的呈现）。
 * <p>Swagger UI 地址 /swagger-ui.html，仅 dev/test 环境开启（application-prod.yml 已关闭）。</p>
 */
@Configuration
public class OpenApiConfig {

    /** Bearer JWT 安全方案名，SecurityConfig 白名单已放行 /v3/api-docs 与 /swagger-ui */
    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    /** 401/403 通用失败响应体组件名（引用方写 #/components/schemas/ + 此值） */
    private static final String FAILURE_SCHEMA_NAME = "SecurityFailureResult";

    /** 409 数据冲突（乐观锁/唯一约束）响应体组件名 */
    private static final String CONFLICT_SCHEMA_NAME = "ConflictFailureResult";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("north30 ERP API")
                .version("0.0.1")
                .description("north30 ERP 系统接口文档，契约以本页为准，规则详见 docs/ERP系统开发API设计与规范文档.md"))
            .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            // 全局安全要求：Swagger UI 出现 Authorize 按钮，每个接口自动携带 Authorization 头
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * /api 业务接口分组：仅收敛 RESTful 业务路径，排除 springdoc 自身端点。
     */
    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
            .group("api")
            .pathsToMatch("/api/**")
            .build();
    }

    /**
     * 全局注入 401/403/500 通用响应（引用 Result 结构），写操作额外注入 409 数据冲突响应，
     * 避免逐接口重复声明。422 业务校验失败由各接口按错误码自行声明示例。
     */
    @Bean
    public GlobalOpenApiCustomizer commonResponsesCustomizer() {
        return openApi -> {
            registerFailureSchema(openApi);
            registerConflictSchema(openApi);
            openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    ApiResponses responses = operation.getResponses();
                    addIfAbsent(responses, "401", "未认证：Token 缺失/过期/会话失效", FAILURE_SCHEMA_NAME);
                    addIfAbsent(responses, "403", "已认证但无权限（缺少接口所需权限点）", FAILURE_SCHEMA_NAME);
                    addIfAbsent(responses, "500", "服务器内部错误", FAILURE_SCHEMA_NAME);
                    // 409 数据冲突仅注入写操作（POST/PUT/DELETE/PATCH），GET 读取无并发修改场景
                    if (httpMethod != PathItem.HttpMethod.GET) {
                        addIfAbsent(responses, "409", "数据冲突：乐观锁版本冲突（10601）或唯一约束冲突（10602）",
                            CONFLICT_SCHEMA_NAME);
                    }
                }));
        };
    }

    /**
     * 全局注入响应体 schema：引用 Result 结构，避免逐接口重复声明。
     */
    private void addIfAbsent(ApiResponses responses, String code, String description, String schemaName) {
        if (responses == null || responses.containsKey(code)) {
            return;
        }
        responses.addApiResponse(code, new ApiResponse()
            .description(description)
            .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + schemaName)))));
    }

    /**
     * 401/403 通用失败响应体 schema：与 SecurityConfig.writeFailure 写出的 Result 结构一致。
     * <p>必须在 GlobalOpenApiCustomizer 内注册（springdoc 分组输出会剔除 OpenAPI Bean 中
     * 未被模型转换追踪的 schema），注册与 $ref 引用在同一对象上才不会被裁剪。</p>
     */
    private void registerFailureSchema(OpenAPI openApi) {
        openApi.schema(FAILURE_SCHEMA_NAME, new ObjectSchema()
            .addProperty("code", new IntegerSchema()._enum(List.of(
                CommonErrorCode.UNAUTHORIZED.getCode(), CommonErrorCode.FORBIDDEN.getCode()))
                .example(CommonErrorCode.UNAUTHORIZED.getCode()))
            .addProperty("message", new StringSchema().example("未登录或凭证已过期"))
            .addProperty("data", new Schema<>().nullable(true).example(null))
            .addProperty("timestamp", new IntegerSchema().example(1792067126886L))
            .addProperty("traceId", new StringSchema().example("bd03058f6f0d4e18b30a9ce5bbe58a09")));
    }

    /**
     * 409 数据冲突响应体 schema：与 GlobalExceptionHandler 的乐观锁（10601）/唯一约束（10602）
     * 处理器写出的 Result 结构一致，同样需在 GlobalOpenApiCustomizer 内注册避免被分组裁剪。
     */
    private void registerConflictSchema(OpenAPI openApi) {
        openApi.schema(CONFLICT_SCHEMA_NAME, new ObjectSchema()
            .addProperty("code", new IntegerSchema()._enum(List.of(
                CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(), CommonErrorCode.DUPLICATE_KEY.getCode()))
                .example(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()))
            .addProperty("message", new StringSchema().example(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessage()))
            .addProperty("data", new Schema<>().nullable(true).example(null))
            .addProperty("timestamp", new IntegerSchema().example(1792067126886L))
            .addProperty("traceId", new StringSchema().example("bd03058f6f0d4e18b30a9ce5bbe58a09")));
    }
}
