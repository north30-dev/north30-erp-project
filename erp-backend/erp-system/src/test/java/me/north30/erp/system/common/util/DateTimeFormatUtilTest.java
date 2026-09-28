package me.north30.erp.system.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DateTimeFormatUtil} 纯工具类测试。
 */
class DateTimeFormatUtilTest {

    @Nested
    @DisplayName("format：格式化日期时间")
    class FormatTest {

        @Test
        @DisplayName("格式化 null 日期时间返回 null")
        void shouldReturnNull_whenTimeNull() {
            // When + Then：null 安全
            assertThat(DateTimeFormatUtil.format(null)).isNull();
        }

        @Test
        @DisplayName("格式化非 null 日期时间返回格式化后的字符串")
               void shouldFormat_whenTimeGiven() {
            // Given
            LocalDateTime time = LocalDateTime.of(2026, 9, 28, 10, 20, 30);

            // When
            String formatted = DateTimeFormatUtil.format(time);

            // Then：输出接口文档约定的 yyyy-MM-dd HH:mm:ss
            assertThat(formatted).isEqualTo("2026-09-28 10:20:30");
        }
    }
}
