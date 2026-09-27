package me.north30.erp.system.core.service;

import me.north30.erp.system.core.entity.SysLoginLog;

/**
 * 登录日志服务接口（SYS-01：登录/登出/刷新/失败四类事件）。
 */
public interface ISysLoginLogService {

    /**
     * 记录登录事件。
     */
    void record(SysLoginLog loginLog);
}
