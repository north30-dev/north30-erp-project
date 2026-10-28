/** 全局常量：值与后端 sys_menu / 接口文档对齐，禁止在页面里裸写魔法数字 */

/* ---------- 菜单类型（sys_menu.menu_type） ---------- */
export const MENU_TYPE_DIR = 1
export const MENU_TYPE_MENU = 2
export const MENU_TYPE_BUTTON = 3

export const MENU_TYPE_OPTIONS = [
  { label: '目录', value: MENU_TYPE_DIR },
  { label: '菜单', value: MENU_TYPE_MENU },
  { label: '按钮', value: MENU_TYPE_BUTTON },
] as const

/** 菜单类型反查（表格列渲染用） */
export const MENU_TYPE_MAP: Record<number, string> = {
  [MENU_TYPE_DIR]: '目录',
  [MENU_TYPE_MENU]: '菜单',
  [MENU_TYPE_BUTTON]: '按钮',
}

/** 菜单树最大深度（模块 → 子模块 → 页面 → 按钮，对齐后端 SysMenu.MAX_DEPTH） */
export const MAX_MENU_DEPTH = 4

/* ---------- 权限标识三段式（sys_menu.perms，模块:对象:动作） ---------- */

/** 模块段固定候选域（对齐后端模块划分） */
export const PERMS_MODULE_OPTIONS = [
  'system',
  'base',
  'purchase',
  'sales',
  'inventory',
  'manufacturing',
  'finance',
] as const

/** 动作段预置候选（允许自填，开放集合） */
export const PERMS_ACTION_SUGGESTIONS = [
  'list',
  'query',
  'create',
  'update',
  'delete',
  'export',
] as const

/** 单段合法格式（小写字母开头，小写字母/数字/中划线，与后端 PERMS_PATTERN 段语义一致） */
export const SEGMENT_PATTERN = /^[a-z][a-z0-9-]*$/

/* ---------- 菜单可见性（sys_menu.visible） ---------- */
export const VISIBLE_HIDDEN = 0
export const VISIBLE_SHOWN = 1

export const VISIBLE_OPTIONS = [
  { label: '显示', value: VISIBLE_SHOWN },
  { label: '隐藏', value: VISIBLE_HIDDEN },
] as const

/** 可见性反查（表格列渲染用） */
export const VISIBLE_MAP: Record<number, string> = {
  [VISIBLE_SHOWN]: '显示',
  [VISIBLE_HIDDEN]: '隐藏',
}

/* ---------- 组织部门类型（sys_dept.dept_type，对齐后端 DeptCreateDTO 注释） ---------- */
export const DEPT_TYPE_GROUP = 1
export const DEPT_TYPE_FACTORY = 2
export const DEPT_TYPE_BRANCH = 3
export const DEPT_TYPE_DEPT = 4
export const DEPT_TYPE_WORKSHOP = 5

export const DEPT_TYPE_OPTIONS = [
  { label: '集团', value: DEPT_TYPE_GROUP },
  { label: '生产基地', value: DEPT_TYPE_FACTORY },
  { label: '销售分公司', value: DEPT_TYPE_BRANCH },
  { label: '部门', value: DEPT_TYPE_DEPT },
  { label: '车间', value: DEPT_TYPE_WORKSHOP },
] as const

/** 组织类型反查（表格列渲染用） */
export const DEPT_TYPE_MAP: Record<number, string> = {
  [DEPT_TYPE_GROUP]: '集团',
  [DEPT_TYPE_FACTORY]: '生产基地',
  [DEPT_TYPE_BRANCH]: '销售分公司',
  [DEPT_TYPE_DEPT]: '部门',
  [DEPT_TYPE_WORKSHOP]: '车间',
}

/** 组织树最大深度（对齐后端层级上限 5 级，超限 18027） */
export const MAX_DEPT_DEPTH = 5

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

/* ---------- 数据字典（sys_dict_type / sys_dict_item，对齐后端 DictItemCreateDTO） ---------- */

/** 默认语言编码（后端缺省 zh-CN） */
export const DICT_LANG_DEFAULT = 'zh-CN'

export const IS_DEFAULT_NO = 0
export const IS_DEFAULT_YES = 1

export const IS_DEFAULT_OPTIONS = [
  { label: '否', value: IS_DEFAULT_NO },
  { label: '是', value: IS_DEFAULT_YES },
] as const

/* ---------- 性别（sys_user.gender） ---------- */
export const GENDER_UNKNOWN = 0
export const GENDER_MALE = 1
export const GENDER_FEMALE = 2

export const GENDER_OPTIONS = [
  { label: '未知', value: GENDER_UNKNOWN },
  { label: '男', value: GENDER_MALE },
  { label: '女', value: GENDER_FEMALE },
] as const

/* ---------- 数据范围（sys_role.data_scope，对齐后端 DataScopeEnum：合法取值 1~6、9） ---------- */
export const DATA_SCOPE_ALL = 1
export const DATA_SCOPE_ORG_AND_CHILDREN = 2
export const DATA_SCOPE_ORG_ONLY = 3
export const DATA_SCOPE_DEPT_AND_CHILDREN = 4
export const DATA_SCOPE_DEPT_ONLY = 5
export const DATA_SCOPE_SELF_ONLY = 6
export const DATA_SCOPE_CUSTOM = 9

export const DATA_SCOPE_OPTIONS = [
  { label: '全部', value: DATA_SCOPE_ALL },
  { label: '本组织及下级', value: DATA_SCOPE_ORG_AND_CHILDREN },
  { label: '本组织', value: DATA_SCOPE_ORG_ONLY },
  { label: '本部门及下级', value: DATA_SCOPE_DEPT_AND_CHILDREN },
  { label: '本部门', value: DATA_SCOPE_DEPT_ONLY },
  { label: '仅本人', value: DATA_SCOPE_SELF_ONLY },
  { label: '自定义', value: DATA_SCOPE_CUSTOM },
] as const

/** 数据范围反查（表格列渲染用） */
export const DATA_SCOPE_MAP: Record<number, string> = Object.fromEntries(
  DATA_SCOPE_OPTIONS.map((option) => [option.value, option.label]),
)
