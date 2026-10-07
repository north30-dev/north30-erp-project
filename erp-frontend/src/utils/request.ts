import { message } from 'ant-design-vue'
import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { clearTokens, getAccessToken, getRefreshToken, saveAccessToken } from './token'
import type { Result } from '../types/api'

/** 后端认证接口路径（与 AuthController 对齐） */
const REFRESH_URL = '/auth/refresh'
const LOGIN_URL = '/auth/login'

/** 业务失败/请求异常时向外抛出的统一错误（业务代码 catch 到的都是它） */
export class RequestError extends Error {
  constructor(messageText: string) {
    super(messageText)
    this.name = 'RequestError'
  }
}

/** axios 实例（编码规范：全项目唯一，业务代码禁止直接 import axios） */
const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

/* ---------- 无感刷新：并发 401 合并等待同一次刷新 ---------- */

let refreshing: Promise<boolean> | null = null

/** 用 refresh token 换新 access token；成功返回 true，失败清 token 返回 false */
async function tryRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return false
  }
  try {
    // 用独立的裸 axios，避免走本实例拦截器造成递归
    const response = await axios.post<Result<{ accessToken: string }>>(REFRESH_URL, { refreshToken })
    const result = response.data
    if (result.code !== 200) {
      clearTokens()
      return false
    }
    saveAccessToken(result.data.accessToken)
    return true
  } catch {
    clearTokens()
    return false
  }
}

/** 刷新成功后重放原请求 */
function replay(requestConfig: InternalAxiosRequestConfig): Promise<AxiosResponse> {
  return instance(requestConfig)
}

/** 跳转登录页（保留当前路由供回跳） */
function redirectToLogin(): void {
  clearTokens()
  const current = window.location.hash.replace('#', '')
  if (current !== '/login') {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}

/* ---------- 拦截器 ---------- */

// 请求拦截器：注入 Bearer Token
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：业务状态码 / 401 无感刷新 / 统一错误提示
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    // HTTP 正常时仍需检查业务状态码
    const result = response.data as Result<unknown>
    if (result.code !== 200) {
      const description = result.message || '请求失败'
      message.error(description)
      return Promise.reject(new RequestError(description))
    }
    return response
  },
  async (error: AxiosError) => {
    const requestConfig = error.config as InternalAxiosRequestConfig | undefined

    // 401：优先无感刷新后重放；刷新失败跳登录（登录接口本身的 401 不进入刷新流程）
    if (error.response?.status === 401 && requestConfig && requestConfig.url !== LOGIN_URL) {
      refreshing = refreshing ?? tryRefresh()
      const refreshed = await refreshing
      refreshing = null
      if (refreshed) {
        return replay(requestConfig)
      }
      message.error('登录已失效，请重新登录')
      redirectToLogin()
      return Promise.reject(new RequestError('登录已失效'))
    }

    // 其他错误：优先透出后端 message
    const backendResult = error.response?.data as Result<unknown> | undefined
    const description = backendResult?.message || error.message || '网络异常，请稍后重试'
    message.error(description)
    return Promise.reject(new RequestError(description))
  },
)

/* ---------- 请求方法：api 层专用，直接返回 Result<T> ---------- */

export function get<T>(url: string, params?: Record<string, unknown>): Promise<Result<T>> {
  return instance.get<unknown, AxiosResponse<Result<T>>>(url, { params }).then((r) => r.data)
}

export function post<T>(url: string, data?: unknown): Promise<Result<T>> {
  return instance.post<unknown, AxiosResponse<Result<T>>>(url, data).then((r) => r.data)
}

export function put<T>(url: string, data?: unknown): Promise<Result<T>> {
  return instance.put<unknown, AxiosResponse<Result<T>>>(url, data).then((r) => r.data)
}

export function del<T>(url: string, params?: Record<string, unknown>): Promise<Result<T>> {
  return instance.delete<unknown, AxiosResponse<Result<T>>>(url, { params }).then((r) => r.data)
}
