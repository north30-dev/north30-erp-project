import type { App } from 'vue'
import { perm } from './perm'

/** 全局自定义指令统一注册（编码规范第 1 节：指令集中在 directives 管理） */
export function setupDirectives(app: App): void {
  app.directive('perm', perm)
}
