package me.north30.erp.system.log.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.log.mapper.SysLoginLogMapper;
import me.north30.erp.system.log.service.SysLoginLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录日志服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl implements SysLoginLogService {

    private final SysLoginLogMapper sysLoginLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(SysLoginLog loginLog) {
        sysLoginLogMapper.insert(loginLog);
    }
}
