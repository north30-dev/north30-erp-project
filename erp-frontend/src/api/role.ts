import { get } from '../utils/request'
import type { PageResult, RolePageQueryDTO, RoleVO } from '../types/api'

/** 角色接口（对应后端 SysRoleController /api/system/roles，F1 仅消费分页查询作分配角色下拉数据源） */

export function getRolePage(query: RolePageQueryDTO): Promise<PageResult<RoleVO>> {
  return get<PageResult<RoleVO>>('/system/roles', query as Record<string, unknown>).then((r) => r.data)
}
