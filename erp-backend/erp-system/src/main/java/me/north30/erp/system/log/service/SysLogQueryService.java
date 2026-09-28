package me.north30.erp.system.log.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.log.dto.AuditLogQueryDTO;
import me.north30.erp.system.log.dto.LoginLogQueryDTO;
import me.north30.erp.system.log.vo.AuditLogVO;
import me.north30.erp.system.log.vo.LoginLogVO;

/**
 * 日志查询服务（SYS-01/SYS-05：审计日志只读查询 + 登录日志只读查询）。
 */
public interface SysLogQueryService {

    /**
     * 审计日志分页查询（接口文档 5.8.1，权限点 system:auditlog:list）。
     */
    PageResult<AuditLogVO> pageAuditLogs(AuditLogQueryDTO query);

    /**
     * 审计日志详情（接口文档 5.8.2，权限点 system:auditlog:detail），含变更前后 JSON 与变更字段清单。
     */
    AuditLogVO getAuditLogDetail(Long id);

    /**
     * 登录日志分页查询（接口文档 5.8.3，权限点 system:loginlog:list）。
     */
    PageResult<LoginLogVO> pageLoginLogs(LoginLogQueryDTO query);
}
