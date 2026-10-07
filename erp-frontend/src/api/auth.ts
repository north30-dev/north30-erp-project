import { get, post } from '../utils/request'
import type {
  CaptchaVO,
  ChangePasswordDTO,
  ChangePasswordVO,
  CurrentUserVO,
  LoginDTO,
  LoginVO,
  LogoutVO,
  MenuTreeVO,
  RefreshTokenDTO,
  RefreshTokenVO,
  UserPermsVO,
} from '../types/api'

/** 认证授权接口（对应后端 AuthController，路径已含 /api 前缀由 request 统一拼接） */

export function createCaptcha(): Promise<CaptchaVO> {
  return post<CaptchaVO>('/auth/captcha').then((r) => r.data)
}

export function login(data: LoginDTO): Promise<LoginVO> {
  return post<LoginVO>('/auth/login', data).then((r) => r.data)
}

export function refreshToken(data: RefreshTokenDTO): Promise<RefreshTokenVO> {
  return post<RefreshTokenVO>('/auth/refresh', data).then((r) => r.data)
}

export function logout(): Promise<LogoutVO> {
  return post<LogoutVO>('/auth/logout').then((r) => r.data)
}

export function getCurrentUser(): Promise<CurrentUserVO> {
  return get<CurrentUserVO>('/auth/me').then((r) => r.data)
}

export function getCurrentUserMenus(): Promise<MenuTreeVO[]> {
  return get<MenuTreeVO[]>('/auth/menus').then((r) => r.data)
}

export function getCurrentUserPerms(): Promise<UserPermsVO> {
  return get<UserPermsVO>('/auth/perms').then((r) => r.data)
}

export function changePassword(data: ChangePasswordDTO): Promise<ChangePasswordVO> {
  return post<ChangePasswordVO>('/auth/password', data).then((r) => r.data)
}
