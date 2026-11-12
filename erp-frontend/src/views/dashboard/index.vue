<script setup lang="ts">
// 工作台首页（一期：纯前端，数据全部来自 auth store 缓存，零新接口）。
// 结构：欢迎卡 + 左主卡"快捷入口" + 右栏"系统信息"/"建设进度"。
// 二期预留：KPI 概览与待办列表位于快捷入口卡上方，待业务域提供 countByXxx 汇总接口后接入。
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { Component } from 'vue'
import * as AntdIcons from '@ant-design/icons-vue'
import { MENU_TYPE_MENU, VISIBLE_SHOWN } from '../../constants'
import { useAuthStore } from '../../stores/auth'
import type { MenuTreeVO } from '../../types/api'

const router = useRouter()
const auth = useAuthStore()

const user = computed(() => auth.userInfo)

/** 全量图标表：icon 名由后端 sys_menu 下发（同 AppSidebarItem 的按名解析方式） */
const iconTable = AntdIcons as Record<string, Component | undefined>

/** 快捷入口：先序遍历菜单树取"菜单 && 显示 && 有 path"的节点 */
const shortcuts = computed(() => {
  const out: { path: string; name: string; icon: Component | undefined }[] = []
  function walk(menus: MenuTreeVO[]): void {
    for (const menu of menus) {
      if (menu.menuType === MENU_TYPE_MENU && menu.visible === VISIBLE_SHOWN && menu.path) {
        const iconName = menu.icon
        out.push({
          path: menu.path,
          name: menu.menuName,
          icon: iconName ? iconTable[`${iconName}Outlined`] ?? iconTable[iconName] : undefined,
        })
      }
      walk(menu.children ?? [])
    }
  }
  walk(auth.menus)
  return out
})

/** 欢迎语时段问候（6-12 早 / 12-18 下午 / 其余晚上） */
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 6 && hour < 12) {
    return '早上好'
  }
  if (hour >= 12 && hour < 18) {
    return '下午好'
  }
  return '晚上好'
})

/** 今日日期（YYYY年M月D日） */
const todayText = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`
})

/** 展示名：真实姓名优先，退回登录名 */
const displayName = computed(() => user.value?.realName || user.value?.username || '-')

/** 空值统一占位 */
function text(value?: string | null): string {
  return value || '-'
}

/** 建设进度（随开发阶段手工更新；当前阶段加"当前"徽标） */
const milestones = ref([
  { name: '系统管理', status: 'done' as const, current: false },
  { name: '基础数据', status: 'pending' as const, current: true },
  { name: '业务模块', status: 'pending' as const, current: false },
  { name: '报表与集成', status: 'pending' as const, current: false },
])

function goProfile(): void {
  void router.push('/profile')
}

function goShortcut(path: string): void {
  void router.push(path)
}
</script>

<template>
  <div class="dashboard-page">
    <a-card class="dashboard-page__welcome">
      <div class="dashboard-page__welcome-row">
        <div>
          <div class="dashboard-page__greeting">{{ greeting }}，{{ displayName }}</div>
          <div class="dashboard-page__welcome-sub">今天 {{ todayText }} · 上次登录 {{ text(user?.lastLoginTime) }} {{ text(user?.lastLoginIp) }}</div>
        </div>
        <a-button @click="goProfile">个人中心</a-button>
      </div>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="16">
        <a-card title="快捷入口">
          <!-- 二期预留：KPI 概览与待办列表占位（待各业务域 countByXxx 汇总接口） -->
          <a-empty v-if="shortcuts.length === 0" description="暂无可访问的功能模块" />
          <a-row v-else :gutter="[16, 16]">
            <a-col v-for="item in shortcuts" :key="item.path" :xs="12" :sm="8">
              <div class="dashboard-page__shortcut" role="button" tabindex="0" @click="goShortcut(item.path)">
                <div class="dashboard-page__shortcut-icon">
                  <component :is="item.icon" v-if="item.icon" />
                </div>
                <span class="dashboard-page__shortcut-name">{{ item.name }}</span>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="系统信息">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="系统版本">-</a-descriptions-item>
            <a-descriptions-item label="所属角色">
              <a-space v-if="user?.roles?.length" wrap>
                <a-tag v-for="role in user.roles" :key="role" color="blue">{{ role }}</a-tag>
              </a-space>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="上次登录时间">{{ text(user?.lastLoginTime) }}</a-descriptions-item>
            <a-descriptions-item label="上次登录 IP">{{ text(user?.lastLoginIp) }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
        <a-card title="建设进度" class="dashboard-page__milestones">
          <a-timeline>
            <a-timeline-item v-for="item in milestones" :key="item.name" :color="item.status === 'done' ? 'green' : undefined">
              <span class="dashboard-page__milestone-name">{{ item.name }}</span>
              <a-tag v-if="item.current" color="blue" class="dashboard-page__milestone-tag">当前</a-tag>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-page__welcome-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dashboard-page__greeting {
  color: rgba(0, 0, 0, 0.88);
  font-size: 18px;
  font-weight: 600;
}

.dashboard-page__welcome-sub {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.dashboard-page__shortcut {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.dashboard-page__shortcut:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.dashboard-page__shortcut-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--erp-brand);
  font-size: 16px;
  background: var(--erp-brand-light);
  border-radius: 8px;
}

.dashboard-page__shortcut-name {
  color: rgba(0, 0, 0, 0.88);
  font-size: 15px;
}

.dashboard-page__milestones {
  margin-top: 16px;
}

.dashboard-page__milestone-name {
  color: rgba(0, 0, 0, 0.88);
  font-size: 15px;
}

.dashboard-page__milestone-tag {
  margin-left: 8px;
}
</style>
