<script setup lang="ts">
// 部门管理列表页（F4）：树形表格变体，AppSearchForm + a-table 树 + AppCrudModal。
// 树查询接口无分页，故不使用 useTable；防环/层级/下级与用户绑定校验以后端为准（10001/18025/18027）。
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { ColumnsType } from 'ant-design-vue/es/table'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  PlusSquareOutlined,
} from '@ant-design/icons-vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { deleteDept, getDeptTree } from '../../../api/dept'
import {
  DEPT_TYPE_BRANCH,
  DEPT_TYPE_DEPT,
  DEPT_TYPE_FACTORY,
  DEPT_TYPE_GROUP,
  DEPT_TYPE_MAP,
  DEPT_TYPE_OPTIONS,
  DEPT_TYPE_WORKSHOP,
  MAX_DEPT_DEPTH,
  STATUS_MAP,
  STATUS_OPTIONS,
} from '../../../constants'
import type { DeptTreeQueryDTO, DeptTreeVO } from '../../../types/api'
import DeptFormModal from './components/DeptFormModal.vue'

/** 组织类型标签配色（仅预设 color 名，不使用魔法色值） */
const DEPT_TYPE_COLOR: Record<number, string> = {
  [DEPT_TYPE_GROUP]: 'purple',
  [DEPT_TYPE_FACTORY]: 'geekblue',
  [DEPT_TYPE_BRANCH]: 'cyan',
  [DEPT_TYPE_DEPT]: 'blue',
  [DEPT_TYPE_WORKSHOP]: 'green',
}

/** 列定义（列名与后端 DeptTreeVO 字段一一对应） */
const columns: ColumnsType<DeptTreeVO> = [
  { title: '组织编码', dataIndex: 'deptCode', width: 140 },
  { title: '组织名称', dataIndex: 'deptName', width: 220 },
  { title: '类型', dataIndex: 'deptType', width: 110 },
  { title: '层级', dataIndex: 'deptLevel', width: 80 },
  { title: '负责人', dataIndex: 'leader', width: 120 },
  { title: '联系电话', dataIndex: 'phone', width: 140 },
  { title: '排序', dataIndex: 'deptSort', width: 80 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' },
]

const initialQuery: DeptTreeQueryDTO = { deptName: '', deptType: undefined, status: undefined }

const query = ref<DeptTreeQueryDTO>({ ...initialQuery })
const draftQuery = ref<DeptTreeQueryDTO>({ ...initialQuery })
const treeData = ref<DeptTreeVO[]>([])
const expandedRowKeys = ref<string[]>([])
const loading = ref(false)
const formModalRef = ref<InstanceType<typeof DeptFormModal> | null>(null)

/** 递归收集树中全部节点 ID（用于加载/过滤后全展开） */
function collectIds(nodes: DeptTreeVO[]): string[] {
  const ids: string[] = []
  for (const node of nodes) {
    ids.push(node.id)
    if (node.children?.length) {
      ids.push(...collectIds(node.children))
    }
  }
  return ids
}

function matchesQuery(node: DeptTreeVO): boolean {
  const deptName = query.value.deptName?.trim()
  if (deptName && !node.deptName.includes(deptName)) {
    return false
  }
  if (query.value.deptType !== undefined && node.deptType !== query.value.deptType) {
    return false
  }
  if (query.value.status !== undefined && node.status !== query.value.status) {
    return false
  }
  return true
}

/**
 * 客户端过滤树并保留祖先链：后端树查询会先按条件过滤平铺记录再组树，
 * 命中节点若父级未命中会成为孤儿被丢弃，故本页拉全量树后本地过滤（同菜单页策略）。
 */
function filterTree(nodes: DeptTreeVO[]): DeptTreeVO[] {
  const result: DeptTreeVO[] = []
  for (const node of nodes) {
    const children = node.children?.length ? filterTree(node.children) : []
    const selfMatched = matchesQuery(node)
    if (selfMatched || children.length) {
      result.push({
        ...node,
        // 自身命中时展示完整子树；仅子级命中时只保留命中的后代
        children: selfMatched ? node.children : children.length ? children : null,
      })
    }
  }
  return result
}

const displayTree = computed(() => filterTree(treeData.value))

async function load(): Promise<void> {
  loading.value = true
  try {
    treeData.value = await getDeptTree()
    expandedRowKeys.value = collectIds(displayTree.value)
  } finally {
    loading.value = false
  }
}

/** 查询：本地过滤后全展开命中路径 */
function search(): void {
  query.value = { ...draftQuery.value }
  expandedRowKeys.value = collectIds(displayTree.value)
}

function reset(): void {
  draftQuery.value = { ...initialQuery }
  query.value = { ...initialQuery }
  expandedRowKeys.value = collectIds(displayTree.value)
}

function openCreate(parent?: DeptTreeVO): void {
  formModalRef.value?.show('create', undefined, parent)
}

function openEdit(record: DeptTreeVO): void {
  formModalRef.value?.show('edit', { ...record })
}

/** 删除确认（有子节点时按钮禁用；挂靠用户由后端 18025 拦截） */
function confirmDelete(record: DeptTreeVO): void {
  Modal.confirm({
    title: '删除组织',
    content: `确定删除「${record.deptName}」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteDept(record.id)
      message.success('删除成功')
      await load()
    },
  })
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="dept-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="组织名称">
          <a-input v-model:value="draftQuery.deptName" placeholder="请输入组织名称" allow-clear />
        </a-form-item>
        <a-form-item label="组织类型">
          <a-select
            v-model:value="draftQuery.deptType"
            :options="[...DEPT_TYPE_OPTIONS]"
            placeholder="请选择组织类型"
            allow-clear
          />
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
        <a-button v-perm="'system:dept:create'" type="primary" @click="openCreate()">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </a-space>

      <a-table
        v-model:expanded-row-keys="expandedRowKeys"
        row-key="id"
        :columns="columns"
        :data-source="displayTree"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1270 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'deptType'">
            <a-tag :color="DEPT_TYPE_COLOR[(record as DeptTreeVO).deptType]">
              {{ DEPT_TYPE_MAP[(record as DeptTreeVO).deptType] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'leader'">
            {{ (record as DeptTreeVO).leader ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'phone'">
            {{ (record as DeptTreeVO).phone ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="(record as DeptTreeVO).status === 1 ? 'success' : 'default'">
              {{ STATUS_MAP[(record as DeptTreeVO).status] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-tooltip
                :title="
                  (record as DeptTreeVO).deptLevel < MAX_DEPT_DEPTH
                    ? ''
                    : `已达最大层级（${MAX_DEPT_DEPTH} 级），不可新增下级`
                "
              >
                <a-button
                  v-perm="'system:dept:create'"
                  type="link"
                  size="small"
                  :disabled="(record as DeptTreeVO).deptLevel >= MAX_DEPT_DEPTH"
                  @click="openCreate(record as DeptTreeVO)"
                >
                  <PlusSquareOutlined /> 新增下级
                </a-button>
              </a-tooltip>
              <a-button
                v-perm="'system:dept:update'"
                type="link"
                size="small"
                @click="openEdit(record as DeptTreeVO)"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-button
                v-perm="'system:dept:delete'"
                type="link"
                size="small"
                danger
                :disabled="Boolean((record as DeptTreeVO).children?.length)"
                @click="confirmDelete(record as DeptTreeVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <DeptFormModal ref="formModalRef" :depts="treeData" @saved="load" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
