package me.north30.erp.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额计算工具：所有单价/金额/税率/折扣计算必须通过本工具，禁止 float/double。
 * <p>约定：金额字段数据库类型 DECIMAL(20,6)；除法默认保留 6 位小数、RoundingMode.HALF_UP。</p>
 */
public final class BigDecimalUtil {

    /** 默认除法精度：6 位小数 */
    public static final int DEFAULT_SCALE = 6;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private BigDecimalUtil() {
    }

    /**
     * 加法：null 视为 0
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nvl(a).add(nvl(b));
    }

    /**
     * 减法：null 视为 0
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nvl(a).subtract(nvl(b));
    }

    /**
     * 乘法：null 视为 0（结果精度随操作数，无需额外处理）
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return nvl(a).multiply(nvl(b));
    }

    /**
     * 除法：默认保留 6 位小数，RoundingMode.HALF_UP。
     * <p>除数为 null 或零时返回 0（除零属于业务校验问题，由调用方在业务层校验）。</p>
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return divide(dividend, divisor, DEFAULT_SCALE);
    }

    /**
     * 除法：指定精度，RoundingMode.HALF_UP。
     * <p>除数为 null 或零时返回 0。</p>
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (isZero(divisor)) {
            return ZERO;
        }
        return nvl(dividend).divide(nvl(divisor), scale, RoundingMode.HALF_UP);
    }

    /**
     * 判空并返回默认值
     */
    public static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }

    /**
     * 判断是否为 null 或零
     */
    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(ZERO) == 0;
    }
}
