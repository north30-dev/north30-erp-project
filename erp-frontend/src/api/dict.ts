import { del, get, post, put } from '../utils/request'
import type {
  DeleteResultVO,
  DictItemCreateDTO,
  DictItemPageQueryDTO,
  DictItemUpdateDTO,
  DictItemVO,
  DictTypeCreateDTO,
  DictTypePageQueryDTO,
  DictTypeUpdateDTO,
  DictTypeVO,
  MutationVO,
  PageResult,
} from '../types/api'

/** 字典管理接口（对应后端 SysDictTypeController /api/system/dict-types 与 SysDictItemController /api/system/dict-items，接口文档 5.5） */

export function getDictTypePage(query: DictTypePageQueryDTO): Promise<PageResult<DictTypeVO>> {
  return get<PageResult<DictTypeVO>>('/system/dict-types', query as Record<string, unknown>).then((r) => r.data)
}

export function createDictType(data: DictTypeCreateDTO): Promise<MutationVO> {
  return post<MutationVO>('/system/dict-types', data).then((r) => r.data)
}

export function updateDictType(id: string, data: DictTypeUpdateDTO): Promise<MutationVO> {
  return put<MutationVO>(`/system/dict-types/${id}`, data).then((r) => r.data)
}

export function deleteDictType(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/dict-types/${id}`).then((r) => r.data)
}

export function getDictItemPage(query: DictItemPageQueryDTO): Promise<PageResult<DictItemVO>> {
  return get<PageResult<DictItemVO>>('/system/dict-items', query as Record<string, unknown>).then((r) => r.data)
}

export function createDictItem(data: DictItemCreateDTO): Promise<MutationVO> {
  return post<MutationVO>('/system/dict-items', data).then((r) => r.data)
}

export function updateDictItem(id: string, data: DictItemUpdateDTO): Promise<MutationVO> {
  return put<MutationVO>(`/system/dict-items/${id}`, data).then((r) => r.data)
}

export function deleteDictItem(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/dict-items/${id}`).then((r) => r.data)
}
