package me.north30.erp.system.auth.service.impl;

import me.north30.erp.common.jwt.IssuedToken;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.system.auth.dto.ChangePasswordDTO;
import me.north30.erp.system.auth.dto.LoginDTO;
import me.north30.erp.system.auth.dto.RefreshTokenDTO;
import me.north30.erp.system.auth.dto.UserSecurityData;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.user.entity.SysUser;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AuthServiceImpl 测试数据静态工厂：统一构造账号、请求 DTO、签发令牌与安全数据等测试数据。
 */
public final class AuthServiceImplTestFactory {

    /** 被测用户 ID */
    public static final Long USER_ID = 1L;

    /** 登录用户名 */
    public static final String USERNAME = "admin";

    /** 明文口令 */
    public static final String RAW_PASSWORD = "Admin@123456";

    /** BCrypt 密文（桩值，仅作匹配参数） */
    public static final String ENCODED_PASSWORD = "$2a$10$unit-test-encoded-hash";

    /** 验证码标识 */
    public static final String CAPTCHA_KEY = "captcha-key-0001";

    /** 验证码文本 */
    public static final String CAPTCHA_CODE = "ABCD";

    /** 部门 ID */
    public static final Long DEPT_ID = 10L;

    /** access token jti */
    public static final String ACCESS_JTI = "access-jti-0001";

    /** refresh token jti */
    public static final String REFRESH_JTI = "refresh-jti-0001";

    /** access token 字符串 */
    public static final String ACCESS_TOKEN_VALUE = "access-token-value";

    /** refresh token 字符串 */
    public static final String REFRESH_TOKEN_VALUE = "refresh-token-value";

    /** access token 有效期（秒） */
    public static final long ACCESS_TTL_SECONDS = 7200L;

    /** refresh token 有效期（秒，默认 7 天） */
    public static final long REFRESH_TTL_SECONDS = 604800L;

    private AuthServiceImplTestFactory() {
    }

    /**
     * 启用状态的普通用户（口令修改时间 1 天前，未过期；仓库、手机号、最后登录信息齐全）。
     */
    public static SysUser user() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUserCode("EMP001");
        user.setUsername(USERNAME);
        user.setPassword(ENCODED_PASSWORD);
        user.setRealName("管理员");
        user.setDeptId(DEPT_ID);
        user.setWarehouseIds("1, 2");
        user.setPhone("13812345678");
        user.setEmail("admin@erp.com");
        user.setStatus(1);
        user.setIsAdmin(0);
        user.setPasswordUpdateTime(LocalDateTime.now().minusDays(1));
        user.setLastLoginTime(LocalDateTime.now().minusHours(1));
        user.setLastLoginIp("192.168.1.10");
        return user;
    }

    /**
     * 启用状态的超级管理员（is_admin=1）。
     */
    public static SysUser adminUser() {
        SysUser user = user();
        user.setIsAdmin(1);
        return user;
    }

    /**
     * 停用状态用户（status=0）。
     */
    public static SysUser disabledUser() {
        SysUser user = user();
        user.setStatus(0);
        return user;
    }

    /**
     * 口令修改时间超过 90 天有效期的用户。
     */
    public static SysUser passwordExpiredUser() {
        SysUser user = user();
        user.setPasswordUpdateTime(LocalDateTime.now().minusDays(100));
        return user;
    }

    /**
     * 登录请求 DTO（验证码与工厂常量一致）。
     */
    public static LoginDTO loginDTO() {
        return new LoginDTO(USERNAME, RAW_PASSWORD, CAPTCHA_CODE, CAPTCHA_KEY);
    }

    /**
     * 刷新令牌请求 DTO。
     */
    public static RefreshTokenDTO refreshTokenDTO() {
        return new RefreshTokenDTO(REFRESH_TOKEN_VALUE);
    }

    /**
     * 修改本人密码请求 DTO（原密码与新密码均符合复杂度）。
     */
    public static ChangePasswordDTO changePasswordDTO() {
        return new ChangePasswordDTO("Old@12345", "New@12345", "New@12345");
    }

    /**
     * 自定义新密码/确认密码的修改密码请求 DTO。
     */
    public static ChangePasswordDTO changePasswordDTO(String newPassword, String confirmPassword) {
        return new ChangePasswordDTO("Old@12345", newPassword, confirmPassword);
    }

    /**
     * 已签发 access token（jti 与 TTL 与常量一致）。
     */
    public static IssuedToken accessToken() {
        return new IssuedToken(ACCESS_TOKEN_VALUE, ACCESS_JTI,
            Instant.now().plusSeconds(ACCESS_TTL_SECONDS), ACCESS_TTL_SECONDS);
    }

    /**
     * 已签发 refresh token（jti 与 TTL 与常量一致）。
     */
    public static IssuedToken refreshToken() {
        return new IssuedToken(REFRESH_TOKEN_VALUE, REFRESH_JTI,
            Instant.now().plusSeconds(REFRESH_TTL_SECONDS), REFRESH_TTL_SECONDS);
    }

    /**
     * 登录态 principal（SecurityContext 装载用，jti 与令牌常量一致）。
     */
    public static LoginUser loginUser() {
        return new LoginUser(USER_ID, USERNAME, ACCESS_JTI, REFRESH_JTI);
    }

    /**
     * type=refresh 的真实 Jwt（subject/username/jti 与签发约定一致，不 mock final 类 Jwt）。
     */
    public static Jwt refreshJwt() {
        return Jwt.withTokenValue(REFRESH_TOKEN_VALUE)
            .header("alg", "HS256")
            .subject(String.valueOf(USER_ID))
            .claim(JwtTokenProvider.CLAIM_TYPE, JwtTokenProvider.TYPE_REFRESH)
            .claim(JwtTokenProvider.CLAIM_USERNAME, USERNAME)
            .jti(REFRESH_JTI)
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(600))
            .build();
    }

    /**
     * 用户安全数据（启用、非 admin、单角色、单权限点、数据范围 1-全部）。
     */
    public static UserSecurityData securityData() {
        return new UserSecurityData(USER_ID, 1, 0, List.of("admin"),
            List.of("system:user:list"), 1);
    }

    /**
     * 菜单树节点 VO（菜单树委托返回值用）。
     */
    public static MenuTreeVO menuTreeVO(Long menuId) {
        return new MenuTreeVO(menuId, "系统管理", null, 0L, null, null, null, null, null, null);
    }
}
