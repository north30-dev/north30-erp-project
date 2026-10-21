import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '../api/auth'
import { clearTokens, getAccessToken, getRefreshToken, saveTokens } from '../utils/token'
import type { CurrentUserVO, LoginDTO, MenuTreeVO } from '../types/api'

/** 认证状态：token 存取唯一入口是 utils/token.ts，store 只持有可响应的用户态 */
export const useAuthStore = defineStore('auth', () => {
  /* ---------- 状态 ---------- */

  const userInfo = ref<CurrentUserVO | null>(null)
  /** 动态菜单树（目录/菜单两级，按钮不下发到路由） */
  const menus = ref<MenuTreeVO[]>([])
  /** 权限点集合（v-perm 指令消费） */
  const perms = ref<string[]>([])
  /** 是否管理员 */
  const isAdmin = ref(false)

  /* ---------- 派生 ---------- */

  const isLoggedIn = computed(() => Boolean(getAccessToken() && getRefreshToken()))
  /** 权限点是否包含指定标识（管理员放行全部） */
  function hasPerm(code: string): boolean {
    return isAdmin.value || perms.value.includes(code)
  }

  /* ---------- 动作 ---------- */

  async function login(payload: LoginDTO): Promise<void> {
    const loginVO = await authApi.login(payload)
    saveTokens(loginVO.accessToken, loginVO.refreshToken)
    await loadProfile()
  }

  /** 登录后/刷新页面时加载用户信息、菜单、权限点（三个接口并行） */
  async function loadProfile(): Promise<void> {
    const [user, menuTree, userPerms] = await Promise.all([
      authApi.getCurrentUser(),
      authApi.getCurrentUserMenus(),
      authApi.getCurrentUserPerms(),
    ])
    userInfo.value = user
    menus.value = menuTree
    perms.value = userPerms.perms
    isAdmin.value = userPerms.roles.includes('admin')
  }

  /** 登出：调后端使会话失效并清本地 token（接口失败也继续清理，保证前端可登出） */
  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      clearTokens()
      userInfo.value = null
      menus.value = []
      perms.value = []
      isAdmin.value = false
    }
  }

  return { userInfo, menus, perms, isAdmin, isLoggedIn, hasPerm, login, loadProfile, logout }
})
