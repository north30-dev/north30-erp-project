import { post } from '../utils/request'
import type { StatusDTO, StatusVO } from '../types/api'

/**
 * 状态开关接口工厂（08 文档阶段四状态开关统一）。
 * <p>六域（user/role/menu/dept/dict_type/dict_item）各自新增 POST /{resource}/{id}/status
 * 动作接口；后端不做公共接口以保住声明式鉴权、分域解耦与审计语义，前端用本工厂封装后调用侧零差异。</p>
 *
 * @param resource 资源路径段（如 'users'、'roles'、'menus'、'depts'、'dict-types'、'dict-items'）
 */
export function createStatusApi(resource: string) {
  return {
    /** 变更状态（乐观锁 version 必填，冲突返回 409/10601） */
    changeStatus(id: string, data: StatusDTO): Promise<StatusVO> {
      return post<StatusVO>(`/system/${resource}/${id}/status`, data).then((r) => r.data)
    },
  }
}
