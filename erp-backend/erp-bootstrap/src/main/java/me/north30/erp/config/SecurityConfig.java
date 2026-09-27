package me.north30.erp.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.common.result.Result;
import me.north30.erp.common.web.TraceIdFilter;
import me.north30.erp.system.core.security.JwtLoginUserConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置：OAuth2 资源服务器（JWT）+ 免认证白名单 + 统一 401/403 响应体。
 * <p>白名单显式列举（详细设计 2.5，禁通配放开业务路径）；无状态会话；CSRF 关闭（纯 API）。</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtLoginUserConverter jwtLoginUserConverter;

    public SecurityConfig(ObjectMapper objectMapper,
                          JwtTokenProvider jwtTokenProvider,
                          JwtLoginUserConverter jwtLoginUserConverter) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtLoginUserConverter = jwtLoginUserConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 强度 10（接口文档 4.8/SYS-09 口令约定）
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 免认证白名单（显式列举，禁通配）
                .requestMatchers("/api/auth/captcha", "/api/auth/login", "/api/auth/refresh",
                    "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                // 其余全部需要认证
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtTokenProvider.getDecoder())
                    .jwtAuthenticationConverter(jwtLoginUserConverter::convert)))
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()));
        return http.build();
    }

    /**
     * 前端开发 CORS 配置：放开本机 Vite 开发服务器来源。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(TraceIdFilter.TRACE_ID_HEADER));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 401 未认证：Token 缺失/过期/验签失败/type 非 access/会话不存在，响应体与 Result 结构一致。
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("安全拦截 401 | uri: {} | reason: {}", request.getRequestURI(),
                authException != null ? authException.getMessage() : "未认证");
            writeFailure(response, HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED);
        };
    }

    /**
     * 403 已认证但无权限：缺少接口所需权限点（纵向越权）或数据范围越界。
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("安全拦截 403 | uri: {} | reason: {}", request.getRequestURI(),
                accessDeniedException != null ? accessDeniedException.getMessage() : "无权限");
            writeFailure(response, HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN);
        };
    }

    /**
     * 直接写 Result 结构 JSON（UTF-8 application/json，含 code/message/timestamp/traceId）。
     */
    private void writeFailure(HttpServletResponse response, HttpStatus status, CommonErrorCode errorCode) {
        try {
            response.setStatus(status.value());
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), Result.failure(errorCode));
        } catch (Exception e) {
            // 响应已提交或写出失败时仅记录日志，无法再改写状态码
            log.error("安全拦截响应体写出失败：{}", e.getMessage());
        }
    }
}
