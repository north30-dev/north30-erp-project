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
