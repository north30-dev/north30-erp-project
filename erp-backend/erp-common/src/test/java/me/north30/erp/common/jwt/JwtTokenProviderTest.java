package me.north30.erp.common.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtTokenProvider 单元测试：签发解析往返、type 校验、过期、篡改与密钥校验。
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef";

    private JwtProperties properties;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        provider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("access token 签发解析往返：sub/username/type/rjti/jti claim 完整")
    void createAndParseAccessToken() {
        IssuedToken refresh = provider.createRefreshToken(1L, "admin");
        IssuedToken access = provider.createAccessToken(1L, "admin", refresh.jti());

        Jwt jwt = provider.parse(access.token(), JwtTokenProvider.TYPE_ACCESS);

        assertEquals("1", jwt.getSubject());
        assertEquals("admin", jwt.getClaimAsString(JwtTokenProvider.CLAIM_USERNAME));
        assertEquals(JwtTokenProvider.TYPE_ACCESS, jwt.getClaimAsString(JwtTokenProvider.CLAIM_TYPE));
        assertEquals(refresh.jti(), jwt.getClaimAsString(JwtTokenProvider.CLAIM_REFRESH_JTI));
        assertEquals(access.jti(), jwt.getId());
        assertEquals(Duration.ofHours(2).toSeconds(), access.ttlSeconds());
    }

    @Test
    @DisplayName("refresh token 签发解析：type=refresh 且无 rjti claim")
    void createAndParseRefreshToken() {
        IssuedToken refresh = provider.createRefreshToken(2L, "warehouse01");

        Jwt jwt = provider.parse(refresh.token(), JwtTokenProvider.TYPE_REFRESH);

        assertEquals("2", jwt.getSubject());
        assertEquals(JwtTokenProvider.TYPE_REFRESH, jwt.getClaimAsString(JwtTokenProvider.CLAIM_TYPE));
        assertNull(jwt.getClaimAsString(JwtTokenProvider.CLAIM_REFRESH_JTI));
        assertEquals(Duration.ofDays(7).toSeconds(), refresh.ttlSeconds());
    }

    @Test
    @DisplayName("type 校验：access token 不能按 refresh type 解析")
    void parseWithWrongTypeThrows() {
        IssuedToken access = provider.createAccessToken(1L, "admin", "unused-rjti");

        assertThrows(BadJwtException.class,
            () -> provider.parse(access.token(), JwtTokenProvider.TYPE_REFRESH));
    }

    @Test
    @DisplayName("过期 token 解析抛 JwtException")
    void parseExpiredTokenThrows() {
        // 直接用 Nimbus 构造过期 90 秒的 access token（超过解码器默认 60 秒时钟偏移）
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("1")
            .jwtID(UUID.randomUUID().toString())
            .claim(JwtTokenProvider.CLAIM_TYPE, JwtTokenProvider.TYPE_ACCESS)
            .issuer(properties.getIssuer())
            .issueTime(Date.from(now.minusSeconds(120)))
            .expirationTime(Date.from(now.minusSeconds(90)))
            .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJWT.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException e) {
            throw new IllegalStateException("测试 JWT 构造失败", e);
        }

        assertThrows(JwtException.class,
            () -> provider.parse(signedJWT.serialize(), JwtTokenProvider.TYPE_ACCESS));
    }

    @Test
    @DisplayName("篡改签名后解析抛 JwtException")
    void parseTamperedTokenThrows() {
        IssuedToken access = provider.createAccessToken(1L, "admin", "rjti");
        String tampered = access.token().substring(0, access.token().length() - 2) + "xx";

        assertThrows(JwtException.class,
            () -> provider.parse(tampered, JwtTokenProvider.TYPE_ACCESS));
    }

    @Test
    @DisplayName("密钥不足 32 字节抛 IllegalStateException")
    void shortSecretThrows() {
        JwtProperties shortProperties = new JwtProperties();
        shortProperties.setSecret("too-short-secret");

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(shortProperties));
    }
}
