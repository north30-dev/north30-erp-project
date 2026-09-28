package me.north30.erp.system.log;

import me.north30.erp.common.audit.AuditLogRecord;
import me.north30.erp.system.log.dto.AuditLogQueryDTO;
import me.north30.erp.system.log.dto.LoginLogQueryDTO;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.log.vo.AuditLogVO;

import java.time.LocalDateTime;

/**
 * 日志域测试数据静态工厂：集中构造审计日志记录/VO、登录日志实体与查询 DTO，避免测试方法内堆砌字段。
 */
public final class LogTestFactory {

    private LogTestFactory() {
    }

    /**
     * 构建 AuditLogRecord（字段透传，供各用例按需组合）。
     */
    public static AuditLogRecord record(String module, String bizType, String bizCode, String operateType,
                                        String operateDesc, String operateBy, LocalDateTime operateTime,
                                        String operateIp, String requestUri, String requestMethod,
                                        String beforeJson, String afterJson, Integer resultStatus,
                                        Integer errorCode, String errorMessage, Long costTime, String traceId) {
        return new AuditLogRecord(module, bizType, bizCode, operateType, operateDesc, operateBy, operateTime,
            operateIp, requestUri, requestMethod, beforeJson, afterJson, resultStatus, errorCode, errorMessage,
            costTime, traceId);
    }

    /**
     * 构建字段齐全的成功审计记录。
     */
    public static AuditLogRecord fullRecord() {
        return record("PURCHASE", "PURCHASE_ORDER", "PO202609001", "UPDATE", "SysRoleServiceImpl.update",
            "admin", LocalDateTime.of(2026, 9, 28, 10, 0, 0), "192.168.1.10",
            "/api/purchase/orders/1/approve", "POST", "{\"status\":0}", "{\"status\":1}",
            1, null, null, 25L, "trace-001");
    }

    /**
     * 构建全空字段审计记录（验证落库侧兜底逻辑）。
     */
    public static AuditLogRecord minimalRecord() {
        return record(null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null);
    }

    /**
     * 构建指定超长字段的失败审计记录（验证防御性截断），未指定的字段保持正常值。
     */
    public static AuditLogRecord recordWithOverflow(String bizCode, String operateDesc, String beforeJson,
                                                    String afterJson, String errorMessage, String traceId) {
        return record("PURCHASE", "PURCHASE_ORDER", bizCode, "UPDATE", operateDesc, "admin",
            LocalDateTime.of(2026, 9, 28, 10, 0, 0), "192.168.1.10", "/api/purchase/orders/1/approve", "POST",
            beforeJson, afterJson, 0, 10602, errorMessage, 25L, traceId);
    }

    /**
     * 构建审计日志 VO（含变更前后 JSON，用于详情 diff 测试）。
     */
    public static AuditLogVO auditLogVO(Long id, String beforeJson, String afterJson) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(id);
        vo.setModule("PURCHASE");
        vo.setBizType("PURCHASE_ORDER");
        vo.setBizCode("PO202609001");
        vo.setOperateType("UPDATE");
        vo.setOperateBy("admin");
        vo.setResultStatus(1);
        vo.setBeforeJson(beforeJson);
        vo.setAfterJson(afterJson);
        return vo;
    }

    /**
     * 构建审计日志分页查询入参。
     */
    public static AuditLogQueryDTO auditLogQueryDTO(Integer pageNum, Integer pageSize, String bizCode,
                                                    String module, String bizType, String operateType,
                                                    String operateBy, Integer resultStatus,
                                                    String startTime, String endTime) {
        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setBizCode(bizCode);
        query.setModule(module);
        query.setBizType(bizType);
        query.setOperateType(operateType);
        query.setOperateBy(operateBy);
        query.setResultStatus(resultStatus);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        return query;
    }

    /**
     * 构建登录日志实体（备注等审计字段为空）。
     */
    public static SysLoginLog loginLog(Long id, Long userId, String username, Integer loginType,
                                       LocalDateTime loginTime, String loginIp, String userAgent,
                                       Integer resultStatus, String failReason) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setId(id);
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginType(loginType);
        loginLog.setLoginTime(loginTime);
        loginLog.setLoginIp(loginIp);
        loginLog.setUserAgent(userAgent);
        loginLog.setResultStatus(resultStatus);
        loginLog.setFailReason(failReason);
        return loginLog;
    }

    /**
     * 构建登录日志分页查询入参。
     */
    public static LoginLogQueryDTO loginLogQueryDTO(Integer pageNum, Integer pageSize, String username,
                                                    Integer loginType, Integer resultStatus,
                                                    String startTime, String endTime) {
        LoginLogQueryDTO query = new LoginLogQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setUsername(username);
        query.setLoginType(loginType);
        query.setResultStatus(resultStatus);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        return query;
    }
}
