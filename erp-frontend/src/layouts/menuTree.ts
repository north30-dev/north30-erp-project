import type { MenuTreeVO } from '../types/api'

/**
 * 在菜单树中查找 path 对应节点的祖先链（根 → 目标，深度优先）。
 * 精确命中与"最深前缀"回退统一处理：深层子节点先于浅层目录参与前缀匹配，
 * 因此隐藏子页（如 /system/dict/items）可回退到其父菜单（字典管理），
 * 精确路径则天然返回完整链。供侧边栏选中态/展开态与顶栏面包屑共用。
 */
export function findMenuChain(menus: MenuTreeVO[], path: string): MenuTreeVO[] {
  for (const menu of menus) {
    const childChain = findMenuChain(menu.children ?? [], path)
    if (childChain.length > 0) {
      return [menu, ...childChain]
    }
    if (menu.path && path.startsWith(menu.path)) {
      return [menu]
    }
  }
  return []
}
