package me.north30.erp.common.util;

import java.math.BigDecimal;

/**
 * BigDecimalUtilTest 测试数据工厂：统一从字符串构造金额测试值，避免浮点字面量精度污染。
 */
final class BigDecimalUtilTestFactory {

    private BigDecimalUtilTestFactory() {
    }

    /**
     * 从字符串构造金额测试值。
     * @param value 金额字符串，如 "10.50"
     * @return 对应的 BigDecimal
     */
    static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
