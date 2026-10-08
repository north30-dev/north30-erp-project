package me.north30.erp.system.user.strategy;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.SystemErrorCode;

import java.security.SecureRandom;
import java.util.List;

/**
 * 用户口令复杂度策略（纯函数，无 Spring 依赖，可直接单测）。
 * <p>全系统唯一口令复杂度规则：长度 ≥8 且至少包含大写字母、小写字母、数字、
 * 特殊字符中的 3 类。认证链（修改本人密码）与用户管理链（创建/重置口令）共用本类。</p>
 */
public final class PasswordStrategy {

    /** 口令最小长度 */
    public static final int MIN_LENGTH = 8;

    /** 至少需要的字符类别数（大写/小写/数字/特殊字符） */
    public static final int MIN_CATEGORIES = 3;

    /** 随机初始口令长度（须满足复杂度：4 类字符各至少 1 个） */
    private static final int RANDOM_PASSWORD_LENGTH = 12;

    private static final String PASSWORD_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String PASSWORD_LOWER = "abcdefghijklmnopqrstuvwxyz";

    private static final String PASSWORD_DIGIT = "0123456789";

    private static final String PASSWORD_SPECIAL = "!@#$%^&*";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordStrategy() {
    }

    /**
     * 校验口令是否满足复杂度要求。
     *
     * @param password 待校验的明文口令（可为 null）
     * @return 满足返回 true；null、长度不足或类别数不足返回 false
     */
    public static boolean matches(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        int categories = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isLowerCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            categories++;
        }
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            categories++;
        }
        return categories >= MIN_CATEGORIES;
    }

    /**
     * 校验口令复杂度，不满足直接抛业务异常 18009（服务/实体编排用）。
     *
     * @param password 待校验的明文口令
     */
    public static void checkOrThrow(String password) {
        if (!matches(password)) {
            throw new BusinessException(SystemErrorCode.PASSWORD_COMPLEXITY_ERROR);
        }
    }

    /**
     * 生成随机初始口令：12 位，4 类字符各至少 1 个，Fisher-Yates 打乱避免固定模式。
     *
     * @return 满足复杂度要求的随机明文口令
     */
    public static String generate() {
        List<String> categories = List.of(PASSWORD_UPPER, PASSWORD_LOWER, PASSWORD_DIGIT, PASSWORD_SPECIAL);
        StringBuilder sb = new StringBuilder();
        for (String category : categories) {
            sb.append(category.charAt(SECURE_RANDOM.nextInt(category.length())));
        }
        while (sb.length() < RANDOM_PASSWORD_LENGTH) {
            String category = categories.get(SECURE_RANDOM.nextInt(categories.size()));
            sb.append(category.charAt(SECURE_RANDOM.nextInt(category.length())));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
