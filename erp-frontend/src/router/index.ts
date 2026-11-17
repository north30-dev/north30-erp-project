import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getAccessToken } from '../utils/token'
import { useAuthStore } from '../stores/auth'
import { isDynamicRoutesRegistered, registerDynamicRoutes } from './dynamicRoutes'

/**
 * 静态路由：仅登录页。业务页面全部由菜单树驱动动态注册（dynamicRoutes.ts），
 * "/" 首页重定向与 404 catch-all 兜底也在动态注册时追加。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/system/login/index.vue'),
    meta: { requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/**
 * 全局守卫时序：
 * 1. 无 token → 登录页并携带 redirect（登录成功后回跳）；
 * 2. 有 token 但内存态未加载（刷新/新标签页）→ 先 loadProfile 补齐用户档案（含权限点，v-perm 依赖）；
 * 3. 动态路由未注册（首次进入/登出后重登）→ 按菜单树注册，随后 replace 重入当前导航以命中新路由；
 * 4. 就绪后直接放行。
 */
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const hasToken = Boolean(getAccessToken())
  if (!hasToken) {
    return to.name === 'login' ? true : { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login') {
    return true
  }
  if (!auth.userInfo) {
    try {
      await auth.loadProfile()
    } catch {
      // 失败提示由 request 拦截器弹出（token 失效时其内部已跳登录），这里兜底不再放行
      return { name: 'login' }
    }
  }
  if (!isDynamicRoutesRegistered()) {
    registerDynamicRoutes(router, auth.menus)
    // 此刻路由表才包含目标页面，replace 重入以重新解析本次导航。
    // 只携带 path/query/hash：注册前导航可能已匹配到 404 catch-all（登出后兜底路由保留），
    // 若原样带回旧 name（not-found）会永远命中 404，无法命中刚注册的首页重定向。
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  }
  return true
})

export default router
