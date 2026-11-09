<script setup lang="ts">
// 系统参数新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，字段与后端 ConfigCreateDTO/ConfigUpdateDTO 对齐。
// 编辑模式 configKey/valueType/configGroup 只读（后端不允许修改），isSystem/status 不进编辑表单
// （内置标记列表页展示、状态由列表页开关维护）；version 乐观锁随编辑回填提交。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import AppCrudModal from '../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudRules } from '../../../../components/AppCrudModal/type'
import { createConfig, updateConfig } from '../../../../api/config'
import { STATUS_ENABLED, STATUS_OPTIONS } from '../../../../constants'
import {
  IS_SYSTEM_NO,
  IS_SYSTEM_OPTIONS,
  VALUE_TYPE_BOOLEAN,
  VALUE_TYPE_JSON,
  VALUE_TYPE_NUMBER,
  VALUE_TYPE_OPTIONS,
  VALUE_TYPE_STRING,
} from '../config'

/** 与后端 ConfigCreateDTO @Pattern 一致：小写域前缀.参数名，至少两段（如 system.page.default.size） */
const CONFIG_KEY_PATTERN = /^[a-z][a-z0-9]*(\.[a-z][a-z0-9-]*)+$/

/** 数字参数值前置校验（BigDecimal 可解析语义，与后端 18034 校验一致） */
const NUMBER_VALUE_PATTERN = /^[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?$/

/** 布尔参数值前置校验（后端仅允许 true/false，忽略大小写，18034） */
const BOOLEAN_VALUE_PATTERN = /^(true|false)$/i

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<'create' | 'edit'>('create')
const editId = ref<string | null>(null)
/** 当前值类型（configValue 分型校验依据：edit 打开时回填，create 由下拉 change 同步） */
const currentValueType = ref(VALUE_TYPE_STRING)

/** create 模式初始值工厂（默认值与后端 applyCreateDefault 一致：isSystem=0、status=1） */
function initialForm(): CrudForm {
  return {
    configKey: '',
    configName: '',
    configValue: '',
    valueType: VALUE_TYPE_STRING,
    configGroup: '',
    isSystem: IS_SYSTEM_NO,
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 按值类型校验参数值（与后端 SysConfig.validateValue 一致：数字可解析、布尔仅 true/false、JSON 可解析） */
function validateValueByType(_rule: Rule, value?: string): Promise<void> {
  const text = value ?? ''
  if (!text) {
    return Promise.resolve() // 空值交给 required 规则
  }
  if (currentValueType.value === VALUE_TYPE_NUMBER && !NUMBER_VALUE_PATTERN.test(text)) {
    return Promise.reject('数字类型参数值须可解析为数字（如 100、0.5、1E+2）')
  }
  if (currentValueType.value === VALUE_TYPE_BOOLEAN && !BOOLEAN_VALUE_PATTERN.test(text)) {
    return Promise.reject('布尔类型参数值仅允许 true / false')
  }
  if (currentValueType.value === VALUE_TYPE_JSON) {
    try {
      JSON.parse(text)
    } catch {
      return Promise.reject('JSON 类型参数值须为合法 JSON')
    }
  }
  return Promise.resolve()
}

/** 校验规则与后端 DTO 一致：configKey ≤100 且小写域前缀格式、configName ≤200、configValue ≤500 分型校验、configGroup ≤50、remark ≤500 */
const rules: CrudRules = {
  configKey: [
    { required: true, message: '请输入参数键' },
    { max: 100, message: '参数键长度不能超过 100' },
    { pattern: CONFIG_KEY_PATTERN, message: '参数键必须为小写域前缀.参数名格式（如 system.page.default.size）' },
  ],
  configName: [
    { required: true, message: '请输入参数名称' },
    { max: 200, message: '参数名称长度不能超过 200' },
  ],
  configValue: [
    { required: true, message: '请输入参数值' },
    { max: 500, message: '参数值长度不能超过 500' },
    { validator: validateValueByType },
  ],
  valueType: [{ required: true, message: '请选择值类型' }],
  configGroup: [
    { required: true, message: '请输入参数分组' },
    { max: 50, message: '参数分组长度不能超过 50' },
  ],
}

async function handleSubmit(form: CrudForm): Promise<void> {
  if (mode.value === 'create') {
    await createConfig({
      configKey: form.configKey as string,
      configName: form.configName as string,
      configValue: form.configValue as string,
      valueType: form.valueType as number,
      configGroup: form.configGroup as string,
      isSystem: form.isSystem as number,
      status: form.status as number,
      remark: (form.remark as string) || null,
    })
    message.success('新增系统参数成功')
  } else {
    const result = await updateConfig(editId.value as string, {
      configValue: form.configValue as string,
      configName: (form.configName as string) || null,
      remark: (form.remark as string) || null,
      version: form.version as number,
    })
    message.success(`修改系统参数成功，${result.effectNote}`)
  }
  emit('saved')
}

/** 打开弹窗（edit 时 record 为表格行 ConfigVO，含 version/remark/valueType 回填） */
function show(nextMode: 'create' | 'edit', record?: Record<string, unknown>): void {
  mode.value = nextMode
  editId.value = nextMode === 'edit' ? (record?.id as string) : null
  currentValueType.value = nextMode === 'edit' && record ? (record.valueType as number) : VALUE_TYPE_STRING
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="系统参数"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
    :width="640"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="参数键" name="configKey">
            <a-input
              v-model:value="(form.configKey as string)"
              placeholder="小写域前缀.参数名，如 system.page.default.size"
              :disabled="mode === 'edit'"
              autocomplete="off"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="参数名称" name="configName">
            <a-input v-model:value="(form.configName as string)" placeholder="请输入参数名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="值类型" name="valueType">
            <a-select
              v-model:value="(form.valueType as number)"
              :options="[...VALUE_TYPE_OPTIONS]"
              placeholder="请选择值类型"
              :disabled="mode === 'edit'"
              @change="(value: number) => (currentValueType = value)"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="参数分组" name="configGroup">
            <a-input
              v-model:value="(form.configGroup as string)"
              placeholder="如 system、inventory"
              :disabled="mode === 'edit'"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="参数值" name="configValue">
            <a-textarea
              v-model:value="(form.configValue as string)"
              :rows="3"
              :maxlength="500"
              placeholder="请输入参数值（布尔仅 true/false，JSON 须合法）"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'create'" :span="12">
          <a-form-item label="是否内置" name="isSystem">
            <a-radio-group v-model:value="(form.isSystem as number)" :options="[...IS_SYSTEM_OPTIONS]" />
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
