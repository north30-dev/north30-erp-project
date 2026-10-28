<script setup lang="ts">
// 字典类型新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，字段与后端 DictTypeCreateDTO/DictTypeUpdateDTO 对齐。
// 编辑模式 dictType 只读（后端 dict_type 不可修改）；version 乐观锁随编辑回填提交。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import AppCrudModal from '../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudRules } from '../../../../components/AppCrudModal/type'
import { createDictType, updateDictType } from '../../../../api/dict'
import { STATUS_ENABLED, STATUS_OPTIONS } from '../../../../constants'

/** 与后端 DictTypeCreateDTO @Pattern 一致：小写字母开头，小写字母/数字/下划线 */
const DICT_TYPE_PATTERN = /^[a-z][a-z0-9_]*$/

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<'create' | 'edit'>('create')
const editId = ref<string | null>(null)

/** create 模式初始值工厂（默认值与后端一致：status=1） */
function initialForm(): CrudForm {
  return {
    dictType: '',
    dictName: '',
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 校验规则与后端 DTO 一致：dictType ≤100 且格式受限、dictName ≤100 必填、remark ≤500 */
const rules: CrudRules = {
  dictType: [
    { required: true, message: '请输入字典类型编码' },
    { max: 100, message: '字典类型编码长度不能超过 100' },
    { pattern: DICT_TYPE_PATTERN, message: '仅允许小写字母开头，由小写字母、数字与下划线组成' },
  ],
  dictName: [
    { required: true, message: '请输入字典类型名称' },
    { max: 100, message: '字典类型名称长度不能超过 100' },
  ],
}

async function handleSubmit(form: CrudForm): Promise<void> {
  if (mode.value === 'create') {
    await createDictType({
      dictType: form.dictType as string,
      dictName: form.dictName as string,
      status: form.status as number,
      remark: (form.remark as string) || null,
    })
    message.success('新增字典类型成功')
  } else {
    await updateDictType(editId.value as string, {
      dictName: form.dictName as string,
      status: form.status as number,
      remark: (form.remark as string) || null,
      version: form.version as number,
    })
    message.success('修改字典类型成功')
  }
  emit('saved')
}

/** 打开弹窗（edit 时 record 为表格行 DictTypeVO，含 version/remark 回填） */
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
    title="字典类型"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="类型编码" name="dictType">
            <a-input
              v-model:value="(form.dictType as string)"
              placeholder="唯一编码，如 user_status"
              :disabled="mode === 'edit'"
              autocomplete="off"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="类型名称" name="dictName">
            <a-input v-model:value="(form.dictName as string)" placeholder="请输入类型名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
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
