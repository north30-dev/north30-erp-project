package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 登录日志表（sys_login_log：登录/登出/刷新/失败四类事件）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

    /** 用户 ID（失败且用户不存在时为空） */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 事件类型 1-登录 2-登出 3-令牌刷新 4-登录失败 */
    private Integer loginType;

    /** 事件时间 */
    private LocalDateTime loginTime;

    /** 来源 IP */
    private String loginIp;

    /** 客户端 UA */
    private String userAgent;

    /** 结果 0-失败 1-成功 */
    private Integer resultStatus;

    /** 失败原因（账号或密码错误/账号锁定/账号停用） */
    private String failReason;
}
