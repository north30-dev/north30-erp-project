/** 全局常量：值与后端 sys_menu / 接口文档对齐，禁止在页面里裸写魔法数字 */

/* ---------- 菜单类型（sys_menu.menu_type） ---------- */
export const MENU_TYPE_DIR = 1
export const MENU_TYPE_MENU = 2
export const MENU_TYPE_BUTTON = 3

/* ---------- 通用状态 ---------- */
export const STATUS_ENABLED = 1
export const STATUS_DISABLED = 0

/* ---------- 分页默认值（与后端 PageConstants 一致：默认 20 条，上限 200 条） ---------- */
export const DEFAULT_PAGE_NUM = 1
export const DEFAULT_PAGE_SIZE = 20
export const MAX_PAGE_SIZE = 200

/* ---------- 常用状态文案映射 ---------- */
export const STATUS_OPTIONS = [
  { label: '启用', value: STATUS_ENABLED },
  { label: '停用', value: STATUS_DISABLED },
] as const

/** 状态反查（表格列渲染用） */
export const STATUS_MAP: Record<number, string> = {
  [STATUS_ENABLED]: '启用',
  [STATUS_DISABLED]: '停用',
}

/* ---------- 性别（sys_user.gender） ---------- */
export const GENDER_UNKNOWN = 0
export const GENDER_MALE = 1
export const GENDER_FEMALE = 2

export const GENDER_OPTIONS = [
  { label: '未知', value: GENDER_UNKNOWN },
  { label: '男', value: GENDER_MALE },
  { label: '女', value: GENDER_FEMALE },
] as const

/* ---------- 数据范围（sys_role.data_scope） ---------- */
export const DATA_SCOPE_ALL = 0
export const DATA_SCOPE_DEPT = 1
export const DATA_SCOPE_DEPT_AND_CHILD = 2

export const DATA_SCOPE_OPTIONS = [
  { label: '全部数据', value: DATA_SCOPE_ALL },
  { label: '本部门', value: DATA_SCOPE_DEPT },
  { label: '本部门及以下', value: DATA_SCOPE_DEPT_AND_CHILD },
] as const
