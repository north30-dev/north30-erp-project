package me.north30.erp.system.auth.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.web.RequestContextUtil;
import me.north30.erp.system.common.enums.LoginTypeEnum;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.log.service.SysLoginLogService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录日志策略：登录/刷新/登出日志落库（独立写事务，失败不阻断主流程）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogStrategy {

    private final SysLoginLogService sysLoginLogService;

    /**
     * 记录登录日志（独立写事务，失败仅告警不阻断登录主流程，详见 SYS-01 降级约定）。
     */
    public void record(Long userId, String username, LoginTypeEnum loginType,
                       boolean success, String failReason) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setLoginType(loginType.getCode());
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setLoginIp(RequestContextUtil.resolveClientIp());
            loginLog.setUserAgent(RequestContextUtil.resolveUserAgent());
            loginLog.setResultStatus(success ? 1 : 0);
            loginLog.setFailReason(failReason);
            sysLoginLogService.record(loginLog);
        } catch (Exception e) {
            log.error("登录日志写入失败 | username: {} | type: {}", username, loginType, e);
        }
    }
}
