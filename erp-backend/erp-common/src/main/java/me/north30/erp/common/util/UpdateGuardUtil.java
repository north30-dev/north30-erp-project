package me.north30.erp.common.util;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

/**
 * 更新参数校验工具类
 */
public class UpdateGuardUtil {

    private UpdateGuardUtil() {
    }

    public static void checkOptimisticLockConflict(int updateCount) {
        if (updateCount == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        
    }

}
