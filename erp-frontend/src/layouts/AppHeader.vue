<script setup lang="ts">
// 顶栏：左侧面包屑（按菜单树推导当前路径链）+ 右侧 刷新/全屏/用户下拉（个人中心 / 退出登录）
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import { CompressOutlined, DownOutlined, ExpandOutlined, LogoutOutlined, ReloadOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../stores/auth'
import { resetDynamicRoutes } from '../router/dynamicRoutes'
import { findMenuChain } from './menuTree'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/**
 * 面包屑：菜单树中当前路径的祖先链（隐藏子页回退到父菜单链）；
 * 菜单树无匹配时回退到 route.meta.title（如固定注册的 /profile）。
 */
const breadcrumbs = computed(() => {
  const chain = findMenuChain(auth.menus, route.path)
  if (chain.length > 0) {
    return chain.map((menu) => ({ key: menu.menuId, title: menu.menuName }))
  }
  const title = route.meta.title
  return typeof title === 'string' ? [{ key: route.path, title }] : []
})

/** 展示名：真实姓名优先，退回登录名 */
const displayName = computed(() => auth.userInfo?.realName || auth.userInfo?.username || '')

/** 当前是否全屏（fullscreenchange 事件同步，驱动按钮图标切换） */
const isFullscreen = ref(false)

function handleFullscreenChange(): void {
  isFullscreen.value = Boolean(document.fullscreenElement)
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

/** 刷新当前页（整页重载，最简可靠） */
function handleReload(): void {
  window.location.reload()
}

/** 全屏切换 */
function toggleFullscreen(): void {
  if (document.fullscreenElement) {
    void document.exitFullscreen()
  } else {
    void document.documentElement.requestFullscreen()
  }
}

const handleUserMenuClick: MenuProps['onClick'] = ({ key }) => {
  if (key === 'profile') {
    void router.push('/profile')
    return
  }
  if (key === 'logout') {
    handleLogout()
  }
}

function handleLogout(): void {
  Modal.confirm({
    title: '退出登录',
    content: '确定要退出当前账号吗？',
    okText: '退出',
    cancelText: '取消',
    onOk: async () => {
      await auth.logout()
      // 清理动态路由，防止上一账号的页面路由残留（404 兜底保留）
      resetDynamicRoutes(router)
      router.replace({ name: 'login' })
    },
  })
}
</script>

<template>
  <div class="app-header">
    <a-breadcrumb class="app-header__crumbs">
      <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.key">{{ item.title }}</a-breadcrumb-item>
    </a-breadcrumb>
    <div class="app-header__actions">
      <a-tooltip title="刷新当前页">
        <a-button type="text" size="small" class="app-header__action-btn" @click="handleReload">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip :title="isFullscreen ? '退出全屏' : '全屏'">
        <a-button type="text" size="small" class="app-header__action-btn" @click="toggleFullscreen">
          <template #icon>
            <CompressOutlined v-if="isFullscreen" />
            <ExpandOutlined v-else />
          </template>
        </a-button>
      </a-tooltip>
      <a-dropdown placement="bottomRight">
        <div class="app-header__user" role="button" tabindex="0">
          <a-avatar :size="28" class="app-header__avatar">{{ displayName.charAt(0) }}</a-avatar>
          <span class="app-header__name">{{ displayName }}</span>
          <DownOutlined class="app-header__caret" />
        </div>
      <template #overlay>
        <a-menu @click="handleUserMenuClick">
          <a-menu-item key="profile">
            <template #icon><UserOutlined /></template>
            个人中心
          </a-menu-item>
          <a-menu-item key="logout">
            <template #icon><LogoutOutlined /></template>
            退出登录
          </a-menu-item>
        </a-menu>
      </template>
      </a-dropdown>
    </div>
  </div>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
}

.app-header__actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.app-header__action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: rgba(0, 0, 0, 0.65);
}

.app-header__user {
  display: flex;
  gap: 8px;
  align-items: center;
  height: 100%;
  cursor: pointer;
}

.app-header__avatar {
  color: #fff;
  background-color: var(--erp-brand);
}

.app-header__name {
  font-size: 15px;
}

.app-header__caret {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
