package me.north30.erp.common.util;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间格式化工具：输出接口文档约定的 "yyyy-MM-dd HH:mm:ss" 字符串。
 */
public final class DateTimeFormatUtil {

    /** 统一输出格式 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 查询条件用日期格式 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateTimeFormatUtil() {
    }

    /**
     * 格式化时间为字符串，null 安全。
     */
    public static String format(LocalDateTime time) {
        return time == null ? null : FORMATTER.format(time);
    }

    /**
     * 解析查询时间条件：yyyy-MM-dd（按全天边界展开）或 yyyy-MM-dd HH:mm:ss；终点为右开区间。
     *
     * @param text         时间文本（可空，空返回 null 表示不过滤）
     * @param endExclusive true 表示作为区间终点（yyyy-MM-dd 展开为次日零点，右开）
     * @return 解析后的时间；text 为空返回 null
     */
    public static LocalDateTime parseQueryTime(String text, boolean endExclusive) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text, DATE_FORMATTER);
                return endExclusive ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
            }
            return LocalDateTime.parse(text, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "时间参数格式非法，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}
