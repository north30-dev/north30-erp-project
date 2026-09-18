import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 应用全局状态（组合式写法） */
export const useAppStore = defineStore('app', () => {
  /** 侧边栏是否折叠 */
  const collapsed = ref(false)

  /** 切换侧边栏折叠状态 */
  function toggleCollapsed(): void {
    collapsed.value = !collapsed.value
  }

  return { collapsed, toggleCollapsed }
})
