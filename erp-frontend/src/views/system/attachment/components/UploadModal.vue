<script setup lang="ts">
// 附件上传弹窗（F8 页面私有组件）：multipart 提交 file + bizType/bizId/bizCode，字段与后端 AttachmentBizQueryDTO 对齐。
// a-upload beforeUpload 返回 false 拦截自动上传，文件手动收集进 FormData；文件类型/大小/数量校验由后端兜底（10401/10402/10403）。
// 动作为"上传"而非新增/编辑，语义上不复用 AppCrudModal，自持 a-modal + a-form。
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import type { UploadFile } from 'ant-design-vue/es/upload/interface'
import { UploadOutlined } from '@ant-design/icons-vue'
import { uploadAttachment } from '../../../../api/attachment'
import { UPLOAD_ACCEPT, UPLOAD_SIZE_TIP } from '../config'

const emit = defineEmits<{ saved: [] }>()

const open = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance | null>(null)

/** 已选文件（手动收集，仅保留最新一个） */
const fileList = ref<UploadFile[]>([])

const form = reactive({ bizType: '', bizId: '', bizCode: '' })

/** 校验规则与后端 AttachmentBizQueryDTO 一致：bizType/bizId 必填、bizCode ≤64 */
const rules = {
  bizType: [{ required: true, message: '请输入业务类型' }],
  bizId: [{ required: true, message: '请输入业务主键' }],
  bizCode: [{ max: 64, message: '单据号长度不能超过 64' }],
}

function beforeUpload(): boolean {
  return false
}

/** 提交上传：bizId 后端 Long 以字符串传输，bizCode 选填为空不传 */
async function handleOk(): Promise<void> {
  try {
    await formRef.value?.validate()
  } catch {
    return // 校验失败：a-form 自动标红，不提交
  }
  const file = fileList.value[0]?.originFileObj
  if (!file) {
    message.warning('请选择要上传的文件')
    return
  }
  submitting.value = true
  try {
    await uploadAttachment(file, {
      bizType: form.bizType.trim(),
      bizId: form.bizId.trim(),
      bizCode: form.bizCode.trim() || null,
    })
    message.success('上传成功')
    open.value = false
    emit('saved')
  } catch {
    // 提交失败：保持弹窗打开，提示已由 request 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

/** 打开弹窗（可预填业务类型/业务主键/单据号，来源为当前查询条件） */
function show(record?: { bizType?: string; bizId?: string; bizCode?: string }): void {
  form.bizType = record?.bizType ?? ''
  form.bizId = record?.bizId ?? ''
  form.bizCode = record?.bizCode ?? ''
  fileList.value = []
  open.value = true
}

defineExpose({ show })
</script>

<template>
  <a-modal
    v-model:open="open"
    title="上传附件"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="业务类型" name="bizType">
            <a-input v-model:value="form.bizType" placeholder="附件所属业务类型，如 CUSTOMER" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="业务主键" name="bizId">
            <a-input v-model:value="form.bizId" placeholder="附件所属业务记录 ID" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="单据号" name="bizCode">
            <a-input v-model:value="form.bizCode" placeholder="选填，附件所属业务单据编码" :maxlength="64" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="附件文件" required>
            <a-upload
              v-model:file-list="fileList"
              :accept="UPLOAD_ACCEPT"
              :max-count="1"
              :before-upload="beforeUpload"
            >
              <a-button>
                <UploadOutlined /> 选择文件
              </a-button>
            </a-upload>
          </a-form-item>
          <p class="upload-tip">{{ UPLOAD_SIZE_TIP }}</p>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<style scoped>
.upload-tip {
  margin: -8px 0 0;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
