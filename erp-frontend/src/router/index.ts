import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getAccessToken } from '../utils/token'
import { useAuthStore } from '../stores/auth'

/** 全局路由表（动态业务路由后续由 auth store 的菜单树驱动，基建期先放占位页） */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/system/login/index.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/system/user',
    name: 'system-user',
    component: () => import('../views/system/user/index.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/system/role',
    name: 'system-role',
    component: () => import('../views/system/role/index.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/',
    name: 'home',
    // 占位：F9 动态菜单落地后改为工作台/首页，现阶段默认进用户管理页
    redirect: '/system/user',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/** 全局守卫：无 token 一律去登录页；有 token 但内存态未加载（刷新/新标签页）先补齐用户档案（含权限点，v-perm 依赖） */
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const hasToken = Boolean(getAccessToken())
  if (!hasToken && to.name !== 'login') {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (hasToken && !auth.userInfo && to.name !== 'login') {
    try {
      await auth.loadProfile()
    } catch {
      // 失败提示由 request 拦截器弹出（token 失效时其内部已跳登录），这里兜底不再放行
      return { name: 'login' }
    }
  }
  return true
})

export default router
