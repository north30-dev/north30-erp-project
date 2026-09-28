package me.north30.erp.system.user.service.impl;

import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysUserServiceImpl} 纯 Mockito 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private SysUserServiceImpl service;

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
