<script setup lang="ts">
// 登录日志查询页（F7，只读）：AppSearchForm + useTable 两件套，仅分页查询无增删改。
// 事件时间为日期粒度（后端 yyyy-MM-dd 解析为当天起止、左闭右开），开始/结束两个独立日期选择。
import { computed, onMounted, watch } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import AppPageContainer from '../../../components/AppPageContainer/index.vue'
import { useTable } from '../../../composables/useTable'
import { getLoginLogPage } from '../../../api/log'
import type { PageQuery, PageResult, LoginLogQueryDTO, LoginLogVO } from '../../../types/api'
import { LOGIN_LOG_COLUMNS, LOGIN_TYPE_FAILED, LOGIN_TYPE_MAP, LOGIN_TYPE_OPTIONS } from './config'
import { RESULT_FAILED, RESULT_STATUS_MAP, RESULT_STATUS_OPTIONS } from '../../../constants'

/** 表单查询条件（startTime/endTime 为日期草稿，直接映射查询 DTO；type alias 便于赋给 Record<string, unknown>） */
type LoginLogDraftQuery = {
  username: string
  loginType: number | undefined
  resultStatus: number | undefined
  startTime: string | undefined
  endTime: string | undefined
}

/** 提交查询：转发登录日志分页接口 */
function fetchLoginLogPage(query: LoginLogDraftQuery & PageQuery): Promise<PageResult<LoginLogVO>> {
  const { startTime, endTime, ...condition } = query
  const payload: LoginLogQueryDTO = { ...condition, startTime, endTime }
  return getLoginLogPage(payload)
}

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, handleTableChange } =
  useTable<LoginLogDraftQuery, LoginLogVO>(fetchLoginLogPage, {
    username: '',
    loginType: undefined,
    resultStatus: undefined,
    startTime: undefined,
    endTime: undefined,
  })

/** 结束日期不能早于已选的开始日期 */
const isEndDisabled = computed(
  (): ((current: Dayjs) => boolean) =>
    (current) =>
      draftQuery.value.startTime
        ? current.isBefore(dayjs(draftQuery.value.startTime!, 'YYYY-MM-DD'), 'day')
        : false,
)

/** 开始日期改到结束日期之后时，自动清空结束日期，避免出现 开始 > 结束 的非法区间 */
watch(
  () => draftQuery.value.startTime,
  (start) => {
    if (start && draftQuery.value.endTime && dayjs(draftQuery.value.endTime).isBefore(dayjs(start))) {
      draftQuery.value.endTime = undefined
    }
  },
)

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})
</script>

<template>
  <div class="login-log-page">
    <AppPageContainer>
      <template #search>
        <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
          <a-form-item label="用户名">
            <a-input v-model:value="draftQuery.username" placeholder="请输入用户名" allow-clear />
          </a-form-item>
          <a-form-item label="事件类型">
            <a-select
              v-model:value="draftQuery.loginType"
              :options="[...LOGIN_TYPE_OPTIONS]"
              placeholder="请选择事件类型"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="结果">
            <a-select
              v-model:value="draftQuery.resultStatus"
              :options="[...RESULT_STATUS_OPTIONS]"
              placeholder="请选择结果"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="开始日期">
            <a-date-picker
              v-model:value="draftQuery.startTime"
              value-format="YYYY-MM-DD"
              placeholder="开始日期"
              allow-clear
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="结束日期">
            <a-date-picker
              v-model:value="draftQuery.endTime"
              value-format="YYYY-MM-DD"
              placeholder="结束日期"
              allow-clear
              :disabled-date="isEndDisabled"
              style="width: 100%"
            />
          </a-form-item>
        </AppSearchForm>
      </template>

      <a-table
        row-key="id"
        :columns="LOGIN_LOG_COLUMNS"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1180 }"
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
          <template v-if="column.dataIndex === 'loginType'">
            <a-tag v-if="(record as LoginLogVO).loginType === LOGIN_TYPE_FAILED" color="red">
              {{ LOGIN_TYPE_MAP[(record as LoginLogVO).loginType] ?? '-' }}
            </a-tag>
            <a-tag v-else color="blue">
              {{ LOGIN_TYPE_MAP[(record as LoginLogVO).loginType] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'resultStatus'">
            <a-tag :color="(record as LoginLogVO).resultStatus === RESULT_FAILED ? 'red' : 'green'">
              {{ RESULT_STATUS_MAP[(record as LoginLogVO).resultStatus] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'loginIp'">
            {{ (record as LoginLogVO).loginIp ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'userAgent'">
            {{ (record as LoginLogVO).userAgent ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'failReason'">
            {{ (record as LoginLogVO).failReason ?? '-' }}
          </template>
        </template>
      </a-table>
    </AppPageContainer>
  </div>
</template>
