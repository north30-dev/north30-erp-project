<script setup lang="ts">
// 字典项列表页（F5 子表，主从跳转式变体）：三件套骨架；dictType 由路由 query 注入为固定过滤条件。
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { ColumnsType } from 'ant-design-vue/es/table'
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import AppSearchForm from '../../../../components/AppSearchForm/index.vue'
import { useTable } from '../../../../composables/useTable'
import { deleteDictItem, getDictItemPage, updateDictItem } from '../../../../api/dict'
import {
  DICT_LANG_DEFAULT,
  IS_DEFAULT_YES,
  STATUS_DISABLED,
  STATUS_ENABLED,
  STATUS_OPTIONS,
} from '../../../../constants'
import type { DictItemPageQueryDTO, DictItemVO } from '../../../../types/api'
import DictItemFormModal from './components/DictItemFormModal.vue'
/** 列定义（列名与后端 DictItemVO 字段一一对应） */
const columns: ColumnsType<DictItemVO> = [
  { title: '字典项标签', dataIndex: 'itemLabel', width: 160 },
  { title: '字典项值', dataIndex: 'itemValue', width: 140 },
  { title: '语言', dataIndex: 'lang', width: 90 },
  { title: '排序', dataIndex: 'itemSort', width: 80 },
  { title: '默认', dataIndex: 'isDefault', width: 80 },
  { title: '标签样式', dataIndex: 'cssClass', width: 120 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '缓存', dataIndex: 'cached', width: 80 },
  { title: '备注', dataIndex: 'remark', width: 160 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' },
]

const route = useRoute()
const router = useRouter()

/** 当前字典类型编码（后端 DictItemQueryDTO.dictType 必填，缺失 10001） */
const currentDictType = (route.query.dictType as string) ?? ''

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange } =
  useTable<Omit<DictItemPageQueryDTO, 'pageNum' | 'pageSize' | 'dictType'>, DictItemVO>(getDictItemPage, {
    lang: '',
    status: undefined,
  })

const formModalRef = ref<InstanceType<typeof DictItemFormModal> | null>(null)

const statusSavingId = ref<string | null>(null)

onMounted(() => {
  // 无 dictType 参数属异常进入（如手动输 URL），提示后返回类型页
  if (!currentDictType) {
    message.warning('请从字典类型页进入字典项管理')
    void router.replace('/system/dict')
    return
  }
  void search()
})

/** 返回字典类型页 */
function goBack(): void {
  void router.push('/system/dict')
}

/** 状态开关：携带行内 version 走全量更新接口提交乐观锁，成功后整表刷新 */
async function handleStatusChange(record: DictItemVO, checked: boolean): Promise<void> {
  statusSavingId.value = record.id
  try {
    await updateDictItem(record.id, {
      itemLabel: record.itemLabel,
      itemSort: record.itemSort,
      cssClass: record.cssClass,
      isDefault: record.isDefault,
      extJson: record.extJson,
      status: checked ? STATUS_ENABLED : STATUS_DISABLED,
      remark: record.remark,
      version: record.version,
    })
    await reload()
  } finally {
    statusSavingId.value = null
  }
}

/** 删除确认（危险操作统一 Modal.confirm + danger 按钮） */
function confirmDelete(record: DictItemVO): void {
  Modal.confirm({
    title: '删除字典项',
    content: `确定删除字典项「${record.itemLabel}（${record.itemValue}）」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteDictItem(record.id)
      message.success('删除成功')
      await reload()
    },
  })
}
</script>

<template>
  <div class="dict-items-page">
    <a-card>
      <a-space class="page-header">
        <a-button @click="goBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <a-typography-title :level="4" class="page-title">
          字典项管理 - {{ currentDictType }}
        </a-typography-title>
      </a-space>

      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="语言">
          <a-input v-model:value="draftQuery.lang" :placeholder="`如 ${DICT_LANG_DEFAULT}`" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="draftQuery.status"
            :options="[...STATUS_OPTIONS]"
            placeholder="请选择状态"
            allow-clear
          />
        </a-form-item>
      </AppSearchForm>

      <a-space class="toolbar">
        <a-button
          v-perm="'system:dict:create'"
          type="primary"
          @click="formModalRef?.show('create', { dictType: currentDictType })"
        >
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </a-space>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1160 }"
        :pagination="{
          current: pageNum,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (count: number) => `共 ${count} 条`,
        }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'isDefault'">
            <a-tag v-if="(record as DictItemVO).isDefault === IS_DEFAULT_YES" color="green">默认</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-switch
              :checked="(record as DictItemVO).status === STATUS_ENABLED"
              :loading="statusSavingId === (record as DictItemVO).id"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: boolean) => handleStatusChange(record as DictItemVO, checked)"
            />
          </template>
          <template v-else-if="column.dataIndex === 'cached'">
            <a-tag v-if="(record as DictItemVO).cached" color="cyan">已缓存</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'cssClass'">
            {{ (record as DictItemVO).cssClass ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            {{ (record as DictItemVO).remark ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-perm="'system:dict:update'"
                type="link"
                size="small"
                @click="formModalRef?.show('edit', { ...(record as DictItemVO) })"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-button
                v-perm="'system:dict:delete'"
                type="link"
                size="small"
                danger
                @click="confirmDelete(record as DictItemVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <DictItemFormModal ref="formModalRef" @saved="reload" />
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
}
</style>
