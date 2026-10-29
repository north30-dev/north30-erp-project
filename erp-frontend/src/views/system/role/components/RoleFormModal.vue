<script setup lang="ts">
// 角色新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，字段与后端 RoleCreateDTO/RoleUpdateDTO 对齐。
// 编辑模式 roleCode 只读（后端 role_code 不可修改）；version 乐观锁随编辑回填提交。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import AppCrudModal from '../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudRules } from '../../../../components/AppCrudModal/type'
import { createRole, updateRole } from '../../../../api/role'
import { DATA_SCOPE_ALL, DATA_SCOPE_OPTIONS, STATUS_ENABLED, STATUS_OPTIONS } from '../../../../constants'

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<'create' | 'edit'>('create')
const editId = ref<string | null>(null)

/** create 模式初始值工厂（AppCrudModal 每次打开时调用；默认值与后端一致：dataScope=1、status=1） */
function initialForm(): CrudForm {
  return {
    roleCode: '',
    roleName: '',
    dataScope: DATA_SCOPE_ALL,
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 校验规则与后端 DTO 一致：roleCode ≤50 必填、roleName ≤100 必填、remark ≤500 */
const rules: CrudRules = {
  roleCode: [
    { required: true, message: '请输入角色编码' },
    { max: 50, message: '角色编码长度不能超过 50' },
  ],
  roleName: [
    { required: true, message: '请输入角色名称' },
    { max: 100, message: '角色名称长度不能超过 100' },
  ],
}

async function handleSubmit(form: CrudForm): Promise<void> {
  if (mode.value === 'create') {
    await createRole({
      roleCode: form.roleCode as string,
      roleName: form.roleName as string,
      dataScope: form.dataScope as number,
      status: form.status as number,
      remark: (form.remark as string) || null,
    })
    message.success('新增角色成功')
  } else {
    await updateRole(editId.value as string, {
      roleName: form.roleName as string,
      dataScope: form.dataScope as number,
      remark: (form.remark as string) || null,
      version: form.version as number,
    })
    message.success('修改角色成功')
  }
  emit('saved')
}

/** 打开弹窗（edit 时 record 为表格行 RoleVO，含 version/remark 回填） */
function show(nextMode: 'create' | 'edit', record?: Record<string, unknown>): void {
  mode.value = nextMode
  editId.value = nextMode === 'edit' ? (record?.id as string) : null
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="角色"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="角色编码" name="roleCode">
            <a-input
              v-model:value="(form.roleCode as string)"
              placeholder="唯一编码，如 warehouse-operator"
              :disabled="mode === 'edit'"
              autocomplete="off"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="角色名称" name="roleName">
            <a-input v-model:value="(form.roleName as string)" placeholder="请输入角色名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="数据范围" name="dataScope">
            <a-select v-model:value="(form.dataScope as number)" :options="[...DATA_SCOPE_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'create'" :span="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="(form.status as number)" :options="[...STATUS_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="(form.remark as string)" :rows="2" :maxlength="500" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>
    </template>
  </AppCrudModal>
</template>

<style scoped>
.w-full {
  width: 100%;
}
</style>
