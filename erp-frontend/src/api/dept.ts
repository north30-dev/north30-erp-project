import { del, get, post, put } from '../utils/request'
import type {
  DeptCreateDTO,
  DeptMutationVO,
  DeptTreeQueryDTO,
  DeptTreeVO,
  DeptUpdateDTO,
} from '../types/api'

/** 组织部门接口（对应后端 SysDeptController /api/system/depts，接口文档 5.4） */

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
