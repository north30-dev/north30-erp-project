<script setup lang="ts">
// 附件管理页（F8）：后端 5.9.3 列表接口按业务类型 + 业务主键查询且不分页（返回 List），
// 故本页不接 useTable 分页状态机；bizType/bizId 后端必填（@NotBlank/@NotNull，缺失 400），
// 进入页面不自动查询，由用户先选择/输入条件再查询/上传。业务类型为字典下拉
// （attachment_biz_type，onMounted 拉取一次选项仅存页面内存），表格列翻译为 itemLabel、未命中回退原值。
// 上传/下载/删除按钮分别挂 system:attachment:upload / download / delete 权限点。下载走文件流接口
// （带 Token 的 Blob），文件名直接以行数据 fileName 命名（后端经 Content-Disposition RFC 5987 输出中文名，无需解析响应头）。
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { deleteAttachment, downloadAttachment, getAttachmentList } from '../../../api/attachment'
import { getDictOptions } from '../../../api/dict'
import type { AttachmentVO, DictItemOptionVO } from '../../../types/api'
import { ATTACHMENT_COLUMNS, BIZ_TYPE_DICT_TYPE, buildBizTypeLabelMap, formatFileSize } from './config'
import UploadModal from './components/UploadModal.vue'

const loading = ref(false)
const records = ref<AttachmentVO[]>([])

/** 业务类型字典选项（页面内存 state，仅拉取一次并传给上传弹窗复用） */
const bizTypeOptions = ref<DictItemOptionVO[]>([])

/** 业务类型下拉选项（label/itemLabel、value/itemValue） */
const bizTypeSelectOptions = computed(() =>
  bizTypeOptions.value.map((option) => ({ label: option.itemLabel, value: option.itemValue })),
)

/** 业务类型 itemValue -> itemLabel 映射（表格列翻译，未命中回退原值） */
const bizTypeLabelMap = computed(() => buildBizTypeLabelMap(bizTypeOptions.value))

// bizType 允许 undefined：a-select allow-clear 清空后值为 undefined
const draftQuery = reactive<{ bizType: string | undefined; bizId: string; bizCode: string }>({
  bizType: undefined,
  bizId: '',
  bizCode: '',
})

const uploadModalRef = ref<InstanceType<typeof UploadModal> | null>(null)

onMounted(async () => {
  try {
    bizTypeOptions.value = await getDictOptions(BIZ_TYPE_DICT_TYPE)
  } catch {
    // 字典选项加载失败不阻塞页面（错误提示已由 request 拦截器统一弹出）
  }
})

/** 查询条件是否满足后端必填契约（bizType/bizId） */
function queryReady(): boolean {
  return !!draftQuery.bizType && draftQuery.bizId.trim() !== ''
}

async function load(): Promise<void> {
  loading.value = true
  try {
    records.value = await getAttachmentList({
      bizType: draftQuery.bizType ?? '',
      bizId: draftQuery.bizId.trim(),
      bizCode: draftQuery.bizCode.trim() || null,
    })
  } finally {
    loading.value = false
  }
}

/** 查询：bizType/bizId 必填与后端 DTO 校验一致，缺失时不发起请求 */
async function handleSearch(): Promise<void> {
  if (!queryReady()) {
    message.warning('请选择业务类型并输入业务主键后查询')
    return
  }
  await load()
}

/** 重置：清空条件与列表（条件必填，清空后不发起查询） */
function handleReset(): void {
  draftQuery.bizType = undefined
  draftQuery.bizId = ''
  draftQuery.bizCode = ''
  records.value = []
}

/** 上传成功：条件齐全时按当前条件刷新列表 */
async function handleUploaded(): Promise<void> {
  if (queryReady()) {
    await load()
  }
}

/** 下载附件：文件流经 Blob 落地，文件名取行数据 fileName */
async function handleDownload(record: AttachmentVO): Promise<void> {
  const blob = await downloadAttachment(record.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = record.fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 删除确认（逻辑删除，仅上传人可删，后端 10202 兜底拦截） */
function confirmDelete(record: AttachmentVO): void {
  Modal.confirm({
    title: '删除附件',
    content: `确定删除附件「${record.fileName}」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteAttachment(record.id)
      message.success('删除成功')
      await load()
    },
  })
}
</script>

<template>
  <div class="attachment-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="handleSearch" @reset="handleReset">
        <a-form-item label="业务类型">
          <a-select
            v-model:value="draftQuery.bizType"
            :options="bizTypeSelectOptions"
            placeholder="必填，请选择业务类型"
            show-search
            allow-clear
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item label="业务主键">
          <a-input v-model:value="draftQuery.bizId" placeholder="必填，业务记录 ID" allow-clear />
        </a-form-item>
        <a-form-item label="单据号">
          <a-input v-model:value="draftQuery.bizCode" placeholder="选填" allow-clear />
        </a-form-item>
      </AppSearchForm>

      <a-space class="toolbar">
        <a-button
          v-perm="'system:attachment:upload'"
          type="primary"
          @click="
            uploadModalRef?.show({
              bizType: draftQuery.bizType,
              bizId: draftQuery.bizId,
              bizCode: draftQuery.bizCode,
            })
          "
        >
          <template #icon><UploadOutlined /></template>
          上传附件
        </a-button>
        <span class="toolbar-tip">附件按业务类型 + 业务主键归档，请先输入查询条件</span>
      </a-space>

      <a-table
        row-key="id"
        :columns="ATTACHMENT_COLUMNS"
        :data-source="records"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'fileSize'">
            {{ formatFileSize((record as AttachmentVO).fileSize) }}
          </template>
          <template v-else-if="column.dataIndex === 'bizType'">
            {{ bizTypeLabelMap[(record as AttachmentVO).bizType] ?? (record as AttachmentVO).bizType }}
          </template>
          <template v-else-if="column.dataIndex === 'bizCode'">
            {{ (record as AttachmentVO).bizCode ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-perm="'system:attachment:download'"
                type="link"
                size="small"
                @click="handleDownload(record as AttachmentVO)"
              >
                <DownloadOutlined /> 下载
              </a-button>
              <a-button
                v-perm="'system:attachment:delete'"
                type="link"
                size="small"
                danger
                @click="confirmDelete(record as AttachmentVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <UploadModal ref="uploadModalRef" :biz-type-options="bizTypeOptions" @saved="handleUploaded" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}

.toolbar-tip {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
