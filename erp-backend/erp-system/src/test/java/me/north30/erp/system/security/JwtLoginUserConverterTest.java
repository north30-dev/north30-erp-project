package me.north30.erp.system.security;

import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.system.auth.service.UserAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.ACCESS_JTI;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.REFRESH_JTI;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.USER_ID;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.USERNAME;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.accessJwt;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.disabledSecurityData;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.jwtWithInvalidSubject;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.jwtWithType;
import static me.north30.erp.system.security.JwtLoginUserConverterTestFactory.securityData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * JwtLoginUserConverter 单元测试：JWT → Authentication 转换的类型校验、会话校验、
 * 账号状态校验与角色/权限点装载。
 */
@ExtendWith(MockitoExtension.class)
class JwtLoginUserConverterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private UserAccessService userAccessService;

    @InjectMocks
    private JwtLoginUserConverter converter;

    @Nested
    @DisplayName("JWT 转换为认证令牌")
    class ConvertTest {

        @Test
        @DisplayName("合法 access 会话：装载角色（ROLE_ 前缀）与权限点为 GrantedAuthority，principal 为 LoginUser")
        void shouldReturnAuthentication_whenAccessSessionValid() {
            // Given
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI)))
                .thenReturn(Boolean.TRUE);
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(securityData());
            // When
            AbstractAuthenticationToken token = converter.convert(accessJwt());
            // Then
            assertThat(token).isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(token.isAuthenticated()).isTrue();
            assertThat(token.getPrincipal()).isEqualTo(new LoginUser(USER_ID, USERNAME, ACCESS_JTI, REFRESH_JTI));
            assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_manager", "system:user:list", "system:user:add");
        }

        @Test
        @DisplayName("type 非 access（refresh token 访问业务接口）：拒绝认证且不触发任何查询")
        void shouldReject_whenTokenTypeNotAccess() {
            // When / Then
            assertThatThrownBy(() -> converter.convert(jwtWithType(JwtTokenProvider.TYPE_REFRESH)))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessage("令牌类型不匹配，禁止访问业务接口");
            verifyNoInteractions(stringRedisTemplate, userAccessService);
        }

        @Test
        @DisplayName("subject 非数字（身份信息无效）：拒绝认证且不触发任何查询")
        void shouldReject_whenSubjectNotNumber() {
            // When / Then
            assertThatThrownBy(() -> converter.convert(jwtWithInvalidSubject()))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessage("令牌身份信息无效");
            verifyNoInteractions(stringRedisTemplate, userAccessService);
        }

        @Test
        @DisplayName("会话白名单不存在：拒绝认证且不装载权限")
        void shouldReject_whenSessionNotExists() {
            // Given
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI)))
                .thenReturn(Boolean.FALSE);
            // When / Then
            assertThatThrownBy(() -> converter.convert(accessJwt()))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessage("登录会话不存在或已失效");
            verify(userAccessService, never()).loadByUserId(any());
        }

        @Test
        @DisplayName("会话白名单查询返回 null（Redis 异常边界）：同样拒绝认证")
        void shouldReject_whenSessionCheckReturnsNull() {
            // Given
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI)))
                .thenReturn(null);
            // When / Then
            assertThatThrownBy(() -> converter.convert(accessJwt()))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessage("登录会话不存在或已失效");
            verify(userAccessService, never()).loadByUserId(any());
        }

        @Test
        @DisplayName("用户不存在（安全数据为 null）：抛 DisabledException")
        void shouldThrowDisabled_whenUserNotExists() {
            // Given
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI)))
                .thenReturn(Boolean.TRUE);
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(null);
            // When / Then
            assertThatThrownBy(() -> converter.convert(accessJwt()))
                .isInstanceOf(DisabledException.class)
                .hasMessage("账号已停用或不存在");
        }

        @Test
        @DisplayName("账号已停用（status=0）：抛 DisabledException")
        void shouldThrowDisabled_whenUserDisabled() {
            // Given
            when(stringRedisTemplate.hasKey(RedisKeyConstants.sessionKey(USER_ID, ACCESS_JTI)))
                .thenReturn(Boolean.TRUE);
            when(userAccessService.loadByUserId(USER_ID)).thenReturn(disabledSecurityData());
            // When / Then
            assertThatThrownBy(() -> converter.convert(accessJwt()))
                .isInstanceOf(DisabledException.class)
                .hasMessage("账号已停用或不存在");
        }
    }
}
