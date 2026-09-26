package me.north30.erp.common.result;

import java.util.List;

/**
 * 分页响应体：结构对齐 API 文档 1.3（list/total/pageNum/pageSize/pages）。
 * <p>不依赖 MyBatis-Plus 的 IPage，业务层自行转换，保持 common 模块零 MP 核心依赖。</p>
 *
 * @param <T> 记录类型
 * @param list 当前页记录
 * @param total 总记录数
 * @param pageNum 页码（从 1 开始）
 * @param pageSize 每页条数
 * @param pages 总页数
 */
public record PageResult<T>(
    List<T> list,
    long total,
    long pageNum,
    long pageSize,
    long pages
) {

    /**
     * 构建分页结果，自动计算总页数
     */
    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> list) {
        long safePageSize = pageSize <= 0 ? 1 : pageSize;
        long pages = (total + safePageSize - 1) / safePageSize;
        return new PageResult<>(list, total, pageNum, safePageSize, pages);
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(0, pageNum, pageSize, List.of());
    }
}
