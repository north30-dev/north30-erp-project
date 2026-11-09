<script setup lang="ts">
// 导入任务页（F8）：后端 D2 为占位实现（无 sys_import_task 表），任务列表恒返回空分页、结果查询恒报 10503。
// 页面按 ImportTaskController 契约开发：任务列表走 useTable 分页（仅 pageNum/pageSize 参数，无查询条件），
// 空数据正常渲染 a-table 空态；结果查询提供顶部任务号输入入口（列表无行时也可查询）与行内"结果"按钮，
// 10503 等失败提示由 request 拦截器统一弹出，抽屉内渲染空态。两个接口均挂 system:import:detail 权限点
// （任务列表接口文档未单列权限点，沿用 detail，与后端 @PreAuthorize 一致）。
import { onMounted, ref } from 'vue'
import { useTable } from '../../../composables/useTable'
import { getImportResult, getImportTaskPage } from '../../../api/importtask'
import type { ImportErrorVO, ImportResultVO, ImportTaskVO, PageQuery } from '../../../types/api'
import { IMPORT_ERROR_COLUMNS, IMPORT_TASK_COLUMNS } from './config'

/** 任务列表分页状态机（无查询条件，仅分页参数） */
const { pageNum, pageSize, records, total, loading, search, handleTableChange } =
  useTable<PageQuery, ImportTaskVO>(getImportTaskPage, {})

/** 结果查询抽屉状态（taskId 入口：列表恒空时仍可按任务号直接查询） */
const taskId = ref('')
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<ImportResultVO | null>(null)

/** 查询导入结果：空任务号不请求（后端 10001 兜底），失败（10503）渲染空态 */
async function showResult(nextTaskId: string): Promise<void> {
  const target = nextTaskId.trim()
  if (!target) {
    return
  }
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getImportResult(target)
  } catch {
    // 失败提示已由 request 拦截器统一弹出，抽屉内保持空态
  } finally {
    detailLoading.value = false
  }
}

/** 原始行数据 JSON 序列化展示（脱敏后回显，空值显示 -） */
function formatRawData(data: unknown): string {
  if (data == null) {
    return '-'
  }
  try {
    return JSON.stringify(data)
  } catch {
    return '-'
  }
}

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})
</script>

<template>
  <div class="import-task-page">
    <a-card>
      <a-alert
        class="placeholder-tip"
        type="info"
        show-icon
        message="导入执行能力由各业务模块陆续落地，当前仅提供任务列表与结果查询能力"
      />

      <a-space class="toolbar">
        <a-input
          v-model:value="taskId"
          placeholder="输入导入任务号查询结果"
          style="width: 280px"
          allow-clear
          @press-enter="showResult(taskId)"
        />
        <a-button
          v-perm="'system:import:detail'"
          type="primary"
          :disabled="!taskId.trim()"
          @click="showResult(taskId)"
        >
          查询结果
        </a-button>
      </a-space>

      <a-table
        row-key="taskId"
        :columns="IMPORT_TASK_COLUMNS"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1000 }"
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
          <template v-if="column.dataIndex === 'bizType'">
            {{ (record as ImportTaskVO).bizType ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'importBatch'">
            {{ (record as ImportTaskVO).importBatch ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'totalRows'">
            {{ (record as ImportTaskVO).totalRows ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'successRows'">
            {{ (record as ImportTaskVO).successRows ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'failRows'">
            {{ (record as ImportTaskVO).failRows ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'committed'">
            <a-tag v-if="(record as ImportTaskVO).committed === true" color="green">已提交</a-tag>
            <a-tag v-else-if="(record as ImportTaskVO).committed === false" color="orange">未提交</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-perm="'system:import:detail'"
              type="link"
              size="small"
              @click="showResult((record as ImportTaskVO).taskId)"
            >
              结果
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="drawerVisible" title="导入结果" width="720" destroy-on-close>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="任务 ID" :span="2">{{ detail.taskId }}</a-descriptions-item>
            <a-descriptions-item label="总行数">{{ detail.totalRows ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="成功行数">{{ detail.successRows ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="失败行数">{{ detail.failRows ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="提交状态">
              <a-tag v-if="detail.committed === true" color="green">已提交</a-tag>
              <a-tag v-else-if="detail.committed === false" color="orange">未提交</a-tag>
              <span v-else>-</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="section">
            <div class="section-title">错误行明细（{{ detail.errors?.length ?? 0 }}）</div>
            <a-table
              :columns="IMPORT_ERROR_COLUMNS"
              :data-source="detail.errors ?? []"
              :pagination="false"
              size="small"
              :row-key="(record: ImportErrorVO) => `${record.rowNo ?? 0}-${record.fieldName ?? ''}`"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'rowNo'">
                  {{ (record as ImportErrorVO).rowNo ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'fieldName'">
                  {{ (record as ImportErrorVO).fieldName ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'errorCode'">
                  {{ (record as ImportErrorVO).errorCode ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'errorMessage'">
                  {{ (record as ImportErrorVO).errorMessage ?? '-' }}
                </template>
                <template v-else-if="column.dataIndex === 'rawData'">
                  {{ formatRawData((record as ImportErrorVO).rawData) }}
                </template>
              </template>
            </a-table>
          </div>
        </template>
        <a-empty v-else-if="!detailLoading" description="未获取到导入结果（任务不存在或无数据）" />
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.placeholder-tip {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.section {
  margin-top: 24px;
}

.section-title {
  margin-bottom: 8px;
  font-weight: 600;
}
</style>
