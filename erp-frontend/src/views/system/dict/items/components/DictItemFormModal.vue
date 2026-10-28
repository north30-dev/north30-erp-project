<script setup lang="ts">
// 字典项新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，字段与后端 DictItemCreateDTO/DictItemUpdateDTO 对齐。
// dictType 由路由注入（create 提交时携带）；编辑模式 itemValue 只读（后端 item_value 不可修改）。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import AppCrudModal from '../../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudRules } from '../../../../../components/AppCrudModal/type'
import { createDictItem, updateDictItem } from '../../../../../api/dict'
import {
  DICT_LANG_DEFAULT,
  IS_DEFAULT_NO,
  IS_DEFAULT_OPTIONS,
  STATUS_ENABLED,
  STATUS_OPTIONS,
} from '../../../../../constants'

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<'create' | 'edit'>('create')
const editId = ref<string | null>(null)
/** 当前字典类型编码（父组件 show() 时注入） */
const dictType = ref('')

/** create 模式初始值工厂（默认值与后端一致：lang=zh-CN、isDefault=0、status=1） */
function initialForm(): CrudForm {
  return {
    itemLabel: '',
    itemValue: '',
    lang: DICT_LANG_DEFAULT,
    itemSort: 0,
    cssClass: '',
    isDefault: IS_DEFAULT_NO,
    extJson: '',
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 校验规则与后端 DTO 一致：itemLabel ≤200 必填、itemValue ≤100 必填、lang ≤10、cssClass ≤100、remark ≤500 */
const rules: CrudRules = {
  itemLabel: [
    { required: true, message: '请输入字典项标签' },
    { max: 200, message: '字典项标签长度不能超过 200' },
  ],
  itemValue: [
    { required: true, message: '请输入字典项值' },
    { max: 100, message: '字典项值长度不能超过 100' },
  ],
  lang: [{ max: 10, message: '语言编码长度不能超过 10' }],
  cssClass: [{ max: 100, message: '标签样式长度不能超过 100' }],
  extJson: [
    {
      validator: (_rule: unknown, value: unknown) => {
        const text = typeof value === 'string' ? value.trim() : ''
        if (!text) return Promise.resolve()
        try {
          JSON.parse(text)
          return Promise.resolve()
        } catch {
          return Promise.reject('扩展属性必须为合法 JSON')
        }
      },
    },
  ],
}

async function handleSubmit(form: CrudForm): Promise<void> {
  const extJson = (form.extJson as string)?.trim() || null
  if (mode.value === 'create') {
    await createDictItem({
      dictType: dictType.value,
      itemLabel: form.itemLabel as string,
      itemValue: form.itemValue as string,
      lang: (form.lang as string) || null,
      itemSort: (form.itemSort as number) ?? null,
      cssClass: (form.cssClass as string) || null,
      isDefault: form.isDefault as number,
      extJson,
      status: form.status as number,
      remark: (form.remark as string) || null,
    })
    message.success('新增字典项成功')
  } else {
    await updateDictItem(editId.value as string, {
      itemLabel: form.itemLabel as string,
      itemSort: (form.itemSort as number) ?? null,
      cssClass: (form.cssClass as string) || null,
      isDefault: form.isDefault as number,
      extJson,
      status: form.status as number,
      remark: (form.remark as string) || null,
      version: form.version as number,
    })
    message.success('修改字典项成功')
  }
  emit('saved')
}

/** 打开弹窗（create 需传入 dictType；edit 时 record 为表格行 DictItemVO，含 version 回填） */
function show(nextMode: 'create' | 'edit', record?: Record<string, unknown>): void {
  mode.value = nextMode
  if (nextMode === 'create') {
    dictType.value = (record?.dictType as string) ?? ''
    editId.value = null
  } else {
    dictType.value = record?.dictType as string
    editId.value = record?.id as string
  }
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="字典项"
    :width="640"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="字典项标签" name="itemLabel">
            <a-input v-model:value="(form.itemLabel as string)" placeholder="展示文案，如 启用" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="字典项值" name="itemValue">
            <a-input
              v-model:value="(form.itemValue as string)"
              placeholder="编码值，如 enabled"
              :disabled="mode === 'edit'"
              autocomplete="off"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="语言编码" name="lang">
            <a-input v-model:value="(form.lang as string)" placeholder="如 zh-CN" autocomplete="off" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="显示顺序" name="itemSort">
            <a-input-number v-model:value="(form.itemSort as number)" :min="0" class="w-full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="标签样式" name="cssClass">
            <a-input v-model:value="(form.cssClass as string)" placeholder="前端标签样式，选填" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="是否默认" name="isDefault">
            <a-radio-group v-model:value="(form.isDefault as number)" :options="[...IS_DEFAULT_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="(form.status as number)" :options="[...STATUS_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="扩展属性" name="extJson">
            <a-textarea
              v-model:value="(form.extJson as string)"
              :rows="2"
              placeholder="JSON 字符串，选填，如 {&quot;color&quot;: &quot;red&quot;}"
            />
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
