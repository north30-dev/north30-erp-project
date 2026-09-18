import { message } from 'ant-design-vue'
import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { Result } from '../types/api'

/** localStorage 中存储 token 的键名 */
const TOKEN_KEY = 'erp_token'

/** 业务失败/请求异常时向外抛出的统一错误 */
class RequestError extends Error {
  constructor(messageText: string) {
    super(messageText)
    this.name = 'RequestError'
  }
}

/** 从本地存储读取 token */
function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** axios 实例 */
const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器：注入 Bearer Token
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理业务状态码与网络错误
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    // 后端统一响应体：HTTP 正常时仍需检查业务状态码
    const result = response.data as Result<unknown>
    if (result.code !== 200) {
      message.error(result.message || '请求失败')
      return Promise.reject(new RequestError(result.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError) => {
    // 网络错误 / HTTP 错误：优先透出后端返回的 message
    const backendResult = error.response?.data as Result<unknown> | undefined
    const description = backendResult?.message || error.message || '网络异常，请稍后重试'
    message.error(description)
    return Promise.reject(error instanceof RequestError ? error : new RequestError(description))
  },
)

/** GET 请求：返回后端统一响应体 */
export function get<T>(url: string, params?: Record<string, unknown>): Promise<Result<T>> {
  return instance
    .get<unknown, AxiosResponse<Result<T>>>(url, { params })
    .then((response) => response.data)
}

/** POST 请求：返回后端统一响应体 */
export function post<T>(url: string, data?: unknown): Promise<Result<T>> {
  return instance
    .post<unknown, AxiosResponse<Result<T>>>(url, data)
    .then((response) => response.data)
}
