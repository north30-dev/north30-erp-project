package me.north30.erp.common.util;

import org.springframework.dao.OptimisticLockingFailureException;

import me.north30.erp.common.exception.CommonErrorCode;

/**
 * 更新参数校验工具类
 */
public class UpdateGuardUtil {

    private UpdateGuardUtil() {
    }

    public static void checkOptimisticLockConflict(int updateCount) {
        if (updateCount == 0) {
            throw new OptimisticLockingFailureException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessage());
        }
        
    }

    

}
