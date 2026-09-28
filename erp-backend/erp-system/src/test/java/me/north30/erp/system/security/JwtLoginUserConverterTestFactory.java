package me.north30.erp.system.security;

import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.system.auth.dto.UserSecurityData;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

/**
 * JwtLoginUserConverter 测试数据静态工厂：统一构造真实 Jwt（不 mock final 类）与用户安全数据。
 */
public final class JwtLoginUserConverterTestFactory {

    /** 被测用户 ID */
    public static final Long USER_ID = 1L;

    /** 用户名 */
    public static final String USERNAME = "admin";

    /** access token jti */
    public static final String ACCESS_JTI = "access-jti-0001";

    /** refresh token jti */
    public static final String REFRESH_JTI = "refresh-jti-0001";

    private JwtLoginUserConverterTestFactory() {
    }

    /**
     * type=access 的合法 Jwt（claim 与签发约定一致）。
     */
    public static Jwt accessJwt() {
        return jwt(JwtTokenProvider.TYPE_ACCESS, String.valueOf(USER_ID), ACCESS_JTI, REFRESH_JTI);
    }

    /**
     * 指定 type claim 的 Jwt（类型不匹配场景用）。
     */
    public static Jwt jwtWithType(String type) {
        return jwt(type, String.valueOf(USER_ID), ACCESS_JTI, REFRESH_JTI);
    }

    /**
     * subject 非数字的 Jwt（身份信息无效场景用）。
     */
    public static Jwt jwtWithInvalidSubject() {
        return jwt(JwtTokenProvider.TYPE_ACCESS, "not-a-number", ACCESS_JTI, REFRESH_JTI);
    }

    /**
     * 启用状态、含两个角色与两个权限点的安全数据。
     */
    public static UserSecurityData securityData() {
        return new UserSecurityData(USER_ID, 1, 1, List.of("admin", "manager"),
            List.of("system:user:list", "system:user:add"), 1);
    }

    /**
     * 停用状态（status=0）的安全数据。
     */
    public static UserSecurityData disabledSecurityData() {
        return new UserSecurityData(USER_ID, 0, 1, List.of(), List.of(), 6);
    }

    /**
     * 构造 Jwt：header/type/username/rjti/jti/iat/exp 全量填充。
     */
    private static Jwt jwt(String type, String subject, String jti, String refreshJti) {
        return Jwt.withTokenValue("token-value")
            .header("alg", "HS256")
            .subject(subject)
            .claim(JwtTokenProvider.CLAIM_TYPE, type)
            .claim(JwtTokenProvider.CLAIM_USERNAME, USERNAME)
            .claim(JwtTokenProvider.CLAIM_REFRESH_JTI, refreshJti)
            .jti(jti)
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(600))
            .build();
    }
}
