<script setup lang="ts">
// 参数配置列表页（F6）：AppSearchForm + useTable + AppCrudModal 三件套骨架，写法对齐 F4/F5 基准。
// 系统内置参数（is_system=1）不可停用/删除（后端 18033 拦截），前端同步禁用开关与删除按钮；
// 状态由列表页开关维护，编辑弹窗仅覆盖 ConfigUpdateDTO 允许的 configValue/configName/remark。
import { onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { useTable } from '../../../composables/useTable'
import { changeConfigStatus, deleteConfig, getConfigPage } from '../../../api/config'
import { IS_SYSTEM_YES, STATUS_DISABLED, STATUS_ENABLED, STATUS_OPTIONS } from '../../../constants'
import type { ConfigQueryDTO, ConfigVO } from '../../../types/api'
import { CONFIG_COLUMNS, VALUE_TYPE_MAP } from './config'
import ConfigFormModal from './components/ConfigFormModal.vue'

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange } =
  useTable<Omit<ConfigQueryDTO, 'pageNum' | 'pageSize'>, ConfigVO>(getConfigPage, {
    configKey: '',
    configName: '',
    configGroup: '',
    status: undefined,
  })

const formModalRef = ref<InstanceType<typeof ConfigFormModal> | null>(null)

const statusSavingId = ref<string | null>(null)

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})

/** 状态开关：走专用 status 动作接口（08 文档阶段四状态开关统一），成功后整表刷新 */
async function handleStatusChange(record: ConfigVO, checked: boolean): Promise<void> {
  statusSavingId.value = record.id
  try {
    await changeConfigStatus(record.id, {
      status: checked ? STATUS_ENABLED : STATUS_DISABLED,
      version: record.version,
    })
    await reload()
  } finally {
    statusSavingId.value = null
  }
}

/** 删除确认（内置参数不可删：按钮已禁用，此处再由后端 18033 兜底拦截） */
function confirmDelete(record: ConfigVO): void {
  Modal.confirm({
    title: '删除系统参数',
    content: `确定删除系统参数「${record.configName}（${record.configKey}）」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteConfig(record.id)
      message.success('删除成功')
      await reload()
    },
  })
}
</script>

<template>
  <div class="config-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="参数键">
          <a-input v-model:value="draftQuery.configKey" placeholder="请输入参数键" allow-clear />
        </a-form-item>
        <a-form-item label="参数名称">
          <a-input v-model:value="draftQuery.configName" placeholder="请输入参数名称" allow-clear />
        </a-form-item>
        <a-form-item label="分组">
          <a-input v-model:value="draftQuery.configGroup" placeholder="请输入分组" allow-clear />
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
        <a-button v-perm="'system:config:create'" type="primary" @click="formModalRef?.show('create')">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </a-space>

      <a-table
        row-key="id"
        :columns="CONFIG_COLUMNS"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1300 }"
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
          <template v-if="column.dataIndex === 'valueType'">
            {{ VALUE_TYPE_MAP[(record as ConfigVO).valueType] ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'isSystem'">
            <a-tag v-if="(record as ConfigVO).isSystem === IS_SYSTEM_YES" color="red">内置</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tooltip :title="(record as ConfigVO).isSystem === IS_SYSTEM_YES ? '内置参数不可停用' : ''">
              <a-switch
                :checked="(record as ConfigVO).status === STATUS_ENABLED"
                :disabled="(record as ConfigVO).isSystem === IS_SYSTEM_YES"
                :loading="statusSavingId === (record as ConfigVO).id"
                checked-children="启用"
                un-checked-children="停用"
                @change="(checked: boolean) => handleStatusChange(record as ConfigVO, checked)"
              />
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            {{ (record as ConfigVO).remark ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-perm="'system:config:update'"
                type="link"
                size="small"
                @click="formModalRef?.show('edit', { ...(record as ConfigVO) })"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-tooltip :title="(record as ConfigVO).isSystem === IS_SYSTEM_YES ? '内置参数不可删除' : ''">
                <a-button
                  v-perm="'system:config:delete'"
                  type="link"
                  size="small"
                  danger
                  :disabled="(record as ConfigVO).isSystem === IS_SYSTEM_YES"
                  @click="confirmDelete(record as ConfigVO)"
                >
                  <DeleteOutlined /> 删除
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <ConfigFormModal ref="formModalRef" @saved="reload" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
