import { del, get, post, put } from '../utils/request'
import { createStatusApi } from './statusApi'
import type {
  ConfigCreateDTO,
  ConfigQueryDTO,
  ConfigUpdateDTO,
  ConfigUpdateVO,
  ConfigVO,
  DeleteResultVO,
  MutationVO,
  PageResult,
} from '../types/api'

/**
 * 系统参数管理接口（对应后端 SysConfigController /api/system/configs，接口文档 5.6）。
 * 变更状态接口的 ConfigStatusDTO/ConfigStatusVO 与通用 StatusDTO/StatusVO 同构，走 statusApi 工厂（08 文档阶段四）。
 */

/** 变更系统参数状态（内置参数不可停用，后端 18033 拦截；乐观锁冲突 409/10601） */
export const changeConfigStatus = createStatusApi('configs').changeStatus

/** 系统参数分页查询（5.6.1） */
export function getConfigPage(query: ConfigQueryDTO): Promise<PageResult<ConfigVO>> {
  return get<PageResult<ConfigVO>>('/system/configs', query as Record<string, unknown>).then((r) => r.data)
}

/** 新增系统参数（5.6.2，configKey 全局唯一，重复 10602） */
export function createConfig(data: ConfigCreateDTO): Promise<MutationVO> {
  return post<MutationVO>('/system/configs', data).then((r) => r.data)
}

/** 修改系统参数（5.6.3，configKey/valueType/configGroup/isSystem 不可修改，返回生效提示） */
export function updateConfig(id: string, data: ConfigUpdateDTO): Promise<ConfigUpdateVO> {
  return put<ConfigUpdateVO>(`/system/configs/${id}`, data).then((r) => r.data)
}

/** 删除系统参数（5.6.4，内置参数不可删除，后端 18033 拦截） */
export function deleteConfig(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/configs/${id}`).then((r) => r.data)
}
