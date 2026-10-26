import { del, get, post, put } from '../utils/request'
import type {
  DeleteResultVO,
  MenuCreatedVO,
  MenuCreateDTO,
  MenuNodeVO,
  MenuTreeQueryDTO,
  MenuUpdatedVO,
  MenuUpdateDTO,
} from '../types/api'

/** 菜单与权限点接口（对应后端 SysMenuController /api/system/menus，接口文档 5.3） */

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
