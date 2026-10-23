<script setup lang="ts">
// 分配角色弹窗（页面私有组件）：多选角色全量覆盖提交（PUT /{id}/roles）。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { assignUserRoles } from '../../../../api/user'
import { getRolePage } from '../../../../api/role'
import { STATUS_ENABLED } from '../../../../constants'
import type { RoleVO, UserDetailVO } from '../../../../types/api'

const emit = defineEmits<{ saved: [] }>()

const open = ref(false)
const submitting = ref(false)
const optionLoading = ref(false)
const detail = ref<UserDetailVO | null>(null)
const roleIds = ref<string[]>([])
const options = ref<RoleVO[]>([])

async function show(record: UserDetailVO): Promise<void> {
  detail.value = record
  roleIds.value = record.roleIds ?? []
  open.value = true
  optionLoading.value = true
  try {
    const page = await getRolePage({ pageNum: 1, pageSize: 200, status: STATUS_ENABLED })
    options.value = page.list
  } finally {
    optionLoading.value = false
  }
}

async function handleOk(): Promise<void> {
  submitting.value = true
  try {
    const result = await assignUserRoles(detail.value!.id, { roleIds: roleIds.value })
    message.success(result.permissionRefreshTip || '分配角色成功')
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
    :title="`分配角色 - ${detail?.realName ?? ''}`"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-form layout="vertical">
      <a-form-item label="角色列表">
        <a-select
          v-model:value="roleIds"
          mode="multiple"
          allow-clear
          placeholder="请选择角色（可多选）"
          :loading="optionLoading"
          :options="options.map((role) => ({ label: role.roleName, value: role.id }))"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
