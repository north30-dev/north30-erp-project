package me.north30.erp.common.util;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 查询参数守卫工具类
 */
public final class QueryGuardUtil {

    private QueryGuardUtil() {
    }

    /**
     * 确保实体不为空
     * 
     * @param entity 实体
     * @param errorCode 错误码
     * @return 实体
     */
    public static <T> T requirePresent(T entity, ErrorCode errorCode) {
        if (entity == null) {
            throw new BusinessException(errorCode);
        }
        return entity;
    }

    /**
     * 确保实体不为空
     * 
     * @param entity 实体
     * @param errorCode 错误码
     * @param message 错误信息
     * @return 实体
     */
    public static <T> T requirePresent(T entity, ErrorCode errorCode, String message) {
        if (entity == null) {
            throw new BusinessException(errorCode, message);
        }
        return entity;
    }

    /**
     * 确保实体为空
     * 
     * @param entity 实体
     * @param errorCode 错误码
     */
    public static void requireAbsent(Object entity, ErrorCode errorCode) {
        if (entity != null) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 确保实体为空
     * 
     * @param entity 实体
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void requireAbsent(Object entity, ErrorCode errorCode, String message) {
        if (entity != null) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 确保查询结果为0条
     * 
     * @param count 查询结果数量
     * @param errorCode 错误码
     */
    public static void requireCountZero(Long count, ErrorCode errorCode) {
        if (count == null || count != 0L) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 确保查询结果为0条
     * 
     * @param count 查询结果数量
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void requireCountZero(Long count, ErrorCode errorCode, String message) {
        if (count == null || count != 0L) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 确保查询结果大于0条
     * 
     * @param count 查询结果数量
     * @param errorCode 错误码
     */
    public static void requireCountPositive(Long count, ErrorCode errorCode) {
        if (count == null || count <= 0L) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 确保查询结果大于0条
     * 
     * @param count 查询结果数量
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void requireCountPositive(Long count, ErrorCode errorCode, String message) {
        if (count == null || count <= 0L) {
            throw new BusinessException(errorCode, message);
        }
    }
}
