package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.jwt.IssuedToken;
import me.north30.erp.common.jwt.JwtProperties;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.system.core.dto.LoginDTO;
import me.north30.erp.system.core.dto.RefreshTokenDTO;
import me.north30.erp.system.core.entity.SysLoginLog;
import me.north30.erp.system.core.entity.SysUser;
import me.north30.erp.system.core.service.ISysDeptService;
import me.north30.erp.system.core.service.ISysLoginLogService;
import me.north30.erp.system.core.service.ISysMenuService;
import me.north30.erp.system.core.service.ISysRoleMenuService;
import me.north30.erp.system.core.service.ISysRoleService;
import me.north30.erp.system.core.service.ISysUserRoleService;
import me.north30.erp.system.core.service.ISysUserService;
import me.north30.erp.system.core.service.UserSecurityQueryService;
import me.north30.erp.system.core.vo.LoginVO;
import me.north30.erp.system.core.vo.RefreshTokenVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl 登录/刷新单元测试：
 * 覆盖成功、密码错误、账号停用、验证码错误、账号锁定、用户不存在与 refresh 正常/失效分支。
 * Redis 与各 Service 均为 Mock，JwtTokenProvider 与 BCryptPasswordEncoder 为真实实现。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef";
    private static final String CAPTCHA_CODE = "Ab3d";
    private static final String CAPTCHA_KEY = "captcha-key-1";
    private static final String CAPTCHA_REDIS_KEY = "system:captcha:" + CAPTCHA_KEY;
    private static final String FAIL_REDIS_KEY = "system:login:fail:admin";
    private static final String RAW_PASSWORD = "Admin@123456";

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ISysUserService sysUserService;
    @Mock
    private ISysLoginLogService sysLoginLogService;
    @Mock
    private ISysDeptService sysDeptService;
    @Mock
    private ISysUserRoleService sysUserRoleService;
    @Mock
    private ISysRoleService sysRoleService;
    @Mock
    private ISysRoleMenuService sysRoleMenuService;
    @Mock
    private ISysMenuService sysMenuService;
    @Mock
    private UserSecurityQueryService userSecurityQueryService;

    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        jwtTokenProvider = new JwtTokenProvider(properties);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(stringRedisTemplate, jwtTokenProvider, passwordEncoder,
            sysUserService, sysLoginLogService, sysDeptService, sysUserRoleService,
            sysRoleService, sysRoleMenuService, sysMenuService, userSecurityQueryService);
    }

    private LoginDTO loginDTO(String username, String password) {
        return new LoginDTO(username, password, CAPTCHA_CODE, CAPTCHA_KEY);
    }

    private SysUser userWithStatus(Integer status) {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setRealName("系统管理员");
        user.setStatus(status);
        user.setIsAdmin(1);
        user.setPasswordUpdateTime(LocalDateTime.now());
        return user;
    }

    private void stubCaptchaOk() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(CAPTCHA_REDIS_KEY)).thenReturn(CAPTCHA_CODE);
    }

    @Test
    @DisplayName("登录成功：签发 token 对、记录日志、更新最后登录信息")
    void loginSuccess() {
        stubCaptchaOk();
        when(sysUserService.getByUsername("admin")).thenReturn(userWithStatus(1));
        when(sysUserRoleService.listByUserId(1L)).thenReturn(List.of());
        when(sysRoleService.listByIds(any())).thenReturn(List.of());

        LoginVO vo = authService.login(loginDTO("admin", RAW_PASSWORD));

        assertNotNull(vo.accessToken());
        assertNotNull(vo.refreshToken());
        assertEquals("Bearer", vo.tokenType());
        assertEquals((int) Duration.ofHours(2).toSeconds(), vo.expiresIn());
        assertNotNull(vo.userInfo());
        assertEquals("admin", vo.userInfo().username());
        assertEquals(1, vo.userInfo().isAdmin());
        // access token 的 rjti 与 refresh token 的 jti 配对
        Jwt accessJwt = jwtTokenProvider.parse(vo.accessToken(), JwtTokenProvider.TYPE_ACCESS);
        Jwt refreshJwt = jwtTokenProvider.parse(vo.refreshToken(), JwtTokenProvider.TYPE_REFRESH);
        assertEquals(refreshJwt.getId(), accessJwt.getClaimAsString(JwtTokenProvider.CLAIM_REFRESH_JTI));
        // 成功日志 + 最后登录信息更新
        verify(sysLoginLogService).record(any(SysLoginLog.class));
        verify(sysUserService).updateLastLogin(eq(1L), anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("密码错误：抛 18001，失败计数累加并记录失败日志")
    void loginWrongPassword() {
        stubCaptchaOk();
        when(sysUserService.getByUsername("admin")).thenReturn(userWithStatus(1));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(loginDTO("admin", "Wrong@123")));

        assertEquals(18001, ex.getCode());
        verify(valueOperations).increment(FAIL_REDIS_KEY);
        verify(sysLoginLogService).record(any(SysLoginLog.class));
    }

    @Test
    @DisplayName("账号停用：抛 18003")
    void loginDisabledAccount() {
        stubCaptchaOk();
        when(sysUserService.getByUsername("admin")).thenReturn(userWithStatus(0));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(loginDTO("admin", RAW_PASSWORD)));

        assertEquals(18003, ex.getCode());
    }

    @Test
    @DisplayName("验证码错误：抛 18004 且不查用户")
    void loginCaptchaError() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(CAPTCHA_REDIS_KEY)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(loginDTO("admin", RAW_PASSWORD)));

        assertEquals(18004, ex.getCode());
        verify(sysUserService, org.mockito.Mockito.never()).getByUsername(anyString());
    }

    @Test
    @DisplayName("账号锁定：Redis 失败计数达阈值抛 18002")
    void loginLockedAccount() {
        stubCaptchaOk();
        when(sysUserService.getByUsername("admin")).thenReturn(userWithStatus(1));
        when(valueOperations.get(FAIL_REDIS_KEY)).thenReturn("5");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(loginDTO("admin", RAW_PASSWORD)));

        assertEquals(18002, ex.getCode());
    }

    @Test
    @DisplayName("用户不存在：抛 18001 并记录失败日志")
    void loginUserNotFound() {
        stubCaptchaOk();
        when(sysUserService.getByUsername("ghost")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.login(loginDTO("ghost", RAW_PASSWORD)));

        assertEquals(18001, ex.getCode());
        verify(sysLoginLogService).record(any(SysLoginLog.class));
    }

    @Test
    @DisplayName("刷新成功：签发新 access token 且不延长 refresh")
    void refreshTokenSuccess() {
        // refresh 流程仅会话写入用到 opsForValue，不涉及验证码
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        IssuedToken refresh = jwtTokenProvider.createRefreshToken(1L, "admin");
        when(stringRedisTemplate.hasKey("system:refresh:invalid:1:" + refresh.jti())).thenReturn(false);
        when(stringRedisTemplate.hasKey("system:session:refresh:1:" + refresh.jti())).thenReturn(true);
        when(sysUserService.getById(1L)).thenReturn(userWithStatus(1));

        RefreshTokenVO vo = authService.refreshToken(new RefreshTokenDTO(refresh.token()));

        assertNotNull(vo.accessToken());
        assertEquals("Bearer", vo.tokenType());
        assertEquals((int) Duration.ofHours(2).toSeconds(), vo.expiresIn());
        verify(sysLoginLogService).record(any(SysLoginLog.class));
    }

    @Test
    @DisplayName("用 access token 刷新：type 不匹配抛 10102")
    void refreshTokenWithTypeMismatch() {
        IssuedToken access = jwtTokenProvider.createAccessToken(1L, "admin", "rjti");

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.refreshToken(new RefreshTokenDTO(access.token())));

        assertEquals(10102, ex.getCode());
    }

    @Test
    @DisplayName("jti 已失效（登出后）：刷新抛 10102")
    void refreshTokenWithInvalidatedJti() {
        IssuedToken refresh = jwtTokenProvider.createRefreshToken(1L, "admin");
        when(stringRedisTemplate.hasKey("system:refresh:invalid:1:" + refresh.jti())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> authService.refreshToken(new RefreshTokenDTO(refresh.token())));

        assertEquals(10102, ex.getCode());
    }

    @Test
    @DisplayName("签发的 token 对 jti 互不相同且可解析")
    void issuedTokensHaveDistinctJti() {
        IssuedToken refresh = jwtTokenProvider.createRefreshToken(1L, "admin");
        IssuedToken access = jwtTokenProvider.createAccessToken(1L, "admin", refresh.jti());

        assertNotEquals(access.jti(), refresh.jti());
        assertTrue(jwtTokenProvider.parse(access.token(), JwtTokenProvider.TYPE_ACCESS) != null);
        assertTrue(jwtTokenProvider.parse(refresh.token(), JwtTokenProvider.TYPE_REFRESH) != null);
    }
}
