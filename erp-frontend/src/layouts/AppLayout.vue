<script setup lang="ts">
// 主布局壳：左侧深色导航（logo + 动态菜单）+ 顶栏（面包屑/用户）+ 内容区路由出口
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-sider class="app-layout__sider" theme="dark" :width="220">
      <div class="app-layout__logo">north30 ERP</div>
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
}

.app-layout__logo {
  flex: none;
  height: 48px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  background: #fff;
  display: flex;
  align-items: center;
}

.app-layout__content {
  overflow-y: auto;
  padding: 16px;
}
</style>
