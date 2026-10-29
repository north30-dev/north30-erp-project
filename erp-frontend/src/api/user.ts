import { del, get, getBlob, post, put } from '../utils/request'
import { createStatusApi } from './statusApi'
import type {
  DeleteResultVO,
  PageResult,
  UserAssignRolesDTO,
  UserAssignRolesVO,
  UserCreateDTO,
  UserDetailVO,
  UserQueryDTO,
  UserResetPasswordDTO,
  UserResetPasswordVO,
  UserStatusDTO,
  UserStatusVO,
  UserUpdateDTO,
  UserVO,
} from '../types/api'

/** 用户管理接口（对应后端 SysUserController /api/system/users，接口文档 5.1） */

export function getUserPage(query: UserQueryDTO): Promise<PageResult<UserVO>> {
  return get<PageResult<UserVO>>('/system/users', query as Record<string, unknown>).then((r) => r.data)
}

export function getUserDetail(id: string): Promise<UserDetailVO> {
  return get<UserDetailVO>(`/system/users/${id}`).then((r) => r.data)
}

export function createUser(data: UserCreateDTO): Promise<number> {
  return post<number>('/system/users', data).then((r) => r.data)
}

export function updateUser(id: string, data: UserUpdateDTO): Promise<string> {
  return put<string>(`/system/users/${id}`, data).then((r) => r.data)
}

export function deleteUser(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/users/${id}`).then((r) => r.data)
}

export function changeUserStatus(id: string, data: UserStatusDTO): Promise<UserStatusVO> {
  return createStatusApi('users').changeStatus(id, data).then((r) => r as UserStatusVO)
}

export function resetUserPassword(id: string, data: UserResetPasswordDTO): Promise<UserResetPasswordVO> {
  return post<UserResetPasswordVO>(`/system/users/${id}/reset-password`, data).then((r) => r.data)
}

export function assignUserRoles(id: string, data: UserAssignRolesDTO): Promise<UserAssignRolesVO> {
  return put<UserAssignRolesVO>(`/system/users/${id}/roles`, data).then((r) => r.data)
}

/** 导出用户列表 CSV（文件流，文件名在响应头，调用方从 fetch 响应头取不到时用时间戳兜底） */
export function exportUserCsv(query: UserQueryDTO): Promise<Blob> {
  return getBlob('/system/users/export', query as Record<string, unknown>)
}
