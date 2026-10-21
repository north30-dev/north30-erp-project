import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getAccessToken } from '../utils/token'

/** 全局路由表（动态业务路由后续由 auth store 的菜单树驱动，基建期先放占位页） */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/system/login/index.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'home',
    // 占位：管理页路由在页面阶段由菜单树动态生成后替换
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/** 全局守卫：无 token 一律去登录页，登录后加载用户档案（用户信息/菜单/权限点） */
router.beforeEach(async (to) => {
  const hasToken = Boolean(getAccessToken())
  if (!hasToken && to.name !== 'login') {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
