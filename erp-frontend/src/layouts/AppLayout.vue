<script setup lang="ts">
// 主布局壳：左侧品牌深色导航（可点 logo + 动态菜单）+ 顶栏（面包屑/刷新/全屏/用户）+ 内容区路由出口
import { useRouter } from 'vue-router'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'

const router = useRouter()

/** logo 点击回工作台 */
function handleLogoClick(): void {
  void router.push('/dashboard')
}
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-sider class="app-layout__sider" theme="dark" :width="220">
      <div class="app-layout__logo" role="button" tabindex="0" @click="handleLogoClick">
        <AppstoreOutlined class="app-layout__logo-icon" />
        <span>North30 ERP</span>
      </div>
      <AppSidebar class="app-layout__menu" />
    </a-layout-sider>
    <a-layout class="app-layout__main">
      <a-layout-header class="app-layout__header">
        <AppHeader />
      </a-layout-header>
      <a-layout-content class="app-layout__content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.app-layout {
  height: 100vh;
  overflow: hidden;
}

.app-layout__sider {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--erp-brand-deep);
}

.app-layout__logo {
  flex: none;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  height: 48px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: pointer;
}

.app-layout__logo-icon {
  font-size: 16px;
}

.app-layout__menu {
  flex: 1;
  overflow-y: auto;
}

.app-layout__main {
  overflow: hidden;
}

.app-layout__header {
  flex: none;
  height: 48px;
  padding: 0 16px;
  background: var(--erp-bg-container);
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.app-layout__content {
  overflow-y: auto;
  padding: 16px;
  background: var(--erp-bg-layout);
}
</style>
