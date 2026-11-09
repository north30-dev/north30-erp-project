// 审计日志查询页（F7）列定义与域内枚举：列名与后端 AuditLogVO 字段一一对应，
// 业务模块/操作类型取值对齐后端 AuditModuleEnum/OperateTypeEnum，业务类型为开放集合。
// 域内枚举暂存本文件，待主会话统一收口进公共 constants 后再迁移。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { AuditLogVO } from '../../../../types/api'

/** 业务模块（后端 AuditModuleEnum，落库为枚举名） */
export const AUDIT_MODULE_SYSTEM = 'SYSTEM'
export const AUDIT_MODULE_MATERIAL = 'MATERIAL'
export const AUDIT_MODULE_PURCHASE = 'PURCHASE'
export const AUDIT_MODULE_SALES = 'SALES'
export const AUDIT_MODULE_INVENTORY = 'INVENTORY'
export const AUDIT_MODULE_MANUFACTURING = 'MANUFACTURING'
export const AUDIT_MODULE_FINANCE = 'FINANCE'

export const AUDIT_MODULE_OPTIONS = [
  { label: '系统管理', value: AUDIT_MODULE_SYSTEM },
  { label: '物料/BOM', value: AUDIT_MODULE_MATERIAL },
  { label: '采购', value: AUDIT_MODULE_PURCHASE },
  { label: '销售', value: AUDIT_MODULE_SALES },
  { label: '库存', value: AUDIT_MODULE_INVENTORY },
  { label: '生产', value: AUDIT_MODULE_MANUFACTURING },
  { label: '财务', value: AUDIT_MODULE_FINANCE },
] as const

/** 业务模块反查（表格列渲染用） */
export const AUDIT_MODULE_MAP: Record<string, string> = Object.fromEntries(
  AUDIT_MODULE_OPTIONS.map((option) => [option.value, option.label]),
)

/** 操作类型（后端 OperateTypeEnum，落库为枚举名） */
export const OPERATE_TYPE_CREATE = 'CREATE'
export const OPERATE_TYPE_UPDATE = 'UPDATE'
export const OPERATE_TYPE_APPROVE = 'APPROVE'
export const OPERATE_TYPE_REJECT = 'REJECT'
export const OPERATE_TYPE_DELETE = 'DELETE'
export const OPERATE_TYPE_POST = 'POST'
export const OPERATE_TYPE_UNAPPROVE = 'UNAPPROVE'
export const OPERATE_TYPE_CLOSE = 'CLOSE'
export const OPERATE_TYPE_REOPEN = 'REOPEN'

export const OPERATE_TYPE_OPTIONS = [
  { label: '创建', value: OPERATE_TYPE_CREATE },
  { label: '修改', value: OPERATE_TYPE_UPDATE },
  { label: '审核', value: OPERATE_TYPE_APPROVE },
  { label: '驳回', value: OPERATE_TYPE_REJECT },
  { label: '删除', value: OPERATE_TYPE_DELETE },
  { label: '过账', value: OPERATE_TYPE_POST },
  { label: '反审核', value: OPERATE_TYPE_UNAPPROVE },
  { label: '关闭', value: OPERATE_TYPE_CLOSE },
  { label: '期间重开', value: OPERATE_TYPE_REOPEN },
] as const

/** 操作类型反查（表格列渲染用） */
export const OPERATE_TYPE_MAP: Record<string, string> = Object.fromEntries(
  OPERATE_TYPE_OPTIONS.map((option) => [option.value, option.label]),
)

/** 登录结果（sys_audit_log.result_status：0-失败 1-成功，语义与通用启停状态不同，域内定义） */
export const RESULT_FAILED = 0
export const RESULT_SUCCESS = 1

export const RESULT_STATUS_OPTIONS = [
  { label: '成功', value: RESULT_SUCCESS },
  { label: '失败', value: RESULT_FAILED },
] as const

/** 结果反查（表格列渲染用） */
export const RESULT_STATUS_MAP: Record<number, string> = {
  [RESULT_SUCCESS]: '成功',
  [RESULT_FAILED]: '失败',
}

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
