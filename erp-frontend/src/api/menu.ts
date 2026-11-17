import { del, get, post, put } from '../utils/request'
import { createStatusApi } from './statusApi'
import type {
  DeleteResultVO,
  MenuCreatedVO,
  MenuCreateDTO,
  MenuNodeVO,
  MenuTreeQueryDTO,
  MenuUpdatedVO,
  MenuUpdateDTO,
  MoveDTO,
} from '../types/api'

/** 菜单与权限点接口（对应后端 SysMenuController /api/system/menus，接口文档 5.3） */

/** 变更菜单状态（08 文档阶段四状态开关统一，内置菜单后端 18039 拒绝） */
export const changeMenuStatus = createStatusApi('menus').changeStatus

/** 菜单上移/下移（08 文档阶段四排序统一，同级规范化重排后相邻交换） */
export function moveMenu(id: string, data: MoveDTO): Promise<void> {
  return post<void>(`/system/menus/${id}/move`, data).then((r) => r.data)
}

export function getMenuTree(query?: MenuTreeQueryDTO): Promise<MenuNodeVO[]> {
  return get<MenuNodeVO[]>('/system/menus/tree', query as Record<string, unknown> | undefined).then(
    (r) => r.data,
  )
}

export function createMenu(data: MenuCreateDTO): Promise<MenuCreatedVO> {
  return post<MenuCreatedVO>('/system/menus', data).then((r) => r.data)
}

export function updateMenu(id: string, data: MenuUpdateDTO): Promise<MenuUpdatedVO> {
  return put<MenuUpdatedVO>(`/system/menus/${id}`, data).then((r) => r.data)
}

export function deleteMenu(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/menus/${id}`).then((r) => r.data)
}
