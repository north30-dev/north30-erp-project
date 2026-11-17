import { del, get, post, put } from '../utils/request'
import { createStatusApi } from './statusApi'
import type {
  DeptCreateDTO,
  DeptMutationVO,
  DeptTreeQueryDTO,
  DeptTreeVO,
  DeptUpdateDTO,
  MoveDTO,
} from '../types/api'

/** 组织部门接口（对应后端 SysDeptController /api/system/depts，接口文档 5.4） */

/** 变更组织部门状态（08 文档阶段四状态开关统一） */
export const changeDeptStatus = createStatusApi('depts').changeStatus

/** 组织上移/下移（08 文档阶段四排序统一，同级规范化重排后相邻交换） */
export function moveDept(id: string, data: MoveDTO): Promise<void> {
  return post<void>(`/system/depts/${id}/move`, data).then((r) => r.data)
}

export function getDeptTree(query?: DeptTreeQueryDTO): Promise<DeptTreeVO[]> {
  return get<DeptTreeVO[]>('/system/depts/tree', query as Record<string, unknown> | undefined).then(
    (r) => r.data,
  )
}

export function createDept(data: DeptCreateDTO): Promise<DeptMutationVO> {
  return post<DeptMutationVO>('/system/depts', data).then((r) => r.data)
}

export function updateDept(id: string, data: DeptUpdateDTO): Promise<DeptMutationVO> {
  return put<DeptMutationVO>(`/system/depts/${id}`, data).then((r) => r.data)
}

export function deleteDept(id: string): Promise<DeptMutationVO> {
  return del<DeptMutationVO>(`/system/depts/${id}`).then((r) => r.data)
}
