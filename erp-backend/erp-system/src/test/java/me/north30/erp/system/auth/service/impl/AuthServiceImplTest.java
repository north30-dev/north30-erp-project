package me.north30.erp.system.auth.service.impl;

import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.constant.TtlConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.exception.SystemErrorCode;
import me.north30.erp.common.jwt.JwtProperties;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.system.auth.vo.CaptchaVO;
import me.north30.erp.system.auth.vo.ChangePasswordVO;
import me.north30.erp.system.auth.vo.CurrentUserVO;
import me.north30.erp.system.auth.vo.LoginVO;
import me.north30.erp.system.auth.vo.LogoutVO;
import me.north30.erp.system.auth.vo.RefreshTokenVO;
import me.north30.erp.system.auth.vo.UserPermsVO;
import me.north30.erp.system.auth.service.UserAccessService;
import me.north30.erp.system.common.enums.LoginTypeEnum;
import me.north30.erp.system.log.service.SysLoginLogService;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import me.north30.erp.system.user.entity.SysUser;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.ACCESS_JTI;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.ACCESS_TOKEN_VALUE;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.ACCESS_TTL_SECONDS;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.CAPTCHA_CODE;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.CAPTCHA_KEY;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.DEPT_ID;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.ENCODED_PASSWORD;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.RAW_PASSWORD;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.REFRESH_JTI;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.REFRESH_TOKEN_VALUE;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.REFRESH_TTL_SECONDS;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.USER_ID;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.USERNAME;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.accessToken;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.adminUser;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.changePasswordDTO;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.disabledUser;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.loginDTO;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.loginUser;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.menuTreeVO;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.passwordExpiredUser;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.refreshJwt;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.refreshToken;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.refreshTokenDTO;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.securityData;
import static me.north30.erp.system.auth.service.impl.AuthServiceImplTestFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl 单元测试：登录认证、验证码、刷新令牌、登出、当前用户信息与改密的正常/异常/降级路径。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAccessService userAccessService;

    @Mock
    private SysLoginLogService sysLoginLogService;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 统一断言业务异常：类型、错误码与提示消息一次校验。
     */
    private static void assertBusinessError(ThrowingCallable action, int expectedCode, String expectedMessage) {
        assertThatThrownBy(action)
            .isInstanceOf(BusinessException.class)
            .hasMessage(expectedMessage)
            .isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getCode()).isEqualTo(expectedCode));
    }

    @Nested
    @DisplayName("创建图形验证码")
    class CreateCaptchaTest {

        @Test
        @DisplayName("正常生成：返回 32 位 key、dataURI 图片与 300 秒有效期，并写入 Redis")
        void shouldReturnCaptchaVo_whenCreateCaptcha() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            // When
            CaptchaVO vo = authService.createCaptcha();
            // Then
            assertThat(vo.captchaKey()).hasSize(32).doesNotContain("-");
            assertThat(vo.captchaImage()).startsWith("data:image/png;base64,");
            assertThat(vo.expireSeconds()).isEqualTo(300);
            verify(valueOperations).set(
                argThat(key -> key != null && key.startsWith("system:captcha:")),
                argThat(code -> code != null && code.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{4}")),
                eq(300L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("Redis 写入失败降级：仍返回验证码 VO 不抛异常")
        void shouldReturnCaptchaVo_whenRedisWriteFailed() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RedisConnectionFailureException("connection refused")).when(valueOperations)
                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
            // When
            CaptchaVO vo = authService.createCaptcha();
            // Then
            assertThat(vo.captchaKey()).hasSize(32);
            assertThat(vo.captchaImage()).startsWith("data:image/png;base64,");
            assertThat(vo.expireSeconds()).isEqualTo(300);
        }
    }

    @Nested
    @DisplayName("登录")
    class LoginTest {

        /**
         * 登录成功链路公共打桩：验证码通过、账号启用、失败计数为 failCount、口令匹配、令牌签发与聚合数据齐备。
         */
        private void stubLoginSuccessFlow(SysUser stubUser, String failCount) {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn(CAPTCHA_CODE);
            when(userAccessService.getByUsername(USERNAME)).thenReturn(stubUser);
            when(valueOperations.get(RedisKeyConstants.loginFailKey(USERNAME))).thenReturn(failCount);
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtTokenProvider.createRefreshToken(USER_ID, USERNAME)).thenReturn(refreshToken());
            when(jwtTokenProvider.createAccessToken(USER_ID, USERNAME, REFRESH_JTI)).thenReturn(accessToken());
            when(userAccessService.getDeptName(DEPT_ID)).thenReturn("研发部");
            when(userAccessService.listRoleCodes(USER_ID)).thenReturn(List.of("admin"));
        }

        @Test
        @DisplayName("登录成功：返回令牌对与用户信息，写 Redis 会话白名单、更新最后登录并记录登录日志")
        void shouldReturnLoginVO_whenLoginSuccess() {
            // Given
            stubLoginSuccessFlow(user(), null);
            // When
            LoginVO vo = authService.login(loginDTO());
            // Then
            assertThat(vo.accessToken()).isEqualTo(ACCESS_TOKEN_VALUE);
            assertThat(vo.tokenType()).isEqualTo("Bearer");
            assertThat(vo.expiresIn()).isEqualTo(7200);
            assertThat(vo.refreshToken()).isEqualTo(REFRESH_TOKEN_VALUE);
            LoginVO.LoginUserInfoVO userInfo = vo.userInfo();
            assertThat(userInfo.userId()).isEqualTo(USER_ID);
            assertThat(userInfo.username()).isEqualTo(USERNAME);
            assertThat(userInfo.realName()).isEqualTo("管理员");
            assertThat(userInfo.deptId()).isEqualTo(DEPT_ID);
            assertThat(userInfo.deptName()).isEqualTo("研发部");
            assertThat(userInfo.roles()).containsExactly("admin");
            assertThat(userInfo.isAdmin()).isEqualTo(0);
            assertThat(userInfo.passwordExpired()).isFalse();
            verify(valueOperations).set(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI),
                "1", ACCESS_TTL_SECONDS, TimeUnit.SECONDS);
            verify(valueOperations).set(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI),
                "1", REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
            verify(userAccessService).updateLastLogin(eq(USER_ID),
                argThat(ip -> ip != null && ip.isEmpty()), any(LocalDateTime.class));
            verify(sysLoginLogService).record(argThat(log -> log != null
                && Long.valueOf(USER_ID).equals(log.getUserId())
                && log.getLoginType() == LoginTypeEnum.LOGIN.getCode()
                && log.getResultStatus() == 1
                && log.getFailReason() == null));
        }

        @Test
        @DisplayName("口令超 90 天未修改：登录成功且 passwordExpired 标记为 true")
        void shouldMarkPasswordExpired_whenPasswordUpdateBeyondValidDuration() {
            // Given
            stubLoginSuccessFlow(passwordExpiredUser(), null);
            // When
            LoginVO vo = authService.login(loginDTO());
            // Then
            assertThat(vo.userInfo().passwordExpired()).isTrue();
            verify(valueOperations).set(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI),
                "1", ACCESS_TTL_SECONDS, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("失败计数 4 次未达阈值 5：不锁定并正常登录")
        void shouldLoginSuccess_whenFailCountBelowThreshold() {
            // Given
            stubLoginSuccessFlow(user(), "4");
            // When
            LoginVO vo = authService.login(loginDTO());
            // Then
            assertThat(vo.accessToken()).isEqualTo(ACCESS_TOKEN_VALUE);
            verify(valueOperations, never()).increment(anyString());
        }

        @Test
        @DisplayName("验证码已过期（Redis 无缓存）：抛 18004 且不触发账号查询")
        void shouldThrowCaptchaError_whenCaptchaExpired() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.CAPTCHA_ERROR.getCode(), "验证码错误或已过期");
            verifyNoInteractions(userAccessService, passwordEncoder, jwtTokenProvider, sysLoginLogService);
        }

        @Test
        @DisplayName("验证码不匹配（忽略大小写比较失败）：抛 18004")
        void shouldThrowCaptchaError_whenCaptchaMismatch() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn("WXYZ");
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.CAPTCHA_ERROR.getCode(), "验证码错误或已过期");
            verifyNoInteractions(userAccessService, passwordEncoder, jwtTokenProvider, sysLoginLogService);
        }

        @Test
        @DisplayName("Redis 不可用降级：跳过验证码校验继续登录流程")
        void shouldSkipCaptchaValidation_whenRedisUnavailable() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
            when(userAccessService.getByUsername(USERNAME)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.USERNAME_PASSWORD_ERROR.getCode(), "用户名或密码错误");
            verify(userAccessService).getByUsername(USERNAME);
        }

        @Test
        @DisplayName("用户不存在：抛 18001 并记录失败日志（统一提示避免泄露账号存在性）")
        void shouldThrowUsernamePasswordError_whenUserNotFound() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn(CAPTCHA_CODE);
            when(userAccessService.getByUsername(USERNAME)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.USERNAME_PASSWORD_ERROR.getCode(), "用户名或密码错误");
            verify(sysLoginLogService).record(argThat(log -> log != null
                && log.getUserId() == null
                && USERNAME.equals(log.getUsername())
                && log.getLoginType() == LoginTypeEnum.LOGIN_FAIL.getCode()
                && log.getResultStatus() == 0
                && "用户名或密码错误".equals(log.getFailReason())));
            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        @DisplayName("账号已停用：抛 18003 且不再读取登录失败计数")
        void shouldThrowAccountDisabled_whenUserDisabled() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn(CAPTCHA_CODE);
            when(userAccessService.getByUsername(USERNAME)).thenReturn(disabledUser());
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.ACCOUNT_DISABLED.getCode(), "账号已停用，请联系管理员");
            verify(valueOperations, never()).get(anyString());
            verify(sysLoginLogService).record(argThat(log -> log != null
                && "账号已停用".equals(log.getFailReason())
                && log.getResultStatus() == 0));
            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        @DisplayName("失败计数达阈值 5：抛 18002 账号已锁定且不校验口令")
        void shouldThrowAccountLocked_whenFailCountReachedThreshold() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn(CAPTCHA_CODE);
            when(userAccessService.getByUsername(USERNAME)).thenReturn(user());
            when(valueOperations.get(RedisKeyConstants.loginFailKey(USERNAME))).thenReturn("5");
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.ACCOUNT_LOCKED.getCode(), "账号已锁定，请 30 分钟后重试");
            verify(valueOperations, never()).increment(anyString());
            verify(sysLoginLogService).record(argThat(log -> log != null
                && "账号已锁定".equals(log.getFailReason())));
            verifyNoInteractions(passwordEncoder, jwtTokenProvider);
        }

        @Test
        @DisplayName("口令不匹配：累加 Redis 失败计数（30 分钟窗口）并抛 18001")
        void shouldThrowUsernamePasswordError_whenPasswordMismatch() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete(RedisKeyConstants.captchaKey(CAPTCHA_KEY))).thenReturn(CAPTCHA_CODE);
            when(userAccessService.getByUsername(USERNAME)).thenReturn(user());
            when(valueOperations.get(RedisKeyConstants.loginFailKey(USERNAME))).thenReturn(null);
            when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
            when(valueOperations.increment(RedisKeyConstants.loginFailKey(USERNAME))).thenReturn(1L);
            // When / Then
            assertBusinessError(() -> authService.login(loginDTO()),
                SystemErrorCode.USERNAME_PASSWORD_ERROR.getCode(), "用户名或密码错误");
            verify(stringRedisTemplate).expire(RedisKeyConstants.loginFailKey(USERNAME), TtlConstants.LOGIN_FAIL_TTL);
            verify(sysLoginLogService).record(argThat(log -> log != null
                && log.getResultStatus() == 0
                && "用户名或密码错误".equals(log.getFailReason())));
            verify(userAccessService, never()).updateLastLogin(any(), any(), any());
            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Nested
    @DisplayName("刷新令牌")
    class RefreshTokenTest {

        /**
         * 刷新成功链路公共打桩：解析通过、jti 未失效、refresh 会话标记存在、账号启用、签发新 access。
         */
        private void stubRefreshHappyPath() {
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtTokenProvider.parse(REFRESH_TOKEN_VALUE, JwtTokenProvider.TYPE_REFRESH)).thenReturn(refreshJwt());
            when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.FALSE);
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.TRUE);
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(jwtTokenProvider.createAccessToken(USER_ID, USERNAME, REFRESH_JTI)).thenReturn(accessToken());
        }

        @Test
        @DisplayName("刷新成功：签发新 access 并写会话、记录刷新日志，且不延长 refresh 令牌")
        void shouldReturnRefreshTokenVO_whenRefreshSuccess() {
            // Given
            stubRefreshHappyPath();
            // When
            RefreshTokenVO vo = authService.refreshToken(refreshTokenDTO());
            // Then
            assertThat(vo.accessToken()).isEqualTo(ACCESS_TOKEN_VALUE);
            assertThat(vo.tokenType()).isEqualTo("Bearer");
            assertThat(vo.expiresIn()).isEqualTo(7200);
            verify(valueOperations).set(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI),
                "1", ACCESS_TTL_SECONDS, TimeUnit.SECONDS);
            verify(sysLoginLogService).record(argThat(log -> log != null
                && log.getLoginType() == LoginTypeEnum.REFRESH.getCode()
                && log.getResultStatus() == 1));
            verify(jwtTokenProvider, never()).createRefreshToken(any(), any());
        }

        @Test
        @DisplayName("refresh token 解析失败（验签/过期/type 不符）：翻译为 10102 且不触发任何查询")
        void shouldThrowRefreshTokenInvalid_whenParseFailed() {
            // Given
            when(jwtTokenProvider.parse(anyString(), eq(JwtTokenProvider.TYPE_REFRESH)))
                .thenThrow(new BadJwtException("验签失败"));
            // When / Then
            assertBusinessError(() -> authService.refreshToken(refreshTokenDTO()),
                CommonErrorCode.REFRESH_TOKEN_INVALID.getCode(), "登录状态已失效，请重新登录");
            verifyNoInteractions(stringRedisTemplate, userAccessService);
        }

        @Test
        @DisplayName("refresh jti 已加入失效集合（登出后）：抛 10102 且不查询账号")
        void shouldThrowRefreshTokenInvalid_whenJtiInvalidated() {
            // Given
            when(jwtTokenProvider.parse(REFRESH_TOKEN_VALUE, JwtTokenProvider.TYPE_REFRESH)).thenReturn(refreshJwt());
            when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.TRUE);
            // When / Then
            assertBusinessError(() -> authService.refreshToken(refreshTokenDTO()),
                CommonErrorCode.REFRESH_TOKEN_INVALID.getCode(), "登录状态已失效，请重新登录");
            verify(userAccessService, never()).getUserById(any());
        }

        @Test
        @DisplayName("refresh 会话标记不存在（会话被删）：抛 10102")
        void shouldThrowRefreshTokenInvalid_whenSessionMarkerMissing() {
            // Given
            when(jwtTokenProvider.parse(REFRESH_TOKEN_VALUE, JwtTokenProvider.TYPE_REFRESH)).thenReturn(refreshJwt());
            when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.FALSE);
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.FALSE);
            // When / Then
            assertBusinessError(() -> authService.refreshToken(refreshTokenDTO()),
                CommonErrorCode.REFRESH_TOKEN_INVALID.getCode(), "登录状态已失效，请重新登录");
            verifyNoInteractions(userAccessService);
        }

        @Test
        @DisplayName("账号不存在：抛 18003 且不签发新令牌")
        void shouldThrowAccountDisabled_whenUserNotFound() {
            // Given
            when(jwtTokenProvider.parse(REFRESH_TOKEN_VALUE, JwtTokenProvider.TYPE_REFRESH)).thenReturn(refreshJwt());
            when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.FALSE);
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.TRUE);
            when(userAccessService.getUserById(USER_ID)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.refreshToken(refreshTokenDTO()),
                SystemErrorCode.ACCOUNT_DISABLED.getCode(), "账号已停用，请联系管理员");
            verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
        }

        @Test
        @DisplayName("账号已停用：抛 18003 且不签发新令牌")
        void shouldThrowAccountDisabled_whenUserDisabled() {
            // Given
            when(jwtTokenProvider.parse(REFRESH_TOKEN_VALUE, JwtTokenProvider.TYPE_REFRESH)).thenReturn(refreshJwt());
            when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.FALSE);
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI)))
                .thenReturn(Boolean.TRUE);
            when(userAccessService.getUserById(USER_ID)).thenReturn(disabledUser());
            // When / Then
            assertBusinessError(() -> authService.refreshToken(refreshTokenDTO()),
                SystemErrorCode.ACCOUNT_DISABLED.getCode(), "账号已停用，请联系管理员");
            verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
        }

        @Test
        @DisplayName("Redis 不可用降级：失效校验视为未失效、会话标记视为存在，刷新仍成功")
        void shouldReturnRefreshTokenVO_whenRedisUnavailable() {
            // Given
            when(jwtTokenProvider.parse(REFRESH_TOKEN_VALUE, JwtTokenProvider.TYPE_REFRESH)).thenReturn(refreshJwt());
            when(stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI)))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI)))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(jwtTokenProvider.createAccessToken(USER_ID, USERNAME, REFRESH_JTI)).thenReturn(accessToken());
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            // When
            RefreshTokenVO vo = authService.refreshToken(refreshTokenDTO());
            // Then
            assertThat(vo.accessToken()).isEqualTo(ACCESS_TOKEN_VALUE);
            verify(valueOperations).set(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI),
                "1", ACCESS_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Nested
    @DisplayName("登出")
    class LogoutTest {

        @BeforeEach
        void setUpLoginContext() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser(), null));
        }

        @Test
        @DisplayName("登出成功：删除 access 会话与 refresh 标记，refresh jti 加入失效集合并记录登出日志")
        void shouldClearSessionsAndReturnLogoutVO_whenLogout() {
            // Given
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtTokenProvider.getProperties()).thenReturn(new JwtProperties());
            // When
            LogoutVO vo = authService.logout();
            // Then
            assertThat(vo.logoutTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
            verify(stringRedisTemplate).delete(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI));
            verify(stringRedisTemplate).delete(RedisKeyConstants.sessionRefreshKey(USER_ID, REFRESH_JTI));
            verify(valueOperations).set(RedisKeyConstants.refreshInvalidKey(USER_ID, REFRESH_JTI),
                "1", REFRESH_TTL_SECONDS, TimeUnit.SECONDS);
            verify(sysLoginLogService).record(argThat(log -> log != null
                && log.getLoginType() == LoginTypeEnum.LOGOUT.getCode()
                && log.getResultStatus() == 1));
        }

        @Test
        @DisplayName("未登录：抛 10101 且不触碰 Redis 与令牌服务")
        void shouldThrowAuthExpired_whenNotLoggedIn() {
            // Given
            SecurityContextHolder.clearContext();
            // When / Then
            assertBusinessError(() -> authService.logout(),
                CommonErrorCode.AUTH_EXPIRED.getCode(), "未认证或登录已过期，请重新登录");
            verifyNoInteractions(stringRedisTemplate, jwtTokenProvider);
        }

        @Test
        @DisplayName("Redis 不可用降级：清理失败不阻断，仍返回登出 VO 并记录日志")
        void shouldReturnLogoutVO_whenRedisUnavailable() {
            // Given
            when(stringRedisTemplate.delete(anyString()))
                .thenThrow(new RedisConnectionFailureException("connection refused"));
            // When
            LogoutVO vo = authService.logout();
            // Then
            assertThat(vo.logoutTime()).isNotBlank();
            verify(sysLoginLogService).record(argThat(log -> log != null
                && log.getLoginType() == LoginTypeEnum.LOGOUT.getCode()));
            verifyNoInteractions(valueOperations);
        }
    }

    @Nested
    @DisplayName("当前用户信息")
    class CurrentUserTest {

        @BeforeEach
        void setUpLoginContext() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser(), null));
        }

        @Test
        @DisplayName("查询成功：手机号掩码、仓库 ID 解析、部门名与角色数据范围装配完整")
        void shouldReturnCurrentUserVO_whenSuccess() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(securityData());
            when(userAccessService.getDeptName(DEPT_ID)).thenReturn("研发部");
            // When
            CurrentUserVO vo = authService.currentUser();
            // Then
            assertThat(vo.userId()).isEqualTo(USER_ID);
            assertThat(vo.userCode()).isEqualTo("EMP001");
            assertThat(vo.username()).isEqualTo(USERNAME);
            assertThat(vo.realName()).isEqualTo("管理员");
            assertThat(vo.deptId()).isEqualTo(DEPT_ID);
            assertThat(vo.deptName()).isEqualTo("研发部");
            assertThat(vo.phone()).isEqualTo("138****5678");
            assertThat(vo.email()).isEqualTo("admin@erp.com");
            assertThat(vo.warehouseIds()).containsExactly(1L, 2L);
            assertThat(vo.roles()).containsExactly("admin");
            assertThat(vo.dataScope()).isEqualTo(1);
            assertThat(vo.lastLoginTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
            assertThat(vo.lastLoginIp()).isEqualTo("192.168.1.10");
        }

        @Test
        @DisplayName("用户不存在：抛 18005 且不再装配安全数据")
        void shouldThrowUserNotFound_whenUserNotExists() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.currentUser(),
                SystemErrorCode.USER_NOT_FOUND.getCode(), "用户 admin 不存在");
            verify(userAccessService, never()).loadByUserId(any());
        }

        @Test
        @DisplayName("安全数据缺失：角色为空、数据范围按仅本人（6），手机号 8 位边界掩码、仓库 ID 透传 null")
        void shouldReturnDefaults_whenSecurityDataMissing() {
            // Given
            SysUser stubUser = user();
            stubUser.setPhone("12345678");
            stubUser.setWarehouseIds(null);
            when(userAccessService.getUserById(USER_ID)).thenReturn(stubUser);
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(null);
            // When
            CurrentUserVO vo = authService.currentUser();
            // Then
            assertThat(vo.roles()).isEmpty();
            assertThat(vo.dataScope()).isEqualTo(6);
            assertThat(vo.warehouseIds()).isNull();
            assertThat(vo.phone()).isEqualTo("123****5678");
        }
    }

    @Nested
    @DisplayName("当前用户菜单树")
    class CurrentUserMenusTest {

        @BeforeEach
        void setUpLoginContext() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser(), null));
        }

        @Test
        @DisplayName("admin 用户：委托聚合服务并以 isAdmin=true 查询")
        void shouldDelegateWithAdminFlag_whenUserIsAdmin() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(adminUser());
            when(userAccessService.listMenuTree(USER_ID, true))
                .thenReturn(List.of(menuTreeVO(1L)));
            // When
            List<MenuTreeVO> menus = authService.currentUserMenus();
            // Then
            assertThat(menus).hasSize(1);
            assertThat(menus.get(0).getMenuId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("普通用户：委托聚合服务并以 isAdmin=false 查询")
        void shouldDelegateWithNonAdminFlag_whenUserIsNormal() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(userAccessService.listMenuTree(USER_ID, false)).thenReturn(List.of());
            // When
            List<MenuTreeVO> menus = authService.currentUserMenus();
            // Then
            assertThat(menus).isEmpty();
        }

        @Test
        @DisplayName("用户不存在：抛 18005 且不查询菜单")
        void shouldThrowUserNotFound_whenUserNotExists() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.currentUserMenus(),
                SystemErrorCode.USER_NOT_FOUND.getCode(), "用户 admin 不存在");
            verify(userAccessService, never()).listMenuTree(any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("当前用户权限点")
    class CurrentUserPermsTest {

        @BeforeEach
        void setUpLoginContext() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser(), null));
        }

        @Test
        @DisplayName("查询成功：返回权限点与角色编码集合")
        void shouldReturnUserPermsVO_whenSuccess() {
            // Given
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(securityData());
            // When
            UserPermsVO vo = authService.currentUserPerms();
            // Then
            assertThat(vo.perms()).containsExactly("system:user:list");
            assertThat(vo.roles()).containsExactly("admin");
        }

        @Test
        @DisplayName("安全数据缺失：抛 18005 用户不存在")
        void shouldThrowUserNotFound_whenSecurityDataMissing() {
            // Given
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.currentUserPerms(),
                SystemErrorCode.USER_NOT_FOUND.getCode(), "用户 admin 不存在");
        }
    }

    @Nested
    @DisplayName("修改本人密码")
    class ChangePasswordTest {

        @BeforeEach
        void setUpLoginContext() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser(), null));
        }

        /**
         * 改密成功链路公共打桩：账号存在、原口令匹配、新口令编码、Redis 存在待清理会话。
         */
        private void stubChangePasswordFlow(String newPassword) {
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(passwordEncoder.matches("Old@12345", ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("new-encoded-password");
            when(stringRedisTemplate.keys(RedisKeyConstants.SESSION_PREFIX + USER_ID + ":*"))
                .thenReturn(Set.of("system:session:1:old-jti"));
            when(stringRedisTemplate.keys(RedisKeyConstants.SESSION_REFRESH_PREFIX + USER_ID + ":*"))
                .thenReturn(Set.of("system:session:refresh:1:old-rjti"));
        }

        @Test
        @DisplayName("改密成功：更新口令与口令修改时间、删除全部会话并返回需重新登录")
        void shouldUpdatePasswordAndInvalidateSessions_whenSuccess() {
            // Given
            stubChangePasswordFlow("New@12345");
            // When
            ChangePasswordVO vo = authService.changePassword(changePasswordDTO());
            // Then
            assertThat(vo.passwordUpdateTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
            assertThat(vo.reLoginRequired()).isTrue();
            verify(userAccessService).updateUser(argThat(u -> u != null
                && "new-encoded-password".equals(u.getPassword())
                && u.getPasswordUpdateTime() != null));
            verify(stringRedisTemplate).delete(Set.of("system:session:1:old-jti"));
            verify(stringRedisTemplate).delete(Set.of("system:session:refresh:1:old-rjti"));
        }

        @Test
        @DisplayName("复杂度边界：8 位且恰好 3 类字符的新口令可通过校验")
        void shouldAcceptPassword_whenEightCharsWithThreeCategories() {
            // Given
            stubChangePasswordFlow("Abc12345");
            // When
            ChangePasswordVO vo = authService.changePassword(changePasswordDTO("Abc12345", "Abc12345"));
            // Then
            assertThat(vo.reLoginRequired()).isTrue();
            verify(userAccessService).updateUser(argThat(u -> u != null
                && "new-encoded-password".equals(u.getPassword())));
        }

        @Test
        @DisplayName("用户不存在：抛 18005 且不校验口令")
        void shouldThrowUserNotFound_whenUserNotExists() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(null);
            // When / Then
            assertBusinessError(() -> authService.changePassword(changePasswordDTO()),
                SystemErrorCode.USER_NOT_FOUND.getCode(), "用户 admin 不存在");
            verifyNoInteractions(passwordEncoder, stringRedisTemplate);
        }

        @Test
        @DisplayName("原密码错误：抛 18008 且不更新用户")
        void shouldThrowOldPasswordError_whenOldPasswordMismatch() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(passwordEncoder.matches("Old@12345", ENCODED_PASSWORD)).thenReturn(false);
            // When / Then
            assertBusinessError(() -> authService.changePassword(changePasswordDTO()),
                SystemErrorCode.OLD_PASSWORD_ERROR.getCode(), "原密码错误");
            verify(userAccessService, never()).updateUser(any());
            verifyNoInteractions(stringRedisTemplate);
        }

        @Test
        @DisplayName("新口令复杂度不足（8 位仅 2 类字符）：抛 18009 且不编码口令")
        void shouldThrowComplexityError_whenNewPasswordTooWeak() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(passwordEncoder.matches("Old@12345", ENCODED_PASSWORD)).thenReturn(true);
            // When / Then
            assertBusinessError(() -> authService.changePassword(changePasswordDTO("abcd1234", "abcd1234")),
                SystemErrorCode.PASSWORD_COMPLEXITY_ERROR.getCode(),
                "新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类");
            verify(passwordEncoder, never()).encode(anyString());
            verifyNoInteractions(stringRedisTemplate);
        }

        @Test
        @DisplayName("确认密码不一致：抛 10001 参数错误且不更新用户")
        void shouldThrowParamError_whenConfirmPasswordMismatch() {
            // Given
            when(userAccessService.getUserById(USER_ID)).thenReturn(user());
            when(passwordEncoder.matches("Old@12345", ENCODED_PASSWORD)).thenReturn(true);
            // When / Then
            assertBusinessError(() -> authService.changePassword(changePasswordDTO("New@12345", "New@54321")),
                CommonErrorCode.PARAM_ERROR.getCode(), "新密码与确认密码不一致");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userAccessService, never()).updateUser(any());
        }
    }
}
