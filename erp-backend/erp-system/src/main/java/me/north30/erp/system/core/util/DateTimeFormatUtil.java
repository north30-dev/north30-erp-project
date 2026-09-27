package me.north30.erp.system.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间格式化工具：输出接口文档约定的 "yyyy-MM-dd HH:mm:ss" 字符串。
 */
public final class DateTimeFormatUtil {

    /** 统一输出格式 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeFormatUtil() {
    }

    /**
     * 格式化时间为字符串，null 安全。
     */
    public static String format(LocalDateTime time) {
        return time == null ? null : FORMATTER.format(time);
    }
}
