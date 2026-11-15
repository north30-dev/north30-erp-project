<script setup lang="ts">
// 审计日志查询页（F7，只读）：AppSearchForm + useTable 两件套，仅分页查询无增删改。
// 变更前后 JSON 为详情接口专属大字段，列表不展示；行内"详情"按钮（system:auditlog:detail）
// 打开抽屉调详情接口，变更 JSON 解析失败（白名单超长截断）时原样展示。
import { onMounted, ref } from 'vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import AppPageContainer from '../../../components/AppPageContainer/index.vue'
import { useTable } from '../../../composables/useTable'
import { getAuditLogDetail, getAuditLogPage } from '../../../api/log'
import type { PageQuery, PageResult, AuditLogQueryDTO, AuditLogVO } from '../../../types/api'
import { AUDIT_LOG_COLUMNS } from './config'
import {
  AUDIT_MODULE_MAP,
  AUDIT_MODULE_OPTIONS,
  OPERATE_TYPE_MAP,
  OPERATE_TYPE_OPTIONS,
  RESULT_FAILED,
  RESULT_STATUS_MAP,
  RESULT_STATUS_OPTIONS,
} from '../../../constants'

/** 表单查询条件（timeRange 为日期区间草稿，提交时拆为 startTime/endTime；type alias 便于赋给 Record<string, unknown>） */
type AuditLogDraftQuery = {
  bizCode: string
  module: string | undefined
  bizType: string
  operateType: string | undefined
  operateBy: string
  resultStatus: number | undefined
  timeRange: [string, string] | undefined
}

/** 提交查询：拆时间区间并转发审计日志分页接口 */
function fetchAuditLogPage(query: AuditLogDraftQuery & PageQuery): Promise<PageResult<AuditLogVO>> {
  const { timeRange, ...condition } = query
  const payload: AuditLogQueryDTO = {
    ...condition,
    startTime: timeRange?.[0],
    endTime: timeRange?.[1],
  }
  return getAuditLogPage(payload)
}

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, handleTableChange } =
  useTable<AuditLogDraftQuery, AuditLogVO>(fetchAuditLogPage, {
    bizCode: '',
    module: undefined,
    bizType: '',
    operateType: undefined,
    operateBy: '',
    resultStatus: undefined,
    timeRange: undefined,
  })

/** 详情抽屉状态：打开时按行 id 调详情接口（列表 VO 不含变更 JSON，须重新拉取） */
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AuditLogVO | null>(null)

async function showDetail(record: AuditLogVO): Promise<void> {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getAuditLogDetail(record.id)
  } finally {
    detailLoading.value = false
  }
}

/** 变更 JSON 美化展示：解析失败（超长截断）时原样返回 */
function formatJson(text: string | null): string {
  if (!text) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(text) as unknown, null, 2)
  } catch {
    return text
  }
}

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})
</script>

<template>
  <div class="audit-log-page">
    <AppPageContainer>
      <template #search>
        <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
          <a-form-item label="单据号">
            <a-input v-model:value="draftQuery.bizCode" placeholder="请输入单据号" allow-clear />
          </a-form-item>
          <a-form-item label="业务模块">
            <a-select
              v-model:value="draftQuery.module"
              :options="[...AUDIT_MODULE_OPTIONS]"
              placeholder="请选择业务模块"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="业务类型">
            <a-input v-model:value="draftQuery.bizType" placeholder="如 SALES_ORDER" allow-clear />
          </a-form-item>
          <a-form-item label="操作类型">
            <a-select
              v-model:value="draftQuery.operateType"
              :options="[...OPERATE_TYPE_OPTIONS]"
              placeholder="请选择操作类型"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="操作人">
            <a-input v-model:value="draftQuery.operateBy" placeholder="请输入操作人" allow-clear />
          </a-form-item>
          <a-form-item label="结果">
            <a-select
              v-model:value="draftQuery.resultStatus"
              :options="[...RESULT_STATUS_OPTIONS]"
              placeholder="请选择结果"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="操作时间">
            <a-range-picker v-model:value="draftQuery.timeRange" value-format="YYYY-MM-DD" />
          </a-form-item>
        </AppSearchForm>
      </template>

      <a-table
        row-key="id"
        :columns="AUDIT_LOG_COLUMNS"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1280 }"
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
          <template v-if="column.dataIndex === 'module'">
            {{ AUDIT_MODULE_MAP[(record as AuditLogVO).module] ?? (record as AuditLogVO).module }}
          </template>
          <template v-else-if="column.dataIndex === 'bizType'">
            {{ (record as AuditLogVO).bizType ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'bizCode'">
            {{ (record as AuditLogVO).bizCode ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'operateDesc'">
            {{ (record as AuditLogVO).operateDesc ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'operateType'">
            {{ OPERATE_TYPE_MAP[(record as AuditLogVO).operateType] ?? (record as AuditLogVO).operateType }}
          </template>
          <template v-else-if="column.dataIndex === 'resultStatus'">
            <a-tag :color="(record as AuditLogVO).resultStatus === RESULT_FAILED ? 'red' : 'green'">
              {{ RESULT_STATUS_MAP[(record as AuditLogVO).resultStatus] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'costTime'">
            {{ (record as AuditLogVO).costTime ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-perm="'system:auditlog:detail'"
              type="link"
              size="small"
              @click="showDetail(record as AuditLogVO)"
            >
              详情
            </a-button>
          </template>
        </template>
      </a-table>
    </AppPageContainer>

    <a-drawer v-model:open="drawerVisible" title="审计日志详情" width="720" destroy-on-close>
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <div class="detail-section">
            <div class="section-title">操作信息</div>
            <dl class="detail-kv">
              <div class="detail-kv__row">
                <dt>业务模块</dt>
                <dd>{{ AUDIT_MODULE_MAP[detail.module] ?? detail.module }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>业务类型</dt>
                <dd>{{ detail.bizType ?? '-' }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>单据号</dt>
                <dd>{{ detail.bizCode ?? '-' }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>操作类型</dt>
                <dd>{{ OPERATE_TYPE_MAP[detail.operateType] ?? detail.operateType }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>操作描述</dt>
                <dd>{{ detail.operateDesc ?? '-' }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>操作人</dt>
                <dd>{{ detail.operateBy }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>操作时间</dt>
                <dd>{{ detail.operateTime }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>结果</dt>
                <dd>
                  <a-tag :color="detail.resultStatus === RESULT_FAILED ? 'red' : 'green'">
                    {{ RESULT_STATUS_MAP[detail.resultStatus] ?? '-' }}
                  </a-tag>
                </dd>
              </div>
              <div class="detail-kv__row">
                <dt>耗时(ms)</dt>
                <dd>{{ detail.costTime ?? '-' }}</dd>
              </div>
              <div v-if="detail.resultStatus === RESULT_FAILED" class="detail-kv__row">
                <dt>错误码</dt>
                <dd>{{ detail.errorCode ?? '-' }}</dd>
              </div>
              <div v-if="detail.resultStatus === RESULT_FAILED" class="detail-kv__row">
                <dt>失败原因</dt>
                <dd>{{ detail.errorMessage ?? '-' }}</dd>
              </div>
            </dl>
          </div>

          <div class="detail-section">
            <div class="section-title">请求信息</div>
            <dl class="detail-kv">
              <div class="detail-kv__row">
                <dt>请求方法</dt>
                <dd>{{ detail.requestMethod ?? '-' }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>操作 IP</dt>
                <dd>{{ detail.operateIp ?? '-' }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>请求地址</dt>
                <dd>{{ detail.requestUri ?? '-' }}</dd>
              </div>
              <div class="detail-kv__row">
                <dt>链路追踪</dt>
                <dd>{{ detail.traceId ?? '-' }}</dd>
              </div>
            </dl>
          </div>

          <div class="detail-section">
            <div class="section-title">变更字段</div>
            <a-space v-if="detail.diffFields && detail.diffFields.length > 0" wrap>
              <a-tag v-for="field in detail.diffFields" :key="field" color="orange">{{ field }}</a-tag>
            </a-space>
            <span v-else>无</span>
          </div>

          <div class="detail-section">
            <div class="section-title">变更前数据</div>
            <pre class="json-block">{{ formatJson(detail.beforeJson) || '无' }}</pre>
          </div>

          <div class="detail-section">
            <div class="section-title">变更后数据</div>
            <pre class="json-block">{{ formatJson(detail.afterJson) || '无' }}</pre>
          </div>
        </template>
        <a-empty v-else-if="!detailLoading" description="未获取到日志详情" />
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.detail-section {
  margin-top: 24px;
}

.section-title {
  margin-bottom: 8px;
  color: var(--erp-text);
  font-size: 14px;
  font-weight: 600;
}

.detail-kv {
  margin: 0;
}

.detail-kv__row {
  display: flex;
  padding: 6px 0;
  font-size: 14px;
  line-height: 22px;
  border-bottom: 1px solid var(--erp-border-line);
}

.detail-kv__row:last-child {
  border-bottom: none;
}

.detail-kv dt {
  flex: none;
  width: 110px;
  color: var(--erp-text-tertiary);
}

.detail-kv dd {
  flex: 1;
  min-width: 0;
  margin: 0;
  color: var(--erp-text);
  word-break: break-all;
}

.json-block {
  max-height: 320px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  background-color: var(--erp-bg-layout);
  border-radius: 4px;
}
</style>
