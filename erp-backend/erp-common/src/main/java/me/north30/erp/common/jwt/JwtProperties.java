package me.north30.erp.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置项（application.yml 的 erp.jwt 前缀）。
 * <p>secret 生产环境必须通过环境变量 ERP_JWT_SECRET 注入，禁止入库入日志。</p>
 */
@Data
@ConfigurationProperties(prefix = "erp.jwt")
public class JwtProperties {

    /** HS256 签名密钥：要求 ≥256 bit（32 字节），生产环境用环境变量 ERP_JWT_SECRET 注入 */
    private String secret;

    /** access token 有效期，默认 2 小时（API 文档 1.5：120 分钟，expiresIn=7200 秒） */
    private Duration accessTtl = Duration.ofHours(2);

    /** refresh token 有效期，默认 7 天（刷新不延长有效期） */
    private Duration refreshTtl = Duration.ofDays(7);

    /** 签发者（iss claim），默认 north30-erp */
    private String issuer = "north30-erp";
}
