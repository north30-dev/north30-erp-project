import { del, get, post, put } from '../utils/request'
import type {
  DeleteResultVO,
  PageResult,
  RoleAssignMenuDTO,
  RoleCreatedVO,
  RoleCreateDTO,
  RoleDataScopeSaveDTO,
  RoleDataScopeVO,
  RoleMenuAssignedVO,
  RoleMenuIdsVO,
  RolePageQueryDTO,
  RoleUpdateDTO,
  RoleUpdatedVO,
  RoleVO,
} from '../types/api'

/** 角色管理接口（对应后端 SysRoleController /api/system/roles，接口文档 5.2） */

export function getRolePage(query: RolePageQueryDTO): Promise<PageResult<RoleVO>> {
  return get<PageResult<RoleVO>>('/system/roles', query as Record<string, unknown>).then((r) => r.data)
}

export function createRole(data: RoleCreateDTO): Promise<RoleCreatedVO> {
  return post<RoleCreatedVO>('/system/roles', data).then((r) => r.data)
}

export function updateRole(id: string, data: RoleUpdateDTO): Promise<RoleUpdatedVO> {
  return put<RoleUpdatedVO>(`/system/roles/${id}`, data).then((r) => r.data)
}

export function deleteRole(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/roles/${id}`).then((r) => r.data)
}

export function assignRoleMenus(id: string, data: RoleAssignMenuDTO): Promise<RoleMenuAssignedVO> {
  return put<RoleMenuAssignedVO>(`/system/roles/${id}/menus`, data).then((r) => r.data)
}

export function getRoleMenuIds(id: string): Promise<RoleMenuIdsVO> {
  return get<RoleMenuIdsVO>(`/system/roles/${id}/menus`).then((r) => r.data)
}

export function getRoleDataScopes(id: string): Promise<RoleDataScopeVO> {
  return get<RoleDataScopeVO>(`/system/roles/${id}/data-scopes`).then((r) => r.data)
}

export function saveRoleDataScopes(id: string, data: RoleDataScopeSaveDTO): Promise<RoleDataScopeVO> {
  return put<RoleDataScopeVO>(`/system/roles/${id}/data-scopes`, data).then((r) => r.data)
}
