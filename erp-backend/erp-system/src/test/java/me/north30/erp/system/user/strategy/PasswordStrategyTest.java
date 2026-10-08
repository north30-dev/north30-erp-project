package me.north30.erp.system.user.strategy;

import me.north30.erp.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PasswordStrategy} 纯单元测试：口令复杂度规则（长度与字符类别数）。
 */
class PasswordStrategyTest {

    @Test
    @DisplayName("校验口令复杂度时，null 或长度不足 8 位返回 false")
    void shouldReturnFalse_whenNullOrTooShort() {
        assertThat(PasswordStrategy.matches(null)).isFalse();
        assertThat(PasswordStrategy.matches("Ab1@")).isFalse();
        assertThat(PasswordStrategy.matches("Ab1@ab1")).isFalse();
    }

    @Test
    @DisplayName("校验口令复杂度时，长度足够但类别数不足 3 类返回 false")
    void shouldReturnFalse_whenCategoriesInsufficient() {
        // 纯小写 + 数字 = 2 类
        assertThat(PasswordStrategy.matches("abcdefg1")).isFalse();
        // 纯大写 + 数字 = 2 类
        assertThat(PasswordStrategy.matches("ABCDEFG1")).isFalse();
        // 小写 + 特殊字符 = 2 类
        assertThat(PasswordStrategy.matches("abcdefg@")).isFalse();
    }

    @Test
    @DisplayName("校验口令复杂度时，长度足够且含 3 类返回 true")
    void shouldReturnTrue_whenThreeCategoriesPresent() {
        assertThat(PasswordStrategy.matches("Abcdefg1")).isTrue();
        assertThat(PasswordStrategy.matches("abcdefg1@")).isTrue();
        assertThat(PasswordStrategy.matches("ABCDEFG1@")).isTrue();
    }

    @Test
    @DisplayName("校验口令复杂度时，4 类齐全返回 true")
    void shouldReturnTrue_whenAllCategoriesPresent() {
        assertThat(PasswordStrategy.matches("Admin@123456")).isTrue();
    }

    @Test
    @DisplayName("checkOrThrow 时，不满足复杂度抛 18009 业务异常")
    void shouldThrow18009_whenCheckOrThrowWeakPassword() {
        assertThatThrownBy(() -> PasswordStrategy.checkOrThrow("123"))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(18009))
            .hasMessage("新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类");
    }

    @Test
    @DisplayName("checkOrThrow 时，满足复杂度不抛异常")
    void shouldNotThrow_whenCheckOrThrowStrongPassword() {
        assertThatCode(() -> PasswordStrategy.checkOrThrow("Abcdefg1@")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("generate 时，生成 12 位且 4 类字符各至少 1 个的口令")
    void shouldGenerate12CharPasswordWithAllCategories() {
        String password = PasswordStrategy.generate();
        assertThat(password).hasSize(12)
            .matches(".*[A-Z].*")
            .matches(".*[a-z].*")
            .matches(".*\\d.*")
            .matches(".*[!@#$%^&*].*");
        // 生成口令自身满足复杂度规则
        assertThat(PasswordStrategy.matches(password)).isTrue();
    }
}
