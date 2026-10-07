package me.north30.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 配置（API 规范文档 1.x：统一响应体 Result 与错误码在文档中的呈现）。
 * <p>Swagger UI 地址 /swagger-ui.html，仅 dev/test 环境开启（application-prod.yml 已关闭）。</p>
 */
@Configuration
public class OpenApiConfig {

    /** Bearer JWT 安全方案名，SecurityConfig 白名单已放行 /v3/api-docs 与 /swagger-ui */
    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

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
     * 全局注入 401/403/500 通用响应（引用 Result 结构），避免逐接口重复声明。
     * 422 业务校验失败由各接口按错误码自行声明示例。
     */
    @Bean
    public GlobalOpenApiCustomizer commonResponsesCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
            pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                addIfAbsent(responses, "401", "未认证：Token 缺失/过期/会话失效");
                addIfAbsent(responses, "403", "已认证但无权限（缺少接口所需权限点）");
                addIfAbsent(responses, "500", "服务器内部错误");
            }));
    }

    private void addIfAbsent(ApiResponses responses, String code, String description) {
        if (responses == null || responses.containsKey(code)) {
            return;
        }
        responses.addApiResponse(code, new ApiResponse().description(description));
    }
}
