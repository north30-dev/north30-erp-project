import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '../stores/auth'

/**
 * 按钮级权限指令（编码规范第 5 节）：
 *   <a-button v-perm="'system:user:create'">新增</a-button>
 * 无权限时直接移除 DOM 元素（相比 disabled 更安全，避免误点击发出非法请求）。
 * 管理员（isAdmin）放行全部权限点，与后端 UserAccessService 语义一致。
 */
export const perm: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const auth = useAuthStore()
    if (!auth.hasPerm(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}
