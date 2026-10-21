/**
 * 全局接口契约类型（与后端 record 逐字段对齐）。
 * 命名规则：后端类名即前端类型名（编码规范第 2/4 节）。
 * 后端字段即前端字段，禁止重命名映射。
 */

/* ---------- 通用响应 ---------- */

/** 后端统一响应体（me.north30.erp.common.result.Result） */
export interface Result<T = unknown> {
  /** 业务状态码，200=成功 */
  code: number
  /** 提示信息 */
  message: string
  /** 业务数据 */
  data: T
  /** 响应时间戳（毫秒） */
  timestamp: number
  /** 链路追踪 ID */
  traceId?: string
}

/** 分页响应体（me.north30.erp.common.result.PageResult：list/total/pageNum/pageSize/pages） */
export interface PageResult<T> {
  /** 当前页记录 */
  list: T[]
  /** 总记录数 */
  total: number
  /** 页码（从 1 开始） */
  pageNum: number
  /** 每页条数 */
  pageSize: number
  /** 总页数 */
  pages: number
}

/* ---------- 认证授权（auth 域，接口文档 4.1-4.8） ---------- */

/** 图形验证码响应 VO（4.1） */
export interface CaptchaVO {
  captchaKey: string
  /** Base64 图片 */
  captchaImage: string
  expireSeconds: number
}

/** 登录请求 DTO（4.2） */
export interface LoginDTO {
  username: string
  password: string
  captcha: string
  captchaKey: string
}

/** 登录用户信息 */
export interface LoginUserInfoVO {
  userId: number
  username: string
  realName: string
  deptId: number | null
  deptName: string | null
  roles: string[]
  /** 是否管理员（0：否，1：是） */
  isAdmin: number
  /** 密码是否过期（0：否，1：是） */
  passwordExpired: number
}

/** 登录响应 VO（4.2） */
export interface LoginVO {
  accessToken: string
  tokenType: string
  /** 过期时间（秒） */
  expiresIn: number
  refreshToken: string
  userInfo: LoginUserInfoVO
}

/** 刷新令牌请求 DTO（4.3） */
export interface RefreshTokenDTO {
  refreshToken: string
}

/** 刷新令牌响应 VO（4.3） */
export interface RefreshTokenVO {
  accessToken: string
  tokenType: string
  expiresIn: number
}

/** 登出响应 VO（4.4） */
export interface LogoutVO {
  logoutTime: string
}

/** 当前用户信息响应 VO（4.5） */
export interface CurrentUserVO {
  userId: number
  userCode: string
  username: string
  realName: string
  deptId: number | null
  deptName: string | null
  phone: string | null
  email: string | null
  /** 数据范围（0：全部，1：本部门，2：本部门及以下部门） */
  dataScope: number
  lastLoginTime: string | null
  lastLoginIp: string | null
  roles: string[]
}

/** 菜单树节点 VO（4.6，sys_menu 树形） */
export interface MenuTreeVO {
  menuId: number
  menuName: string
  /** 类型（1：目录，2：菜单，3：按钮） */
  menuType: number
  parentId: number
  path: string | null
  component: string | null
  icon: string | null
  menuSort: number
  /** 是否可见（0：隐藏，1：显示） */
  visible: number
  perms: string | null
  children: MenuTreeVO[]
}

/** 当前用户权限点集合响应 VO（4.7） */
export interface UserPermsVO {
  perms: string[]
  roles: string[]
}

/** 修改本人密码请求 DTO（4.8） */
export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
}

/** 修改本人密码响应 VO（4.8） */
export interface ChangePasswordVO {
  passwordUpdateTime: string
  /** 是否需要重新登录（0：否，1：是） */
  reLoginRequired: number
}

/* ---------- 通用查询分页参数 ---------- */

/** 分页查询基础参数（各域 QueryDTO 均含这两项） */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
}
