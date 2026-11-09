<script setup lang="ts">
// 编号序列重置弹窗（F8 页面私有组件）：动作为"重置流水"而非新增/编辑，语义上不复用 AppCrudModal，自持 a-modal + a-form。
// bizType/period 由行数据回填只读；目标流水号默认当前已用值；reason 必填 ≤200（对齐后端 CodeSequenceResetDTO）。
// 重置区间合法性（序列不存在 18035/重置值冲突 18036/乐观锁冲突 10601）由后端校验，提示由 request 拦截器统一弹出。
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { resetCodeSequence } from '../../../../api/codesequence'
import type { CodeSequenceVO } from '../../../../types/api'

const emit = defineEmits<{ saved: [] }>()

const open = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

const form = reactive({ bizType: '', period: '', currentNo: 0, reason: '' })

/** 校验规则与后端 CodeSequenceResetDTO 一致：reason 必填且 ≤200（bizType/period/currentNo 均由行数据回填） */
const rules = {
  reason: [
    { required: true, message: '请输入重置原因' },
    { max: 200, message: '重置原因长度不能超过 200' },
  ],
}

/** 打开弹窗：按行回填 bizType/period，目标流水号默认当前已用值 */
function show(record: CodeSequenceVO): void {
  form.bizType = record.bizType
  form.period = record.period
  form.currentNo = record.currentNo
  form.reason = ''
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
    await resetCodeSequence({
      bizType: form.bizType,
      period: form.period,
      currentNo: form.currentNo,
      reason: form.reason.trim(),
    })
    message.success('重置编号流水成功')
    open.value = false
    emit('saved')
  } catch {
    // 提交失败：保持弹窗打开，提示已由 request 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

defineExpose({ show })
</script>

<template>
  <a-modal
    v-model:open="open"
    title="重置编号流水"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="单据类型">
            <a-input :value="form.bizType" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="期间">
            <a-input :value="form.period" disabled />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="目标流水号" name="currentNo">
            <a-input-number v-model:value="form.currentNo" :min="0" :precision="0" class="full-width" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="重置原因" name="reason">
            <a-textarea
              v-model:value="form.reason"
              :rows="3"
              :maxlength="200"
              placeholder="请输入重置原因（必填）"
              show-count
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<style scoped>
.full-width {
  width: 100%;
}
</style>
