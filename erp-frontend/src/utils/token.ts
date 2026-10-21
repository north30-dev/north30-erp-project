/** 双 token 的 localStorage 键名（编码规范第 6 节：读写统一收口本文件） */
export const ACCESS_TOKEN_KEY = 'erp_access_token'
export const REFRESH_TOKEN_KEY = 'erp_refresh_token'

/** 读取访问令牌 */
export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

/** 读取刷新令牌 */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/** 同时保存双 token */
export function saveTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

/** 只更新访问令牌（无感刷新成功后调用） */
export function saveAccessToken(accessToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

/** 清空双 token（登出/刷新失败） */
export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
