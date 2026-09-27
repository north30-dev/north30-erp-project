package me.north30.erp.system.core.vo;

import java.time.LocalDateTime;

/**
 * 登录日志 VO（接口文档 5.8.3）。
 */
public record LoginLogVO(
    Long id,
    Long userId,
    String username,
    Integer loginType,
    LocalDateTime loginTime,
    String loginIp,
    String userAgent,
    Integer resultStatus,
    String failReason
) {
}
