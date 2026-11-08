<script setup lang="ts">
// 分配角色弹窗（页面私有组件）：多选角色全量覆盖提交（PUT /{id}/roles）。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { assignUserRoles } from '../../../../api/user'
import { getRolePage } from '../../../../api/role'
import { useAuthStore } from '../../../../stores/auth'
import { STATUS_ENABLED } from '../../../../constants'
import type { RoleVO, UserDetailVO } from '../../../../types/api'

const emit = defineEmits<{ saved: [] }>()

const auth = useAuthStore()

const open = ref(false)
const submitting = ref(false)
const optionLoading = ref(false)
const detail = ref<UserDetailVO | null>(null)
const roleIds = ref<string[]>([])
const options = ref<RoleVO[]>([])
/** 非超管给自己分配角色时锁定的 ADMIN 角色 ID（后端 18041 禁止移除） */
const lockedAdminRoleId = ref<string | null>(null)

async function show(record: UserDetailVO): Promise<void> {
  detail.value = record
  roleIds.value = record.roleIds ?? []
  lockedAdminRoleId.value = null
  open.value = true
  optionLoading.value = true
  try {
    const page = await getRolePage({ pageNum: 1, pageSize: 200, status: STATUS_ENABLED })
    options.value = page.list
    // 保护：非超管不能摘除自己当前持有的 ADMIN 角色（与后端 18041 一致，避免自我降权无法自愈）
    const adminRole = page.list.find((role) => role.roleCode === 'ADMIN')
    const isSelf = record.id === auth.userInfo?.userId
    if (isSelf && !auth.isAdmin && adminRole && roleIds.value.includes(adminRole.id)) {
      lockedAdminRoleId.value = adminRole.id
    }
  } finally {
    optionLoading.value = false
  }
}

/** 勾选变化：锁定角色被移除时自动回填并提示 */
function handleRoleChange(ids: string[]): void {
  if (lockedAdminRoleId.value && !ids.includes(lockedAdminRoleId.value)) {
    roleIds.value = [...ids, lockedAdminRoleId.value]
    message.warning('不可移除自己持有的超级管理员角色')
    return
  }
  roleIds.value = ids
}

async function handleOk(): Promise<void> {
  if (lockedAdminRoleId.value && !roleIds.value.includes(lockedAdminRoleId.value)) {
    message.warning('不可移除自己持有的超级管理员角色')
    return
  }
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
          :value="roleIds"
          mode="multiple"
          allow-clear
          placeholder="请选择角色（可多选）"
          :loading="optionLoading"
          :options="options.map((role) => ({
            label: role.roleName,
            value: role.id,
            disabled: role.id === lockedAdminRoleId,
          }))"
          @change="handleRoleChange"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
