import { get } from '../utils/request'
import type {
  AuditLogQueryDTO,
  AuditLogVO,
  LoginLogQueryDTO,
  LoginLogVO,
  PageResult,
} from '../types/api'

/**
 * 日志查询接口（对应后端 LoginLogController /api/system/login-logs 与 AuditLogController /api/system/audit-logs，接口文档 5.8）。
 * 日志只增不改不删，仅提供只读查询，无新增/修改/删除接口。
 */

/** 登录日志分页查询（5.8.3，权限点 system:loginlog:list） */
export function getLoginLogPage(query: LoginLogQueryDTO): Promise<PageResult<LoginLogVO>> {
  return get<PageResult<LoginLogVO>>('/system/login-logs', query as Record<string, unknown>).then((r) => r.data)
}

/** 审计日志分页查询（5.8.1，权限点 system:auditlog:list；分页列表裁剪变更前后 JSON 大字段） */
export function getAuditLogPage(query: AuditLogQueryDTO): Promise<PageResult<AuditLogVO>> {
  return get<PageResult<AuditLogVO>>('/system/audit-logs', query as Record<string, unknown>).then((r) => r.data)
}

/** 审计日志详情（5.8.2，权限点 system:auditlog:detail，含变更前后 JSON 与变更字段清单；不存在 18037） */
export function getAuditLogDetail(id: string): Promise<AuditLogVO> {
  return get<AuditLogVO>(`/system/audit-logs/${id}`).then((r) => r.data)
}
