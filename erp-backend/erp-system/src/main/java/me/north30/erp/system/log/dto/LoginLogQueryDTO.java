package me.north30.erp.system.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 登录日志分页查询入参（接口文档 5.8.3）。
 *
 * @param pageNum     页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize    每页条数（1-200）
 * @param username    用户名（sys_login_log.username，模糊）
 * @param loginType   事件类型 1-登录 2-登出 3-令牌刷新 4-登录失败
 * @param resultStatus 结果 0-失败 1-成功
 * @param startTime   事件时间区间起点（含）
 * @param endTime     事件时间区间终点（不含）
 */
public record LoginLogQueryDTO(
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize,
    String username,
    Integer loginType,
    Integer resultStatus,
    String startTime,
    String endTime
) {
}
