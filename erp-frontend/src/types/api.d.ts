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

/** 分页响应体（me.north30.erp.common.result.PageResult：list/total/pageNum/pageSize/pages；后端 Long 全局转 string） */
export interface PageResult<T> {
  /** 当前页记录 */
  list: T[]
  /** 总记录数（后端 Long 序列化为 string，展示时 Number() 收敛） */
  total: string
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
  userId: string
  username: string
  realName: string
  deptId: string | null
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
  userId: string
  userCode: string
  username: string
  realName: string
  deptId: string | null
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
  menuId: string
  menuName: string
  /** 类型（1：目录，2：菜单，3：按钮） */
  menuType: number
  parentId: string
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

/* ---------- 用户管理（user 域，对齐后端 SysUserController） ---------- */

/** 删除回执 VO（me.north30.erp.system.common.vo.DeleteResultVO） */
export interface DeleteResultVO {
  /** 删除记录数（后端 Long 序列化为 string） */
  count: string
}

/** 用户列表项 VO */
export interface UserVO {
  id: string
  userCode: string
  username: string
  realName: string
  phone: string | null
  email: string | null
  deptId: string | null
  deptName: string | null
  roles: string[]
  status: number
  /** 乐观锁版本号（状态切换时回传） */
  version: number
  isAdmin: number
  lastLoginTime: string | null
  createTime: string
}

/** 用户详情 VO */
export interface UserDetailVO {
  id: string
  userCode: string
  username: string
  realName: string
  phone: string | null
  email: string | null
  deptId: string | null
  deptName: string | null
  roles: string[]
  roleIds: string[] | null
  warehouseIds: string[] | null
  status: number
  /** 乐观锁版本号（编辑/状态切换时回传） */
  version: number
  isAdmin: number
  gender: number
  remark: string | null
  loginFailCount: number
  lockUntil: string | null
  passwordUpdateTime: string | null
  lastLoginTime: string | null
  createTime: string
}

/** 用户查询 DTO（GET 分页） */
export interface UserQueryDTO {
  username?: string
  realName?: string
  userCode?: string
  deptId?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 创建用户 DTO */
export interface UserCreateDTO {
  userCode: string
  username: string
  password: string
  realName: string
  deptId: string | null
  warehouseIds: string[] | null
  phone: string | null
  email: string | null
  gender: number
  status: number
  roleIds: string[] | null
  remark: string | null
}

/** 更新用户 DTO */
export interface UserUpdateDTO {
  realName: string
  deptId: string | null
  warehouseIds: string[] | null
  phone: string | null
  email: string | null
  gender: number
  status: number
  remark: string | null
  version: number
}

/** 更新用户状态 DTO */
export interface UserStatusDTO {
  status: number
  version: number
}

/** 更新用户状态响应 VO（5.1.6：变更后状态与会话是否已失效） */
export interface UserStatusVO {
  status: number
  sessionRevoked: boolean
}

/** 重置密码 DTO */
export interface UserResetPasswordDTO {
  newPassword: string
}

/** 重置密码响应 VO */
export interface UserResetPasswordVO {
  initialPassword: string
}

/** 分配角色 DTO */
export interface UserAssignRolesDTO {
  roleIds: string[] | null
}

/** 分配角色响应 VO */
export interface UserAssignRolesVO {
  roleIds: string[] | null
  /** 权限刷新提示 */
  permissionRefreshTip: string
}

/* ---------- 组织部门（dept 域，F1 仅作用户页下拉数据源，对齐后端 DeptTreeVO） ---------- */

/** 组织部门树节点 VO（5.4.1，children 递归） */
export interface DeptTreeVO {
  id: string
  deptCode: string
  deptName: string
  parentId: string
  /** 类型（1：集团，2：生产基地，3：销售分公司，4：部门，5：车间） */
  deptType: number
  /** 层级（1 为顶级，上限 5 级） */
  deptLevel: number
  /** 祖级路径（如 0,1,5） */
  ancestors: string
  leader: string | null
  phone: string | null
  deptSort: number
  status: number
  children: DeptTreeVO[] | null
}

/* ---------- 角色（role 域，对齐后端 SysRoleController /api/system/roles，接口文档 5.2） ---------- */

/** 角色分页查询 DTO（5.2.1） */
export interface RolePageQueryDTO {
  roleCode?: string
  roleName?: string
  status?: number
  pageNum?: number
  pageSize?: number
  orderBy?: string
  orderDirection?: string
}

/** 角色分页记录 VO（5.2.1，userCount 为派生计数；version/remark 供编辑弹窗回填） */
export interface RoleVO {
  id: string
  roleCode: string
  roleName: string
  roleSort: number
  dataScope: number
  isBuiltin: number
  status: number
  remark: string | null
  version: number
  userCount: number
}

/** 新增角色请求 DTO（5.2.2） */
export interface RoleCreateDTO {
  roleCode: string
  roleName: string
  roleSort?: number
  dataScope?: number
  status?: number
  remark?: string | null
}

/** 修改角色请求 DTO（5.2.3，role_code 不可修改，乐观锁 version 必填） */
export interface RoleUpdateDTO {
  roleName: string
  roleSort?: number
  dataScope?: number
  status?: number
  remark?: string | null
  version: number
}

/** 新增角色响应 VO（5.2.2，返回新角色 ID） */
export interface RoleCreatedVO {
  id: string
}

/** 修改角色响应 VO（5.2.3，返回 sys_role.update_time） */
export interface RoleUpdatedVO {
  updateTime: string | null
}

/** 数据范围明细项 VO（5.2.6/5.2.7 scopes[] 元素；Long 后端以字符串序列化） */
export interface RoleDataScopeItemVO {
  bizObject: string
  filterDimension: string
  scopeType: number
  deptIds: string[] | null
  userIds: string[] | null
  fieldMask: number | null
}

/** 数据范围明细项请求 DTO（5.2.6 scopes[] 元素） */
export interface RoleDataScopeItemDTO {
  bizObject: string
  filterDimension: string
  scopeType: number
  deptIds?: string[]
  userIds?: string[]
  fieldMask?: number
}

/** 配置角色数据范围请求 DTO（5.2.6，全量覆盖） */
export interface RoleDataScopeSaveDTO {
  scopes: RoleDataScopeItemDTO[]
}

/** 角色数据范围响应 VO（5.2.6/5.2.7） */
export interface RoleDataScopeVO {
  dataScope: number
  scopes: RoleDataScopeItemVO[]
}

/** 分配角色菜单权限请求 DTO（5.2.5，全量覆盖语义，空数组表示收回全部；Long 后端以字符串强转） */
export interface RoleAssignMenuDTO {
  menuIds: string[]
}

/** 分配角色菜单权限响应 VO（5.2.5） */
export interface RoleMenuAssignedVO {
  menuIds: string[]
  permCount: number
}

/** 角色已分配菜单 ID 响应 VO（5.2.8，分配菜单弹窗回显用） */
export interface RoleMenuIdsVO {
  menuIds: string[]
}
