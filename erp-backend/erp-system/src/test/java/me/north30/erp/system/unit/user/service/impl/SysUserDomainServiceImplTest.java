package me.north30.erp.system.unit.user.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.user.service.impl.SysUserDomainServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysUserDomainServiceImpl} 纯 Mockito 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysUserDomainServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysUserDomainServiceImpl service;

    @BeforeAll
    static void setUpTableInfo() {
        // 初始化 MP 表信息缓存，保证 Lambda 条件在纯单测环境可解析
        UserTestFactory.initTableInfo();
    }

    @Nested
    class GetByUsernameTest {

        @Test
        void shouldReturnUser_whenUsernameExists() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            when(sysUserMapper.selectOne(any())).thenReturn(user);

            // When
            SysUser result = service.getByUsername("zhangsan");

            // Then
            assertThat(result).isSameAs(user);
        }
    }

    @Nested
    class GetByIdTest {

        @Test
        void shouldReturnUser_whenIdExists() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            when(sysUserMapper.selectById(10L)).thenReturn(user);

            // When
            SysUser result = service.getById(10L);

            // Then
            assertThat(result).isSameAs(user);
        }
    }

    @AfterEach
    void clearSecurityContext() {
        // 避免自我操作守卫用例的登录态泄漏到其它用例
        SecurityContextHolder.clearContext();
    }

    @Nested
    class RequireNotSelfTest {

        @Test
        void shouldPass_whenCurrentUserDiffersFromTarget() {
            // Given：当前登录人 2L，目标用户 10L
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(2L, "zhangsan", "jti", "rjti"), null));

            // When / Then：不抛异常
            service.requireNotSelf(10L);
        }

        @Test
        void shouldThrow18041_whenCurrentUserIsTarget() {
            // Given：当前登录人即目标用户
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(10L, "zhangsan", "jti", "rjti"), null));

            // When / Then
            assertThatThrownBy(() -> service.requireNotSelf(10L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18041))
                .hasMessage("禁止对当前登录账号执行该操作");
        }

        @Test
        void shouldThrow10101_whenNoLoginContext() {
            // Given：无登录态
            SecurityContextHolder.clearContext();

            // When / Then
            assertThatThrownBy(() -> service.requireNotSelf(10L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.AUTH_EXPIRED.getCode()));
        }
    }

    @Nested
    class CreateUserTest {

        @Test
        void shouldReturnTrue_whenInsertAffected() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            when(sysUserMapper.insert(user)).thenReturn(1);

            // When
            boolean result = service.createUser(user);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenInsertNotAffected() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            when(sysUserMapper.insert(user)).thenReturn(0);

            // When
            boolean result = service.createUser(user);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class UpdateUserTest {

        @Test
        void shouldReturnTrue_whenUpdateAffected() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            when(sysUserMapper.updateById(user)).thenReturn(1);

            // When
            boolean result = service.updateUser(user);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenUpdateNotAffected() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            when(sysUserMapper.updateById(user)).thenReturn(0);

            // When
            boolean result = service.updateUser(user);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class UpdateLastLoginTest {

        @Test
        void shouldUpdateLoginInfoAndResetFailCount() {
            // Given
            LocalDateTime loginTime = UserTestFactory.TIME_LOGIN;

            // When
            service.updateLastLogin(10L, "127.0.0.1", loginTime);

            // Then
            verify(sysUserMapper).updateById(argThat((SysUser user) ->
                Long.valueOf(10L).equals(user.getId())
                    && "127.0.0.1".equals(user.getLastLoginIp())
                    && loginTime.equals(user.getLastLoginTime())
                    && Integer.valueOf(0).equals(user.getLoginFailCount())));
        }
    }
}
