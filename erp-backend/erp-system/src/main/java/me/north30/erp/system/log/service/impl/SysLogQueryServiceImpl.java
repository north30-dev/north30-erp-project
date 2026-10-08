package me.north30.erp.system.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.system.log.dto.AuditLogQueryDTO;
import me.north30.erp.system.log.dto.LoginLogQueryDTO;
import me.north30.erp.system.log.entity.SysAuditLog;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.log.converter.AuditLogConverter;
import me.north30.erp.system.log.converter.LoginLogConverter;
import me.north30.erp.system.log.mapper.SysAuditLogMapper;
import me.north30.erp.system.log.mapper.SysLoginLogMapper;
import me.north30.erp.system.log.service.SysLogQueryService;
import me.north30.erp.system.log.strategy.AuditLogDiffStrategy;
import me.north30.erp.system.log.vo.AuditLogVO;
import me.north30.erp.system.log.vo.LoginLogVO;
import me.north30.erp.system.common.util.PageNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志查询服务实现：审计日志与登录日志均为单表查询，统一走 LambdaQueryWrapper（接口文档 5.8/5.9）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogQueryServiceImpl implements SysLogQueryService {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final AuditLogConverter auditLogConverter;
    private final LoginLogConverter loginLogConverter;
    private final AuditLogDiffStrategy auditLogDiffStrategy;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> pageAuditLogs(AuditLogQueryDTO query) {
        long pageNum = PageNormalizer.normalizePageNumStrict(query.pageNum());
        long pageSize = PageNormalizer.normalizePageSizeStrict(query.pageSize());
        // 分页列表裁剪掉 before_json/after_json 大字段，仅详情接口返回
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<SysAuditLog>()
            .select(SysAuditLog::getId, SysAuditLog::getModule, SysAuditLog::getBizType,
                SysAuditLog::getBizCode, SysAuditLog::getOperateType, SysAuditLog::getOperateDesc,
                SysAuditLog::getOperateBy, SysAuditLog::getOperateTime, SysAuditLog::getOperateIp,
                SysAuditLog::getRequestUri, SysAuditLog::getRequestMethod, SysAuditLog::getResultStatus,
                SysAuditLog::getErrorCode, SysAuditLog::getErrorMessage, SysAuditLog::getCostTime,
                SysAuditLog::getTraceId)
            .like(StringUtils.hasText(query.bizCode()), SysAuditLog::getBizCode, query.bizCode())
            .eq(StringUtils.hasText(query.module()), SysAuditLog::getModule, query.module())
            .eq(StringUtils.hasText(query.bizType()), SysAuditLog::getBizType, query.bizType())
            .eq(StringUtils.hasText(query.operateType()), SysAuditLog::getOperateType, query.operateType())
            .like(StringUtils.hasText(query.operateBy()), SysAuditLog::getOperateBy, query.operateBy())
            .eq(query.resultStatus() != null, SysAuditLog::getResultStatus, query.resultStatus())
            .orderByDesc(SysAuditLog::getOperateTime)
            .orderByDesc(SysAuditLog::getId);
        LocalDateTime timeStart = DateTimeFormatUtil.parseQueryTime(query.startTime(), false);
        LocalDateTime timeEnd = DateTimeFormatUtil.parseQueryTime(query.endTime(), true);
        wrapper.ge(timeStart != null, SysAuditLog::getOperateTime, timeStart)
            .lt(timeEnd != null, SysAuditLog::getOperateTime, timeEnd);
        Page<SysAuditLog> page = sysAuditLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AuditLogVO> list = page.getRecords().stream().map(auditLogConverter::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogVO getAuditLogDetail(Long id) {
        SysAuditLog entity = sysAuditLogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(SystemManageErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        AuditLogVO detail = auditLogConverter.toVO(entity);
        return detail.withDiffFields(auditLogDiffStrategy.resolveDiffFields(detail.beforeJson(), detail.afterJson()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LoginLogVO> pageLoginLogs(LoginLogQueryDTO query) {
        long pageNum = PageNormalizer.normalizePageNumStrict(query.pageNum());
        long pageSize = PageNormalizer.normalizePageSizeStrict(query.pageSize());
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<SysLoginLog>()
            .like(StringUtils.hasText(query.username()), SysLoginLog::getUsername, query.username())
            .eq(query.loginType() != null, SysLoginLog::getLoginType, query.loginType())
            .eq(query.resultStatus() != null, SysLoginLog::getResultStatus, query.resultStatus())
            .orderByDesc(SysLoginLog::getLoginTime)
            .orderByDesc(SysLoginLog::getId);
        LocalDateTime timeStart = DateTimeFormatUtil.parseQueryTime(query.startTime(), false);
        LocalDateTime timeEnd = DateTimeFormatUtil.parseQueryTime(query.endTime(), true);
        wrapper.ge(timeStart != null, SysLoginLog::getLoginTime, timeStart)
            .lt(timeEnd != null, SysLoginLog::getLoginTime, timeEnd);
        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<LoginLogVO> list = page.getRecords().stream().map(loginLogConverter::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }
}
