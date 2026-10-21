package me.north30.erp.system.unit.user.entity;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.user.entity.SysUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SysUser} 充血方法纯单元测试：直接 new 实体，不依赖 Spring 容器。
 */
class SysUserTest {

    @Test
    @DisplayName("changeStatus 时，停用内置管理员抛 18012")
    void shouldThrow18012_whenDisableAdmin() {
        // Given
        SysUser admin = new SysUser();
        admin.setIsAdmin(1);
        admin.setStatus(1);

        // When / Then
        assertThatThrownBy(() -> admin.changeStatus(0))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(18012))
            .hasMessage("内置超级管理员不可删除或停用");
        assertThat(admin.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("changeStatus 时，普通用户正常赋值状态")
    void shouldAssignStatus_whenNormalUser() {
        // Given
        SysUser user = new SysUser();
        user.setIsAdmin(0);

        // When
        user.changeStatus(0);

        // Then
        assertThat(user.getStatus()).isZero();
    }

    @Test
    @DisplayName("changeStatus 时，null 状态触发值域守卫抛 10001")
    void shouldThrow10001_whenStatusNull() {
        // Given：取值合法性守卫已下沉实体，null/越界值均抛 10001（优先于管理员守卫）
        SysUser admin = new SysUser();
        admin.setIsAdmin(1);

        // When / Then
        assertThatThrownBy(() -> admin.changeStatus(null))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(10001))
            .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
        assertThat(admin.getStatus()).isNull();
    }

    @Test
    @DisplayName("changeStatus 时，越界状态触发值域守卫抛 10001")
    void shouldThrow10001_whenStatusOutOfRange() {
        // Given
        SysUser user = new SysUser();
        user.setIsAdmin(0);

        // When / Then
        assertThatThrownBy(() -> user.changeStatus(2))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(10001))
            .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
        assertThat(user.getStatus()).isNull();
    }

    @Test
    @DisplayName("changeStatus 时，启用管理员不触发守卫")
    void shouldNotThrow_whenEnableAdmin() {
        // Given
        SysUser admin = new SysUser();
        admin.setIsAdmin(1);

        // When / Then
        assertThatCode(() -> admin.changeStatus(1)).doesNotThrowAnyException();
        assertThat(admin.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("initPassword 时，复杂度不足抛 18009 且不赋值")
    void shouldThrow18009_whenWeakPassword() {
        // Given
        SysUser user = new SysUser();
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        // When / Then
        assertThatThrownBy(() -> user.initPassword("123", encoder, LocalDateTime.now()))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(18009))
            .hasMessage("新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类");
        assertThat(user.getPassword()).isNull();
        assertThat(user.getPasswordUpdateTime()).isNull();
    }

    @Test
    @DisplayName("initPassword 时，口令加密赋值并记录口令修改时间")
    void shouldEncodeAndAssign_whenStrongPassword() {
        // Given
        SysUser user = new SysUser();
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("encodedPwd");
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 8, 0);

        // When
        user.initPassword("Passw0rd!", encoder, now);

        // Then
        assertThat(user.getPassword()).isEqualTo("encodedPwd");
        assertThat(user.getPasswordUpdateTime()).isEqualTo(now);
    }
}
