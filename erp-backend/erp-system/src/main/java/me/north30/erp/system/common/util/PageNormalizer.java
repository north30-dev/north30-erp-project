package me.north30.erp.system.common.util;

import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

/**
 * 分页参数归一工具：统一各 ServiceImpl 重复的 normalizePageNum / normalizePageSize 防御二层逻辑。
 * <p>仅兜底缺省值与越界防御；HTTP 入口合法值校验由 DTO 注解 + Controller @Valid 负责（非法返回 400）。</p>
 */
public final class PageNormalizer {

    private PageNormalizer() {
    }

    /**
     * 归一页码：null 或 ≤0 回退默认页码。
     *
     * @param pageNum 页码（可空）
     * @return 归一后的页码
     */
    public static long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 归一页大小：null 或 ≤0 回退默认页大小；超过上限抛业务异常（防御绕过 HTTP 直调 Service 的场景）。
     *
     * @param pageSize 页大小（可空）
     * @return 归一后的页大小
     */
    public static long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "pageSize 不能超过 " + PageConstants.MAX_PAGE_SIZE);
        }
        return pageSize;
    }

    /**
     * 归一页码（严格版）：null 或 <1 回退默认页码。
     *
     * @param pageNum 页码（可空）
     * @return 归一后的页码
     */
    public static long normalizePageNumStrict(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 归一页大小（严格版）：null 回退默认页大小；越界（<1 或 >上限）抛业务异常。
     *
     * @param pageSize 页大小（可空）
     * @return 归一后的页大小
     */
    public static long normalizePageSizeStrict(Integer pageSize) {
        if (pageSize == null) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "每页条数须在 1-" + PageConstants.MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }
}
