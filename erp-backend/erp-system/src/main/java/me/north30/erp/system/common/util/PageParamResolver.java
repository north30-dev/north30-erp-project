package me.north30.erp.system.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.config.service.SysConfigDomainService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 分页参数归一组件：统一各 ServiceImpl 的 normalizePageNum / normalizePageSize 防御二层逻辑。
 * <p>缺省页大小与单页上限消费 sys_config 运行时参数（system.page.default.size / system.page.max.size），
 * 本地 60s TTL 缓存对齐参数管理"1 分钟内生效"承诺；DB 的 max.size 只能收紧不能放宽（生效上限 = min(配置, 200)，
 * 配置超 200 时按 200 走并 log.warn）。仅兜底缺省值与越界防御；HTTP 入口合法值校验由 DTO 注解 + Controller @Valid 负责（非法返回 400）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PageParamResolver {

    private static final String KEY_DEFAULT_SIZE = "system.page.default.size";
    private static final String KEY_MAX_SIZE = "system.page.max.size";

    /** 本地缓存 TTL：对齐"参数将在 1 分钟内生效" */
    private static final long CACHE_TTL_MS = 60_000L;

    private final SysConfigDomainService sysConfigDomainService;

    /** 缓存的缺省页大小（初始兜底常量，首次请求触发刷新） */
    private volatile long defaultSize = PageConstants.DEFAULT_PAGE_SIZE;

    /** 缓存的生效上限（API 契约上界，只能被配置收紧） */
    private volatile long maxSize = PageConstants.MAX_PAGE_SIZE;

    private volatile long loadedAt = 0L;

    /**
     * 归一页码：null 或 ≤0 回退默认页码。
     *
     * @param pageNum 页码（可空）
     * @return 归一后的页码
     */
    public long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 归一页大小：null 或 ≤0 回退配置的默认页大小；超过生效上限抛业务异常（防御绕过 HTTP 直调 Service 的场景）。
     *
     * @param pageSize 页大小（可空）
     * @return 归一后的页大小
     */
    public long normalizePageSize(Integer pageSize) {
        refreshIfExpired();
        if (pageSize == null || pageSize <= 0) {
            return defaultSize;
        }
        if (pageSize > maxSize) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "pageSize 不能超过 " + maxSize);
        }
        return pageSize;
    }

    /**
     * 归一页码（严格版）：null 或 <1 回退默认页码。
     *
     * @param pageNum 页码（可空）
     * @return 归一后的页码
     */
    public long normalizePageNumStrict(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 归一页大小（严格版）：null 回退配置的默认页大小；越界（<1 或 >生效上限）抛业务异常。
     *
     * @param pageSize 页大小（可空）
     * @return 归一后的页大小
     */
    public long normalizePageSizeStrict(Integer pageSize) {
        refreshIfExpired();
        if (pageSize == null) {
            return defaultSize;
        }
        if (pageSize < 1 || pageSize > maxSize) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "每页条数须在 1-" + maxSize + " 之间");
        }
        return pageSize;
    }

    /**
     * TTL 过期时批量刷新缓存的两个分页参数；无论刷新成功与否都推进时间戳，避免 DB 故障时每请求打库。
     */
    private synchronized void refreshIfExpired() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < CACHE_TTL_MS) {
            return;
        }
        try {
            Map<String, String> values = sysConfigDomainService.values(List.of(KEY_DEFAULT_SIZE, KEY_MAX_SIZE));
            applyDefaultSize(values.get(KEY_DEFAULT_SIZE));
            applyMaxSize(values.get(KEY_MAX_SIZE));
        } catch (Exception e) {
            log.warn("刷新分页参数缓存失败，沿用旧值 defaultSize={}, maxSize={}", defaultSize, maxSize, e);
        } finally {
            loadedAt = now;
        }
    }

    /**
     * 应用默认页大小（trim 后）。
     *
     * @param raw 原始字符串（可空）
     */
    private void applyDefaultSize(String raw) {
        if (raw == null) {
            return;
        }
        try {
            defaultSize = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 的值 {} 不是合法数字，沿用旧默认页大小 {}", KEY_DEFAULT_SIZE, raw, defaultSize);
        }
    }

    /**
     * 应用生效上限（trim 后）。
     *
     * @param raw 原始字符串（可空）
     */
    private void applyMaxSize(String raw) {
        if (raw == null) {
            return;
        }
        try {
            long configured = Long.parseLong(raw);
            if (configured > PageConstants.MAX_PAGE_SIZE) {
                log.warn("系统参数 {} 的值 {} 超过 API 契约上界 {}，按 {} 拦截",
                    KEY_MAX_SIZE, raw, PageConstants.MAX_PAGE_SIZE, PageConstants.MAX_PAGE_SIZE);
                return;
            }
            maxSize = configured;
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 的值 {} 不是合法数字，沿用旧上限 {}", KEY_MAX_SIZE, raw, maxSize);
        }
    }
}
