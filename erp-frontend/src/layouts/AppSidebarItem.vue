<script setup lang="ts">
// 侧边栏菜单节点（递归组件）：目录渲染为 a-sub-menu，菜单渲染为 a-menu-item，按钮与隐藏节点不渲染
import { computed } from 'vue'
import type { Component } from 'vue'
import * as AntdIcons from '@ant-design/icons-vue'
import { MENU_TYPE_DIR, MENU_TYPE_MENU, VISIBLE_SHOWN } from '../constants'
import type { MenuTreeVO } from '../types/api'

const props = defineProps<{ menu: MenuTreeVO }>()

/** 全量图标表：icon 名由后端 sys_menu 数据下发（大驼峰短名，如 User/Team），无法静态枚举，故整体引入按名解析 */
const iconTable = AntdIcons as Record<string, Component | undefined>

/** 按名解析图标组件；未知图标降级为 undefined 不渲染（兼容带/不带 Outlined 后缀两种写法） */
const iconComp = computed<Component | undefined>(() => {
  const name = props.menu.icon
  if (!name) {
    return undefined
  }
  return iconTable[`${name}Outlined`] ?? iconTable[name]
})

/** 节点 key（a-menu 约定）：优先用路由 path，目录缺 path 时退回 menuId */
const menuKey = computed(() => props.menu.path ?? String(props.menu.menuId))

/** 可见子节点：仅目录与菜单且 visible=显示 */
const visibleChildren = computed(() =>
  props.menu.children?.filter(
    (child) => (child.menuType === MENU_TYPE_DIR || child.menuType === MENU_TYPE_MENU) && child.visible === VISIBLE_SHOWN,
  ) ?? [],
)
</script>

<template>
  <a-sub-menu v-if="menu.menuType === MENU_TYPE_DIR && menu.visible === VISIBLE_SHOWN" :key="menuKey">
    <template #icon>
      <component :is="iconComp" v-if="iconComp" />
    </template>
    <template #title>{{ menu.menuName }}</template>
    <AppSidebarItem v-for="child in visibleChildren" :key="child.menuId" :menu="child" />
  </a-sub-menu>
  <a-menu-item v-else-if="menu.menuType === MENU_TYPE_MENU && menu.visible === VISIBLE_SHOWN" :key="menuKey">
    <component :is="iconComp" v-if="iconComp" />
    <span>{{ menu.menuName }}</span>
  </a-menu-item>
</template>
