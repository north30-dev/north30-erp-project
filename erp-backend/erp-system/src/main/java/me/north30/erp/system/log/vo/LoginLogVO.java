package me.north30.erp.system.log.vo;

import java.time.LocalDateTime;

/**
 * 登录日志 VO（接口文档 5.8.3）。
 *
 * @param id           主键 ID
 * @param userId       用户 ID（失败且用户不存在时为空）
 * @param username     用户名
 * @param loginType    事件类型 1-登录 2-登出 3-令牌刷新 4-登录失败
 * @param loginTime    事件时间
 * @param loginIp      来源 IP
 * @param userAgent    客户端 UA
 * @param resultStatus 结果 0-失败 1-成功
 * @param failReason   失败原因（账号或密码错误/账号锁定/账号停用）
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
