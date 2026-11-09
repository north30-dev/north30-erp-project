// 审计日志查询页（F7）列定义：列名与后端 AuditLogVO 字段一一对应，业务类型为开放集合。
// AUDIT_MODULE_*/OPERATE_TYPE_* 为跨模块通用语义、RESULT_* 为登录/审计日志跨域共用，
// 均已收口至 src/constants（含反查 MAP）。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { AuditLogVO } from '../../../types/api'

/** 列定义（列名与后端 AuditLogVO 字段一一对应，变更前后 JSON 走详情抽屉，不进列表） */
export const AUDIT_LOG_COLUMNS: ColumnsType<AuditLogVO> = [
  { title: '业务模块', dataIndex: 'module', width: 100 },
  { title: '业务类型', dataIndex: 'bizType', width: 150, ellipsis: true },
  { title: '单据号', dataIndex: 'bizCode', width: 150, ellipsis: true },
  { title: '操作类型', dataIndex: 'operateType', width: 100 },
  { title: '操作描述', dataIndex: 'operateDesc', width: 200, ellipsis: true },
  { title: '操作人', dataIndex: 'operateBy', width: 110 },
  { title: '操作时间', dataIndex: 'operateTime', width: 170 },
  { title: '结果', dataIndex: 'resultStatus', width: 80 },
  { title: '耗时(ms)', dataIndex: 'costTime', width: 100 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' },
]
