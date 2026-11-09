<script setup lang="ts">
// 单据编号序列页（F8）：AppSearchForm + useTable 范式分页查询（system:sequence:list）。
// 行内"重置"按钮（system:sequence:reset）打开重置弹窗，仅允许向下重置至未使用区间，
// 区间冲突（18035/18036）与乐观锁冲突（10601）由后端拦截，前端只做 reason 必填校验。
import { onMounted, ref } from 'vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { useTable } from '../../../composables/useTable'
import { getCodeSequencePage } from '../../../api/codeSequence'
import type { CodeSequenceQueryDTO, CodeSequenceVO } from '../../../types/api'
import { CODE_SEQUENCE_COLUMNS } from './config'
import ResetModal from './components/ResetModal.vue'

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange } =
  useTable<Omit<CodeSequenceQueryDTO, 'pageNum' | 'pageSize'>, CodeSequenceVO>(getCodeSequencePage, {
    bizType: '',
    period: '',
  })

const resetModalRef = ref<InstanceType<typeof ResetModal> | null>(null)

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})
</script>

<template>
  <div class="code-sequence-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="单据类型">
          <a-input v-model:value="draftQuery.bizType" placeholder="请输入单据类型" allow-clear />
        </a-form-item>
        <a-form-item label="期间">
          <a-input v-model:value="draftQuery.period" placeholder="yyyyMM，如 202601" allow-clear />
        </a-form-item>
      </AppSearchForm>

      <a-table
        row-key="id"
        :columns="CODE_SEQUENCE_COLUMNS"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1100 }"
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
          <template v-if="column.dataIndex === 'nextCodePreview'">
            <a-typography-text code>
              {{ (record as CodeSequenceVO).nextCodePreview }}
            </a-typography-text>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              v-perm="'system:sequence:reset'"
              type="link"
              size="small"
              @click="resetModalRef?.show(record as CodeSequenceVO)"
            >
              重置
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <ResetModal ref="resetModalRef" @saved="reload" />
  </div>
</template>
