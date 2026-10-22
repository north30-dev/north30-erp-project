<script setup lang="ts">
// 新增/编辑复用弹窗（编码规范第 5 节三件套之一）：open(mode, record?) 驱动，
// 表单对象经作用域插槽下发，提交回调 onSubmit resolve 自动关闭、reject 保持打开。
import { reactive, ref, computed } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import type { CrudMode, CrudForm, CrudRules, CrudRecord, CrudSubmit } from './type'

const props = defineProps<{
  /** 资源名（不含"新增/编辑"前缀，弹窗标题按 mode 自动拼接） */
  title: string
  /** create 模式的表单初始值工厂（每次打开时调用，保证干净状态） */
  initialForm: () => CrudForm
  /** a-form 校验规则 */
  rules?: CrudRules
  /** 提交回调（异步）；失败由 request 拦截器统一提示，这里不再重复弹 */
  onSubmit: CrudSubmit
}>()

const open = ref(false)
const mode = ref<CrudMode>('create')
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)
const form = reactive<CrudForm>({})

const modalTitle = computed(() => `${mode.value === 'create' ? '新增' : '编辑'}${props.title}`)

/** 打开弹窗：create 用工厂重置，edit 浅拷贝回填 */
function show(nextMode: CrudMode, record?: CrudRecord): void {
  mode.value = nextMode
  Object.assign(form, props.initialForm(), mode.value === 'edit' && record ? record : {})
  open.value = true
}

async function handleOk(): Promise<void> {
  try {
    await formRef.value?.validate()
  } catch {
    return // 校验失败：a-form 自动标红，不提交
  }
  submitting.value = true
  try {
    await props.onSubmit({ ...form })
    open.value = false
  } catch {
    // 提交失败：保持弹窗打开，提示已由拦截器弹出
  } finally {
    submitting.value = false
  }
}

defineExpose({ show })
</script>

<template>
  <a-modal
    v-model:open="open"
    :title="modalTitle"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
      <slot :form="form" />
    </a-form>
  </a-modal>
</template>
