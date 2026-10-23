import { get } from '../utils/request'
import type { MenuNodeVO } from '../types/api'

/** 菜单接口（对应后端 SysMenuController /api/system/menus，接口文档 5.3；F2 仅消费菜单树作分配数据源，F3 扩展 CRUD） */

export function getMenuTree(): Promise<MenuNodeVO[]> {
  return get<MenuNodeVO[]>('/system/menus/tree').then((r) => r.data)
}
