package me.north30.erp.common.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.Getter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT 令牌提供者：基于 NimbusJwtEncoder/NimbusJwtDecoder 签发与解析 access/refresh token（HS256）。
 * <p>claim 约定（详细设计 2.5）：sub=用户 ID、username=用户名、type=access/refresh、
 * rjti=配对 refresh token 的 jti（登出时定位失效 refresh 令牌）、jti=UUID、iss=north30-erp。</p>
 */
@Component
public class JwtTokenProvider {

    /** type claim 键 */
    public static final String CLAIM_TYPE = "type";
    /** username claim 键 */
    public static final String CLAIM_USERNAME = "username";
    /** rjti claim 键：access token 携带的配对 refresh token jti */
    public static final String CLAIM_REFRESH_JTI = "rjti";
    /** type=access（可访问业务接口） */
    public static final String TYPE_ACCESS = "access";
    /** type=refresh（仅可用于 /api/auth/refresh） */
    public static final String TYPE_REFRESH = "refresh";

    /** HS256 密钥最小字节数（256 bit） */
    private static final int MIN_SECRET_BYTES = 32;

    @Getter
    private final JwtProperties properties;
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("erp.jwt.secret 长度不足 256 bit（要求至少 32 字节），请配置足够长的密钥");
        }
        this.properties = properties;
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        // 签发器：单个对称密钥 JWK 源
        JWKSet jwkSet = new JWKSet(new OctetSequenceKey.Builder(secretBytes).keyID("erp-jwt-hs256").build());
        JWKSource<SecurityContext> jwkSource = (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
        this.encoder = new NimbusJwtEncoder(jwkSource);
        // 解码器：验签 + exp 校验 + iss 校验
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        this.decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
    }

    /**
     * 签发 access token（type=access，携带配对 refresh token 的 jti）。
     *
     * @param userId 用户 ID（sub）
     * @param username 用户名
     * @param refreshJti 配对 refresh token 的 jti
     * @return 已签发令牌信息
     */
    public IssuedToken createAccessToken(Long userId, String username, String refreshJti) {
        return issue(userId, username, TYPE_ACCESS, refreshJti, properties.getAccessTtl());
    }

    /**
     * 签发 refresh token（type=refresh，仅可用于 /api/auth/refresh）。
     *
     * @param userId 用户 ID（sub）
     * @param username 用户名
     * @return 已签发令牌信息
     */
    public IssuedToken createRefreshToken(Long userId, String username) {
        return issue(userId, username, TYPE_REFRESH, null, properties.getRefreshTtl());
    }

    /**
     * 解析并校验令牌：验签、有效期、签发者、type claim。
     *
     * @param token JWT 字符串
     * @param expectedType 期望的 type claim（access/refresh）
     * @return 解析后的 Jwt
     * @throws JwtException 验签失败、已过期、签发者不符或 type 不匹配
     */
    public Jwt parse(String token, String expectedType) {
        Jwt jwt = decoder.decode(token);
        String type = jwt.getClaimAsString(CLAIM_TYPE);
        if (!expectedType.equals(type)) {
            throw new BadJwtException("令牌 type claim 不匹配：期望 " + expectedType + "，实际 " + type);
        }
        return jwt;
    }

    /**
     * 资源服务器使用的解码器（SecurityConfig 接入 oauth2ResourceServer）。
     */
    public NimbusJwtDecoder getDecoder() {
        return decoder;
    }

    /**
     * 统一签发：生成 jti（UUID）、组装标准 claim 并用 HS256 编码。
     */
    private IssuedToken issue(Long userId, String username, String type, String refreshJti, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .subject(String.valueOf(userId))
            .id(jti)
            .claim(CLAIM_TYPE, type)
            .claim(CLAIM_USERNAME, username)
            .issuer(properties.getIssuer())
            .issuedAt(now)
            .expiresAt(expiresAt);
        if (refreshJti != null) {
            builder.claim(CLAIM_REFRESH_JTI, refreshJti);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        try {
            Jwt encoded = encoder.encode(JwtEncoderParameters.from(header, builder.build()));
            return new IssuedToken(encoded.getTokenValue(), jti, expiresAt, ttl.toSeconds());
        } catch (JwtEncodingException e) {
            throw new IllegalStateException("JWT 签发失败", e);
        }
    }
}
