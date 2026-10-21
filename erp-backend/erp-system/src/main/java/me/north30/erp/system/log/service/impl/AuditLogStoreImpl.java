package me.north30.erp.system.log.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.audit.AuditLogRecord;
import me.north30.erp.common.audit.AuditLogStore;
import me.north30.erp.system.log.entity.SysAuditLog;
import me.north30.erp.system.log.mapper.SysAuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审计日志落库实现：将 common 切面构造的 {@link AuditLogRecord} 写入 sys_audit_log。
 * <p>操作人为空（无登录上下文/系统操作）时兜底为 anonymous；
 * 字段长度按表定义防御性截断，超长 JSON 追加截断标记。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogStoreImpl implements AuditLogStore {

    /** 无登录上下文时的兜底操作人 */
    private static final String ANONYMOUS = "anonymous";

    /** 超长 JSON 截断上限（TEXT 列无硬限制，防御异常大报文） */
    private static final int JSON_MAX_LENGTH = 10000;

    /** 超长截断标记 */
    private static final String TRUNCATED_MARKER = "...(truncated)";

    private final SysAuditLogMapper sysAuditLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void store(AuditLogRecord record) {
        SysAuditLog entity = new SysAuditLog();
        entity.setModule(defaultIfBlank(record.module(), "UNKNOWN"));
        entity.setBizType(record.bizType());
        entity.setBizCode(truncate(record.bizCode(), 50));
        entity.setOperateType(truncate(record.operateType(), 30));
        entity.setOperateDesc(truncate(record.operateDesc(), 200));
        entity.setOperateBy(truncate(defaultIfBlank(record.operateBy(), ANONYMOUS), 50));
        entity.setOperateTime(record.operateTime() != null ? record.operateTime() : LocalDateTime.now());
        entity.setOperateIp(truncate(record.operateIp(), 50));
        entity.setRequestUri(truncate(record.requestUri(), 200));
        entity.setRequestMethod(truncate(record.requestMethod(), 10));
        entity.setBeforeJson(truncate(record.beforeJson(), JSON_MAX_LENGTH));
        entity.setAfterJson(truncate(record.afterJson(), JSON_MAX_LENGTH));
        entity.setResultStatus(record.resultStatus() != null ? record.resultStatus() : 1);
        entity.setErrorCode(record.errorCode());
        entity.setErrorMessage(truncate(record.errorMessage(), 500));
        entity.setCostTime(record.costTime());
        entity.setTraceId(truncate(record.traceId(), 64));
        sysAuditLogMapper.insert(entity);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * 防御性截断：超长部分替换为截断标记，保证落库列长约束。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - TRUNCATED_MARKER.length()) + TRUNCATED_MARKER;
    }
}
