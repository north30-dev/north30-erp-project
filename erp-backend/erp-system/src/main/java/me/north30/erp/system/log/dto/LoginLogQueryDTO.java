package me.north30.erp.system.log.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 登录日志分页查询入参（接口文档 5.8.3）。
 */
@Data
public class LoginLogQueryDTO {

    /** 页码（从 1 开始） */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum;

    /** 每页条数（1-200） */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    private Integer pageSize;

    /** 用户名（sys_login_log.username，模糊） */
    private String username;

    /** 事件类型 1-登录 2-登出 3-令牌刷新 4-登录失败（sys_login_log.login_type） */
    private Integer loginType;

    /** 结果 0-失败 1-成功（sys_login_log.result_status） */
    private Integer resultStatus;

    /** 事件时间区间起点（含） */
    private String startTime;

    /** 事件时间区间终点（不含） */
    private String endTime;
}
