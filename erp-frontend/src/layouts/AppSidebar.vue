<script setup lang="ts">
// 侧边栏导航：读 auth.menus 递归渲染；选中态/展开态与当前路由同步；点击按菜单 path 跳转
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MENU_TYPE_DIR, MENU_TYPE_MENU } from '../constants'
import { useAuthStore } from '../stores/auth'
import { findMenuChain } from './menuTree'
import AppSidebarItem from './AppSidebarItem.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 当前路由命中的菜单链（根目录 → 菜单；隐藏子页回退到父菜单） */
const activeChain = computed(() => findMenuChain(auth.menus, route.path))

/** 选中项：链上最后一个"菜单"节点的 path（如 /system/dict/items 高亮字典管理） */
const selectedKeys = computed<string[]>(() => {
  const menuPath = [...activeChain.value]
    .reverse()
    .find((menu) => menu.menuType === MENU_TYPE_MENU && menu.path)
  return menuPath?.path ? [menuPath.path] : []
})

/** 当前展开的目录 key 集合（受控） */
const expandedKeys = ref<string[]>([])

/** 路由变化时以命中链上的目录重建展开态（保证刷新/跳转后当前菜单位于展开分支内） */
watch(
  () => route.path,
  () => {
    expandedKeys.value = activeChain.value.flatMap((menu) => (menu.menuType === MENU_TYPE_DIR && menu.path ? [menu.path] : []))
  },
  { immediate: true },
)

/** 用户手动展开/收起目录时同步受控状态 */
function handleOpenChange(keys: (string | number)[]): void {
  expandedKeys.value = keys.map(String)
}

/** 菜单点击跳转（key 即菜单 path；目录点击为展开不触发） */
function handleMenuClick({ key }: { key: string | number }): void {
  const target = String(key)
  if (target !== route.path) {
    router.push(target)
  }
}
</script>

<template>
  <a-menu
    mode="inline"
    theme="dark"
    :selected-keys="selectedKeys"
    :open-keys="expandedKeys"
    @open-change="handleOpenChange"
    @click="handleMenuClick"
  >
    <AppSidebarItem v-for="menu in auth.menus" :key="menu.menuId" :menu="menu" />
  </a-menu>
</template>
