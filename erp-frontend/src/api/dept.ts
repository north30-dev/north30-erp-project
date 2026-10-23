import { get } from '../utils/request'
import type { DeptTreeVO } from '../types/api'

/** 组织部门接口（对应后端 SysDeptController /api/system/depts，F1 仅消费树查询作下拉数据源） */

export function getDeptTree(): Promise<DeptTreeVO[]> {
  return get<DeptTreeVO[]>('/system/depts/tree').then((r) => r.data)
}
