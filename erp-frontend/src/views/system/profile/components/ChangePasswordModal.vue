<script setup lang="ts">
// 修改本人密码弹窗（个人中心私有组件）：提交成功后后端吊销全部会话，前端清理登录态并回登录页。
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { changePassword } from '../../../../api/auth'
import { useAuthStore } from '../../../../stores/auth'
import { resetDynamicRoutes } from '../../../../router/dynamicRoutes'
import { isPasswordComplexityValid, PASSWORD_COMPLEXITY_MESSAGE } from '../../../../utils/validators'

const router = useRouter()
const auth = useAuthStore()

const open = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
/** 密码提示交互状态：首次聚焦前为灰色提示，聚焦后按校验结果变红/变绿 */
const pwdTouched = ref(false)

const rules: Record<string, Rule[]> = {
  oldPassword: [
    { required: true, message: '请输入原密码' },
    { max: 64, message: '原密码长度不能超过 64' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { max: 64, message: '新密码长度不能超过 64' },
    {
      validator: (_rule: Rule, value: string) =>
        isPasswordComplexityValid(value ?? '')
          ? Promise.resolve()
          : Promise.reject(PASSWORD_COMPLEXITY_MESSAGE),
      trigger: ['change', 'blur'],
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码' },
    { max: 64, message: '确认新密码长度不能超过 64' },
    {
      validator: (_rule: Rule, value: string) =>
        value === form.newPassword ? Promise.resolve() : Promise.reject('两次输入的新密码不一致'),
      trigger: ['change', 'blur'],
    },
  ],
}

function show(): void {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  pwdTouched.value = false
  open.value = true
}

/** 密码框校验状态：未交互不标色，交互后按复杂度给出 success/error */
function pwdStatus(value: string): '' | 'success' | 'error' {
  if (!pwdTouched.value) {
    return ''
  }
  return isPasswordComplexityValid(value ?? '') ? 'success' : 'error'
}

async function handleOk(): Promise<void> {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    open.value = false
    message.success('密码修改成功，请重新登录')
    // 后端已吊销全部会话，不再调 logout；清本地登录态并移除动态路由后回登录页
    auth.clearSession()
    resetDynamicRoutes(router)
    await router.replace('/login')
  } catch {
    // 失败提示由 request 拦截器统一弹出，保持弹窗打开
  } finally {
    submitting.value = false
  }
}

defineExpose({ show })
</script>

<template>
  <a-modal
    v-model:open="open"
    title="修改密码"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
      <a-form-item label="原密码" name="oldPassword">
        <a-input-password
          v-model:value="form.oldPassword"
          placeholder="请输入原密码"
          autocomplete="current-password"
        />
      </a-form-item>
      <a-form-item
        label="新密码"
        name="newPassword"
        :validate-status="pwdStatus(form.newPassword)"
      >
        <a-input-password
          v-model:value="form.newPassword"
          placeholder="请输入新密码"
          autocomplete="new-password"
          @focus="pwdTouched = true"
        />
        <template #help>
          <span
            :class="[
              'pwd-hint',
              pwdTouched
                ? isPasswordComplexityValid(form.newPassword ?? '')
                  ? 'pwd-hint--ok'
                  : 'pwd-hint--error'
                : '',
            ]"
          >
            {{ PASSWORD_COMPLEXITY_MESSAGE }}
          </span>
        </template>
      </a-form-item>
      <a-form-item label="确认新密码" name="confirmPassword">
        <a-input-password
          v-model:value="form.confirmPassword"
          placeholder="请再次输入新密码"
          autocomplete="new-password"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
/* 密码复杂度提示三态：默认灰、校验通过绿、不通过红（取 antd 标准色板） */
.pwd-hint {
  color: rgba(0, 0, 0, 0.45);
}
.pwd-hint--ok {
  color: #52c41a;
}
.pwd-hint--error {
  color: #ff4d4f;
}
</style>
