// 参数配置页（F6）列定义与域内枚举：列名与后端 ConfigVO 字段一一对应，
// 值类型/内置标记取值对齐后端 SysConfig 注释与 ConfigCreateDTO 校验说明。
// 域内枚举暂存本文件，待主会话统一收口进公共 constants 后再迁移。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { ConfigVO } from '../../../types/api'

/** 值类型（sys_config.value_type：1-字符串 2-数字 3-布尔 4-JSON） */
export const VALUE_TYPE_STRING = 1
export const VALUE_TYPE_NUMBER = 2
export const VALUE_TYPE_BOOLEAN = 3
export const VALUE_TYPE_JSON = 4

export const VALUE_TYPE_OPTIONS = [
  { label: '字符串', value: VALUE_TYPE_STRING },
  { label: '数字', value: VALUE_TYPE_NUMBER },
  { label: '布尔', value: VALUE_TYPE_BOOLEAN },
  { label: 'JSON', value: VALUE_TYPE_JSON },
] as const

/** 值类型反查（表格列渲染用） */
export const VALUE_TYPE_MAP: Record<number, string> = {
  [VALUE_TYPE_STRING]: '字符串',
  [VALUE_TYPE_NUMBER]: '数字',
  [VALUE_TYPE_BOOLEAN]: '布尔',
  [VALUE_TYPE_JSON]: 'JSON',
}

/** 是否系统内置（sys_config.is_system：0-否 1-是，内置不可停用/删除） */
export const IS_SYSTEM_NO = 0
export const IS_SYSTEM_YES = 1

export const IS_SYSTEM_OPTIONS = [
  { label: '否', value: IS_SYSTEM_NO },
  { label: '是', value: IS_SYSTEM_YES },
] as const

/** 列定义（列名与后端 ConfigVO 字段一一对应） */
export const CONFIG_COLUMNS: ColumnsType<ConfigVO> = [
  { title: '参数键', dataIndex: 'configKey', width: 220 },
  { title: '参数名称', dataIndex: 'configName', width: 180 },
  { title: '参数值', dataIndex: 'configValue', width: 200, ellipsis: true },
  { title: '值类型', dataIndex: 'valueType', width: 90 },
  { title: '分组', dataIndex: 'configGroup', width: 130 },
  { title: '内置', dataIndex: 'isSystem', width: 80 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 160, ellipsis: true },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
]
