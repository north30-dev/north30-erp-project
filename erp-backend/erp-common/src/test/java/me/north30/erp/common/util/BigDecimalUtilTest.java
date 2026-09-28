package me.north30.erp.common.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BigDecimalUtil 单元测试：四则运算、除零防护、默认精度与 HALF_UP 舍入、nvl/isZero 边界条件。
 * <p>纯静态工具类，无需 Mockito，直接使用 AssertJ 断言。</p>
 */
@DisplayName("BigDecimalUtil 金额计算工具")
class BigDecimalUtilTest {

    @Nested
    @DisplayName("add：加法")
    class AddTest {

        @Test
        @DisplayName("两个非空金额相加返回精确和")
        void shouldReturnSum_whenBothAmountsPresent() {
            // Given
            BigDecimal a = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal b = BigDecimalUtilTestFactory.amount("2.25");

            // When
            BigDecimal result = BigDecimalUtil.add(a, b);

            // Then
            assertThat(result).isEqualByComparingTo("12.75");
        }

        @Test
        @DisplayName("任一操作数为 null 时按 0 处理")
        void shouldTreatNullOperandAsZero_whenOneOperandIsNull() {
            // Given
            BigDecimal a = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal b = null;

            // When
            BigDecimal result = BigDecimalUtil.add(a, b);

            // Then
            assertThat(result).isEqualByComparingTo("10.50");
        }

        @Test
        @DisplayName("两个操作数均为 null 时返回 0")
        void shouldReturnZero_whenBothOperandsAreNull() {
            // Given
            BigDecimal a = null;
            BigDecimal b = null;

            // When
            BigDecimal result = BigDecimalUtil.add(a, b);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("subtract：减法")
    class SubtractTest {

        @Test
        @DisplayName("两个非空金额相减返回精确差")
        void shouldReturnDifference_whenBothAmountsPresent() {
            // Given
            BigDecimal a = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal b = BigDecimalUtilTestFactory.amount("2.25");

            // When
            BigDecimal result = BigDecimalUtil.subtract(a, b);

            // Then
            assertThat(result).isEqualByComparingTo("8.25");
        }

        @Test
        @DisplayName("被减数为 null 时按 0 处理，允许返回负数")
        void shouldTreatNullMinuendAsZero_whenMinuendIsNull() {
            // Given
            BigDecimal a = null;
            BigDecimal b = BigDecimalUtilTestFactory.amount("2.25");

            // When
            BigDecimal result = BigDecimalUtil.subtract(a, b);

            // Then
            assertThat(result).isEqualByComparingTo("-2.25");
        }
    }

    @Nested
    @DisplayName("multiply：乘法")
    class MultiplyTest {

        @Test
        @DisplayName("单价乘数量返回精确积")
        void shouldReturnProduct_whenBothAmountsPresent() {
            // Given
            BigDecimal price = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal quantity = BigDecimalUtilTestFactory.amount("3");

            // When
            BigDecimal result = BigDecimalUtil.multiply(price, quantity);

            // Then
            assertThat(result).isEqualByComparingTo("31.50");
        }

        @Test
        @DisplayName("任一操作数为 null 时按 0 处理，积为 0")
        void shouldReturnZero_whenOneOperandIsNull() {
            // Given
            BigDecimal price = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal quantity = null;

            // When
            BigDecimal result = BigDecimalUtil.multiply(price, quantity);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("divide：除法（默认精度与指定精度重载）")
    class DivideTest {

        @Test
        @DisplayName("默认保留 6 位小数：1/3 = 0.333333")
        void shouldDivideWithDefaultScale6_whenScaleNotSpecified() {
            // Given
            BigDecimal dividend = BigDecimalUtilTestFactory.amount("1");
            BigDecimal divisor = BigDecimalUtilTestFactory.amount("3");

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor);

            // Then
            assertThat(result).isEqualByComparingTo("0.333333");
            assertThat(result.scale()).isEqualTo(BigDecimalUtil.DEFAULT_SCALE);
        }

        @Test
        @DisplayName("默认精度下按 HALF_UP 舍入：2/3 = 0.666667 而非 0.666666")
        void shouldRoundHalfUp_whenDivideWithDefaultScale() {
            // Given
            BigDecimal dividend = BigDecimalUtilTestFactory.amount("2");
            BigDecimal divisor = BigDecimalUtilTestFactory.amount("3");

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor);

            // Then
            assertThat(result).isEqualByComparingTo("0.666667");
        }

        @Test
        @DisplayName("除数为 0 时返回 0，不抛 ArithmeticException")
        void shouldReturnZero_whenDivisorIsZero() {
            // Given
            BigDecimal dividend = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal divisor = BigDecimalUtilTestFactory.amount("0");

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("除数为 null 时视同除零返回 0")
        void shouldReturnZero_whenDivisorIsNull() {
            // Given
            BigDecimal dividend = BigDecimalUtilTestFactory.amount("10.50");
            BigDecimal divisor = null;

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("指定精度 2 位：10/3 = 3.33")
        void shouldDivideWithCustomScale_whenScaleSpecified() {
            // Given
            BigDecimal dividend = BigDecimalUtilTestFactory.amount("10");
            BigDecimal divisor = BigDecimalUtilTestFactory.amount("3");

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor, 2);

            // Then
            assertThat(result).isEqualByComparingTo("3.33");
            assertThat(result.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("指定精度下按 HALF_UP 舍入：2/3 保留 2 位 = 0.67")
        void shouldRoundHalfUpAtCustomScale_whenScaleSpecified() {
            // Given
            BigDecimal dividend = BigDecimalUtilTestFactory.amount("2");
            BigDecimal divisor = BigDecimalUtilTestFactory.amount("3");

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor, 2);

            // Then
            assertThat(result).isEqualByComparingTo("0.67");
        }

        @Test
        @DisplayName("被除数为 null 且除数有效时返回 0")
        void shouldReturnZero_whenDividendIsNullAndDivisorValid() {
            // Given
            BigDecimal dividend = null;
            BigDecimal divisor = BigDecimalUtilTestFactory.amount("3");

            // When
            BigDecimal result = BigDecimalUtil.divide(dividend, divisor, 2);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("nvl：判空并返回默认值")
    class NvlTest {

        @Test
        @DisplayName("入参为 null 时返回 0")
        void shouldReturnZero_whenValueIsNull() {
            // Given
            BigDecimal value = null;

            // When
            BigDecimal result = BigDecimalUtil.nvl(value);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("入参非 null 时原样返回同一实例")
        void shouldReturnSameInstance_whenValueIsPresent() {
            // Given
            BigDecimal value = BigDecimalUtilTestFactory.amount("88.66");

            // When
            BigDecimal result = BigDecimalUtil.nvl(value);

            // Then
            assertThat(result).isSameAs(value);
        }
    }

    @Nested
    @DisplayName("isZero：判断是否为 null 或零")
    class IsZeroTest {

        @Test
        @DisplayName("入参为 null 时判定为零")
        void shouldReturnTrue_whenValueIsNull() {
            // Given
            BigDecimal value = null;

            // When
            boolean result = BigDecimalUtil.isZero(value);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不同标度的 0.00 判定为零（按 compareTo 语义比较）")
        void shouldReturnTrue_whenValueIsZeroWithDifferentScale() {
            // Given
            BigDecimal value = BigDecimalUtilTestFactory.amount("0.00");

            // When
            boolean result = BigDecimalUtil.isZero(value);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("非零正数判定为非零")
        void shouldReturnFalse_whenValueIsPositive() {
            // Given
            BigDecimal value = BigDecimalUtilTestFactory.amount("0.01");

            // When
            boolean result = BigDecimalUtil.isZero(value);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("非零负数判定为非零")
        void shouldReturnFalse_whenValueIsNegative() {
            // Given
            BigDecimal value = BigDecimalUtilTestFactory.amount("-0.01");

            // When
            boolean result = BigDecimalUtil.isZero(value);

            // Then
            assertThat(result).isFalse();
        }
    }
}
