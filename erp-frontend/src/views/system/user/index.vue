<script setup lang="ts">
// 用户管理列表页（F1 页面范式基准）：AppSearchForm + useTable + AppCrudModal 三件套骨架。
// 差异仅在列定义与表单字段；扩展交互：状态开关、重置密码、分配角色、导出。
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { ColumnsType } from 'ant-design-vue/es/table'
import {
  DeleteOutlined,
  EditOutlined,
  ExportOutlined,
  KeyOutlined,
  PlusOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { useTable } from '../../../composables/useTable'
import { getDeptTree } from '../../../api/dept'
import { useAuthStore } from '../../../stores/auth'
import {
  changeUserStatus,
  deleteUser,
  exportUserCsv,
  getUserDetail,
  getUserPage,
} from '../../../api/user'
import { STATUS_DISABLED, STATUS_ENABLED, STATUS_OPTIONS } from '../../../constants'
import type { DeptTreeVO, UserDetailVO, UserQueryDTO, UserVO } from '../../../types/api'
import UserFormModal from './components/UserFormModal.vue'
import ResetPasswordModal from './components/ResetPasswordModal.vue'
import AssignRolesModal from './components/AssignRolesModal.vue'

const auth = useAuthStore()

/** 列定义（列名与后端 UserVO 字段一一对应） */
const columns: ColumnsType<UserVO> = [
  { title: '用户编码', dataIndex: 'userCode', width: 120 },
  { title: '用户名', dataIndex: 'username', width: 120 },
  { title: '真实姓名', dataIndex: 'realName', width: 120 },
  { title: '所属部门', dataIndex: 'deptName', width: 140 },
  { title: '角色', dataIndex: 'roles', width: 180 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '最后登录时间', dataIndex: 'lastLoginTime', width: 170 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 370, fixed: 'right' },
]

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange } =
  useTable<Omit<UserQueryDTO, 'pageNum' | 'pageSize'>, UserVO>(getUserPage, {
    username: '',
    realName: '',
    deptId: undefined,
    status: undefined,
  })

/** 部门过滤下拉（F4 回接）：children 为 null 时收敛为 undefined，避免空子级渲染 */
const deptTree = ref<DeptTreeVO[]>([])
const deptTreeData = computed(() => toTreeNodes(deptTree.value))
function toTreeNodes(nodes: DeptTreeVO[]): DeptTreeVO[] {
  return nodes.map((node) => ({
    ...node,
    children: node.children?.length ? toTreeNodes(node.children) : undefined,
  })) as DeptTreeVO[]
}

const formModalRef = ref<InstanceType<typeof UserFormModal> | null>(null)
const resetModalRef = ref<InstanceType<typeof ResetPasswordModal> | null>(null)
const assignModalRef = ref<InstanceType<typeof AssignRolesModal> | null>(null)

const exporting = ref(false)
const statusSavingId = ref<string | null>(null)

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求）；同时拉部门树供过滤下拉 */
onMounted(async () => {
  void search()
  deptTree.value = await getDeptTree()
})

/** 拉详情 → 打开编辑弹窗（回填 version/roleIds 等列表页没有的字段） */
async function openEdit(record: UserVO): Promise<void> {
  const detail = await getUserDetail(record.id)
  formModalRef.value?.show('edit', { ...detail })
}

async function openAssign(record: UserVO): Promise<void> {
  const detail: UserDetailVO = await getUserDetail(record.id)
  assignModalRef.value?.show(detail)
}

/** 状态开关：携带行内 version 提交乐观锁，成功后整表刷新 */
async function handleStatusChange(record: UserVO, checked: boolean): Promise<void> {
  statusSavingId.value = record.id
  try {
    await changeUserStatus(record.id, {
      status: checked ? STATUS_ENABLED : STATUS_DISABLED,
      version: record.version,
    })
    await reload()
  } finally {
    statusSavingId.value = null
  }
}

/** 删除确认（危险操作统一 Modal.confirm + danger 按钮） */
function confirmDelete(record: UserVO): void {
  Modal.confirm({
    title: '删除用户',
    content: `确定删除用户「${record.realName}（${record.username}）」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteUser(record.id)
      message.success('删除成功')
      await reload()
    },
  })
}

/** 导出 CSV（文件流下载，文件名带时间戳） */
async function handleExport(): Promise<void> {
  exporting.value = true
  try {
    const blob = await exportUserCsv(draftQuery.value)
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `用户列表_${new Date().toISOString().replaceAll(/[-:T]/g, '').slice(0, 14)}.csv`
    anchor.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="user-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="用户名">
          <a-input v-model:value="draftQuery.username" placeholder="请输入用户名" allow-clear />
        </a-form-item>
        <a-form-item label="真实姓名">
          <a-input v-model:value="draftQuery.realName" placeholder="请输入真实姓名" allow-clear />
        </a-form-item>
        <a-form-item label="所属部门">
          <a-tree-select
            v-model:value="draftQuery.deptId"
            :tree-data="deptTreeData"
            :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择所属部门"
            allow-clear
            show-search
            tree-node-filter-prop="deptName"
            tree-default-expand-all
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
        <a-button v-perm="'system:user:create'" type="primary" @click="formModalRef?.show('create')">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
        <a-button v-perm="'system:user:export'" :loading="exporting" @click="handleExport">
          <template #icon><ExportOutlined /></template>
          导出
        </a-button>
      </a-space>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :scroll="{ x: 1330 }"
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
          <template v-if="(record as UserVO).deptName === undefined" />
          <template v-if="column.dataIndex === 'roles'">
            <a-tag v-for="role in (record as UserVO).roles" :key="role" color="blue">{{ role }}</a-tag>
            <span v-if="!(record as UserVO).roles?.length">-</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-switch
              :checked="(record as UserVO).status === STATUS_ENABLED"
              :loading="statusSavingId === (record as UserVO).id"
              :disabled="!auth.hasPerm('system:user:update')"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: boolean) => handleStatusChange(record as UserVO, checked)"
            />
          </template>
          <template v-else-if="column.dataIndex === 'lastLoginTime'">
            {{ (record as UserVO).lastLoginTime ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-perm="'system:user:update'"
                type="link"
                size="small"
                @click="openEdit(record as UserVO)"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-button
                v-perm="'system:user:resetpwd'"
                type="link"
                size="small"
                @click="resetModalRef?.show(record as UserVO)"
              >
                <KeyOutlined /> 重置密码
              </a-button>
              <a-button
                v-perm="'system:user:assignrole'"
                type="link"
                size="small"
                @click="openAssign(record as UserVO)"
              >
                <TeamOutlined /> 分配角色
              </a-button>
              <a-button
                v-perm="'system:user:delete'"
                type="link"
                size="small"
                danger
                @click="confirmDelete(record as UserVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <UserFormModal ref="formModalRef" @saved="reload" />
    <ResetPasswordModal ref="resetModalRef" />
    <AssignRolesModal ref="assignModalRef" @saved="reload" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
