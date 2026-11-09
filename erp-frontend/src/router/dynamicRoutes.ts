import type { Component } from 'vue'
import type { Router, RouteRecordRaw } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import { MENU_TYPE_MENU, VISIBLE_SHOWN } from '../constants'
import type { MenuTreeVO } from '../types/api'

/** 动态页面的布局父路由名（children 均为绝对路径，仅作挂载点，不产生独立页面） */
const LAYOUT_ROUTE_NAME = 'app-layout'

/** 布局父路由 path：对齐菜单种子唯一顶级目录；后续新增顶级目录时子路由仍为绝对路径，无需改动 */
const LAYOUT_ROUTE_PATH = '/system'

/** 首页重定向路由名（菜单注册后指向首个可见菜单；menus 为空则不注册，'/' 落入 404） */
const HOME_ROUTE_NAME = 'home'

/** 404 兜底路由名（登出重置时保留） */
const NOT_FOUND_ROUTE_NAME = 'not-found'

/** 视图组件模块表（key 相对本文件：../views/<菜单 component>.vue，Vite 懒加载分包） */
const viewModules = import.meta.glob('../views/**/*.vue')

/** 是否已注册（防重复；登出 resetDynamicRoutes 后复位，换账号重登可再注册） */
let registered = false

/** 动态路由是否已注册（路由守卫据此决定是否补注册并 replace 重入） */
export function isDynamicRoutesRegistered(): boolean {
  return registered
}

/**
 * 按菜单 component 字段（相对 src/views、不带 .vue）解析路由懒加载组件。
 * 解析失败告警并返回 undefined，调用方跳过注册、不阻断其余菜单。
 */
function resolveViewComponent(component: string): RouteRecordRaw['component'] {
  const normalized = component.replace(/^\/+/, '').replace(/\.vue$/, '')
  const loader = viewModules[`../views/${normalized}.vue`]
  if (!loader) {
    // 豁免 console.warn：路由注册期无界面上下文，message 弹窗会打断首屏且无法交互，
    // 控制台告警是菜单种子与前端视图漂移（component 拼写/目录不一致）的唯一诊断出口
    console.warn(`[router] 菜单组件未找到，已跳过注册: ${component}`)
    return undefined
  }
  return loader as () => Promise<Component>
}

/** 路由名由 path 派生（/system/user → menu-system-user），稳定且不与静态路由冲突 */
function routeName(path: string): string {
  return `menu-${path.replace(/^\/+/, '').replace(/\//g, '-')}`
}

/** 收集菜单树中可注册页面（menuType=菜单 且 visible=显示 且 path/component 齐备）为布局子路由 */
function collectPageRoutes(menus: MenuTreeVO[], out: RouteRecordRaw[]): void {
  for (const menu of menus) {
    if (menu.menuType === MENU_TYPE_MENU && menu.visible === VISIBLE_SHOWN && menu.path && menu.component) {
      const component = resolveViewComponent(menu.component)
      if (component) {
        out.push({ path: menu.path, name: routeName(menu.path), component, meta: { requiresAuth: true } })
      }
    }
    collectPageRoutes(menu.children ?? [], out)
  }
}

/** 先序遍历取首个可见菜单 path（"/" 重定向目标；无可见菜单返回 null） */
function firstVisibleMenuPath(menus: MenuTreeVO[]): string | null {
  for (const menu of menus) {
    if (menu.menuType === MENU_TYPE_MENU && menu.visible === VISIBLE_SHOWN && menu.path) {
      return menu.path
    }
    const childPath = firstVisibleMenuPath(menu.children ?? [])
    if (childPath) {
      return childPath
    }
  }
  return null
}

/**
 * 按菜单树注册动态业务路由（登录后/刷新恢复时由路由守卫调用，防重复注册）。
 * <p>流程：收集页面路由 → 追加字典项隐藏子页 → 注册布局父路由 → 注册 "/" 重定向到首个可见菜单
 * → 注册 404 catch-all 兜底。menus 为空时无 "/" 重定向，任意路径均命中 404。</p>
 */
export function registerDynamicRoutes(router: Router, menus: MenuTreeVO[]): void {
  if (registered) {
    return
  }
  const children: RouteRecordRaw[] = []
  collectPageRoutes(menus, children)

  // 隐藏子页特判：字典项管理不在菜单树（由字典类型页跳入），此处补注册保证直敲 URL / 刷新可达
  const dictItems = resolveViewComponent('system/dict/items/index')
  if (dictItems) {
    children.push({
      path: '/system/dict/items',
      name: 'menu-system-dict-items',
      component: dictItems,
      meta: { requiresAuth: true },
    })
  }

  if (children.length > 0) {
    router.addRoute({ path: LAYOUT_ROUTE_PATH, name: LAYOUT_ROUTE_NAME, component: AppLayout, children })
  }
  const firstPath = firstVisibleMenuPath(menus)
  if (firstPath) {
    router.addRoute({ path: '/', name: HOME_ROUTE_NAME, redirect: firstPath })
  }
  // catch-all 必须最后注册（兜底排名最低，不影响已有静态/动态路由匹配）
  router.addRoute({
    path: '/:pathMatch(.*)*',
    name: NOT_FOUND_ROUTE_NAME,
    component: () => import('../views/NotFound.vue'),
  })
  registered = true
}

/**
 * 登出时清理动态路由（布局父路由与其下全部页面、"首页重定向"一并移除，
 * 404 兜底保留），防止上一账号的页面路由残留。未注册时幂等返回。
 */
export function resetDynamicRoutes(router: Router): void {
  if (!registered) {
    return
  }
  if (router.hasRoute(LAYOUT_ROUTE_NAME)) {
    // 移除父路由即连带移除全部子页面路由
    router.removeRoute(LAYOUT_ROUTE_NAME)
  }
  if (router.hasRoute(HOME_ROUTE_NAME)) {
    router.removeRoute(HOME_ROUTE_NAME)
  }
  registered = false
}
