package me.north30.erp.common.util;

/**
 * 数据脱敏工具（纯静态，无 Spring 依赖）。
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    /**
     * 手机号掩码：保留前 3 后 4（S-06）；null 或长度不足 8 时原样返回。
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
