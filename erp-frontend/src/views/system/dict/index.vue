<script setup lang="ts">
// 字典类型列表页（F5 主表）：AppSearchForm + useTable + AppCrudModal 三件套骨架，写法对齐 F1/F2 基准。
// 主子表跳转式：操作列"字典项"按钮携带 dictType 编码跳转 /system/dict/items。
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { ColumnsType } from 'ant-design-vue/es/table'
import {
  DeleteOutlined,
  EditOutlined,
  OrderedListOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { useTable } from '../../../composables/useTable'
import { deleteDictType, getDictTypePage, updateDictType } from '../../../api/dict'
import { STATUS_DISABLED, STATUS_ENABLED, STATUS_OPTIONS } from '../../../constants'
import type { DictTypePageQueryDTO, DictTypeVO } from '../../../types/api'
import DictTypeFormModal from './components/DictTypeFormModal.vue'

/** 列定义（列名与后端 DictTypeVO 字段一一对应） */
const columns: ColumnsType<DictTypeVO> = [
  { title: '类型编码', dataIndex: 'dictType', width: 180 },
  { title: '类型名称', dataIndex: 'dictName', width: 180 },
  { title: '字典项数', dataIndex: 'itemCount', width: 100 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' },
]

const router = useRouter()

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange } =
  useTable<Omit<DictTypePageQueryDTO, 'pageNum' | 'pageSize'>, DictTypeVO>(getDictTypePage, {
    dictType: '',
    dictName: '',
    status: undefined,
  })

const formModalRef = ref<InstanceType<typeof DictTypeFormModal> | null>(null)

const statusSavingId = ref<string | null>(null)

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})

/** 状态开关：携带行内 version 走全量更新接口提交乐观锁，成功后整表刷新 */
async function handleStatusChange(record: DictTypeVO, checked: boolean): Promise<void> {
  statusSavingId.value = record.id
  try {
    await updateDictType(record.id, {
      dictName: record.dictName,
      status: checked ? STATUS_ENABLED : STATUS_DISABLED,
      remark: record.remark,
      version: record.version,
    })
    await reload()
  } finally {
    statusSavingId.value = null
  }
}

/** 删除确认（危险操作统一 Modal.confirm + danger 按钮；类型下存在字典项时后端 18030 拦截） */
function confirmDelete(record: DictTypeVO): void {
  Modal.confirm({
    title: '删除字典类型',
    content: `确定删除字典类型「${record.dictName}（${record.dictType}）」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteDictType(record.id)
      message.success('删除成功')
      await reload()
    },
  })
}

/** 跳转字典项页（主子表主从跳转，携带类型编码过滤） */
function goItems(record: DictTypeVO): void {
  void router.push({ path: '/system/dict/items', query: { dictType: record.dictType } })
}
</script>

<template>
  <div class="dict-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="类型编码">
          <a-input v-model:value="draftQuery.dictType" placeholder="请输入类型编码" allow-clear />
        </a-form-item>
        <a-form-item label="类型名称">
          <a-input v-model:value="draftQuery.dictName" placeholder="请输入类型名称" allow-clear />
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
        <a-button v-perm="'system:dict:create'" type="primary" @click="formModalRef?.show('create')">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </a-space>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1130 }"
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
          <template v-if="column.dataIndex === 'itemCount'">
            <a-tag color="blue">{{ (record as DictTypeVO).itemCount }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-switch
              :checked="(record as DictTypeVO).status === STATUS_ENABLED"
              :loading="statusSavingId === (record as DictTypeVO).id"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: boolean) => handleStatusChange(record as DictTypeVO, checked)"
            />
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            {{ (record as DictTypeVO).remark ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-perm="'system:dict:update'"
                type="link"
                size="small"
                @click="formModalRef?.show('edit', { ...(record as DictTypeVO) })"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-button
                v-perm="'system:dict:list'"
                type="link"
                size="small"
                @click="goItems(record as DictTypeVO)"
              >
                <OrderedListOutlined /> 字典项
              </a-button>
              <a-button
                v-perm="'system:dict:delete'"
                type="link"
                size="small"
                danger
                @click="confirmDelete(record as DictTypeVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <DictTypeFormModal ref="formModalRef" @saved="reload" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
