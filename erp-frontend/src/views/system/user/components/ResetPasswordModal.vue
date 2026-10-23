<script setup lang="ts">
// 重置密码弹窗（页面私有组件）：管理员指定新密码，成功后展示后端返回的初始密码。
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { resetUserPassword } from '../../../../api/user'
import { isPasswordComplexityValid, PASSWORD_COMPLEXITY_MESSAGE } from '../../../../utils/validators'
import type { UserVO } from '../../../../types/api'

const emit = defineEmits<{ saved: [] }>()

const open = ref(false)
const submitting = ref(false)
const user = ref<UserVO | null>(null)
const formRef = ref<FormInstance | null>(null)
const form = reactive({ newPassword: '' })
/** 密码提示交互状态：首次聚焦前为灰色提示，聚焦后按校验结果变红/变绿 */
const pwdTouched = ref(false)

const rules: Record<string, Rule[]> = {
  newPassword: [
    { required: true, message: '请输入新密码' },
    {
      validator: (_rule: Rule, value: string) =>
        isPasswordComplexityValid(value ?? '')
          ? Promise.resolve()
          : Promise.reject(PASSWORD_COMPLEXITY_MESSAGE),
      trigger: ['change', 'blur'],
    },
  ],
}

function show(record: UserVO): void {
  user.value = record
  form.newPassword = ''
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
    const result = await resetUserPassword(user.value!.id, { newPassword: form.newPassword })
    message.success(`密码已重置为：${result.initialPassword}`)
    open.value = false
    emit('saved')
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
    :title="`重置密码 - ${user?.realName ?? ''}`"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
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
