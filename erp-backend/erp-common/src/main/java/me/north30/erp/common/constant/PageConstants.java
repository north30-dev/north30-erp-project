package me.north30.erp.common.constant;

/**
 * 分页常量：与 API 文档 1.4 分页规范一致。
 */
public final class PageConstants {

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 每页条数上限（超限抛 10001） */
    public static final int MAX_PAGE_SIZE = 200;

    private PageConstants() {
    }
}
