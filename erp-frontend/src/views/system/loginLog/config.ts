// 登录日志查询页（F7）列定义与域内枚举：列名与后端 LoginLogVO 字段一一对应，
// 事件类型/结果取值对齐后端 LoginLogQueryDTO/LoginLogVO 注释与 sys_login_log DDL。
// LOGIN_TYPE_* 为 sys_login_log 单表语义，保留域内；RESULT_* 为登录/审计日志跨域共用，
// 已收口至 src/constants（含反查 MAP）。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { LoginLogVO } from '../../../types/api'

/** 事件类型（sys_login_log.login_type：1-登录 2-登出 3-令牌刷新 4-登录失败） */
export const LOGIN_TYPE_LOGIN = 1
export const LOGIN_TYPE_LOGOUT = 2
export const LOGIN_TYPE_REFRESH = 3
export const LOGIN_TYPE_FAILED = 4

export const LOGIN_TYPE_OPTIONS = [
  { label: '登录', value: LOGIN_TYPE_LOGIN },
  { label: '登出', value: LOGIN_TYPE_LOGOUT },
  { label: '令牌刷新', value: LOGIN_TYPE_REFRESH },
  { label: '登录失败', value: LOGIN_TYPE_FAILED },
] as const

/** 事件类型反查（表格列渲染用） */
export const LOGIN_TYPE_MAP: Record<number, string> = {
  [LOGIN_TYPE_LOGIN]: '登录',
  [LOGIN_TYPE_LOGOUT]: '登出',
  [LOGIN_TYPE_REFRESH]: '令牌刷新',
  [LOGIN_TYPE_FAILED]: '登录失败',
}

/** 列定义（列名与后端 LoginLogVO 字段一一对应） */
export const LOGIN_LOG_COLUMNS: ColumnsType<LoginLogVO> = [
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '事件类型', dataIndex: 'loginType', width: 110 },
  { title: '事件时间', dataIndex: 'loginTime', width: 170 },
  { title: '来源 IP', dataIndex: 'loginIp', width: 140 },
  { title: '客户端 UA', dataIndex: 'userAgent', width: 260, ellipsis: true },
  { title: '结果', dataIndex: 'resultStatus', width: 90 },
  { title: '失败原因', dataIndex: 'failReason', width: 160, ellipsis: true },
]
