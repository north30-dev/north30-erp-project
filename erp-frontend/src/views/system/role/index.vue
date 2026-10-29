<script setup lang="ts">
// 角色管理列表页（F2）：AppSearchForm + useTable + AppCrudModal 三件套骨架，写法对齐 F1 用户管理基准。
// 扩展交互：数据范围档位标签、内置角色标识、分配菜单（a-tree 勾选）；状态开关走 PUT update（乐观锁）。
import { onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { ColumnsType } from 'ant-design-vue/es/table'
import {
  ApartmentOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import AppSearchForm from '../../../components/AppSearchForm/index.vue'
import { useTable } from '../../../composables/useTable'
import { changeRoleStatus, deleteRole, getRolePage } from '../../../api/role'
import {
  DATA_SCOPE_MAP,
  STATUS_DISABLED,
  STATUS_ENABLED,
  STATUS_OPTIONS,
} from '../../../constants'
import type { RolePageQueryDTO, RoleVO } from '../../../types/api'
import RoleFormModal from './components/RoleFormModal.vue'
import AssignMenusModal from './components/AssignMenusModal.vue'

/** 列定义（列名与后端 RoleVO 字段一一对应） */
const columns: ColumnsType<RoleVO> = [
  { title: '角色编码', dataIndex: 'roleCode', width: 160 },
  { title: '角色名称', dataIndex: 'roleName', width: 160 },
  { title: '数据范围', dataIndex: 'dataScope', width: 130 },
  { title: '内置', dataIndex: 'isBuiltin', width: 80 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '用户数', dataIndex: 'userCount', width: 90 },
  { title: '备注', dataIndex: 'remark', width: 180 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

const { draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange } =
  useTable<Omit<RolePageQueryDTO, 'pageNum' | 'pageSize'>, RoleVO>(getRolePage, {
    roleCode: '',
    roleName: '',
    status: undefined,
  })

const formModalRef = ref<InstanceType<typeof RoleFormModal> | null>(null)
const assignModalRef = ref<InstanceType<typeof AssignMenusModal> | null>(null)

const statusSavingId = ref<string | null>(null)

/** 进入页面即加载列表（useTable 仅提供状态机，不自动请求） */
onMounted(() => {
  void search()
})

/** 状态开关：走专用 status 动作接口（08 文档阶段四状态开关统一），成功后整表刷新 */
async function handleStatusChange(record: RoleVO, checked: boolean): Promise<void> {
  statusSavingId.value = record.id
  try {
    await changeRoleStatus(record.id, {
      status: checked ? STATUS_ENABLED : STATUS_DISABLED,
      version: record.version,
    })
    await reload()
  } finally {
    statusSavingId.value = null
  }
}

/** 删除确认（危险操作统一 Modal.confirm + danger 按钮；内置角色后端 18017 拒绝，前端按钮禁用） */
function confirmDelete(record: RoleVO): void {
  Modal.confirm({
    title: '删除角色',
    content: `确定删除角色「${record.roleName}（${record.roleCode}）」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteRole(record.id)
      message.success('删除成功')
      await reload()
    },
  })
}
</script>

<template>
  <div class="role-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="角色编码">
          <a-input v-model:value="draftQuery.roleCode" placeholder="请输入角色编码" allow-clear />
        </a-form-item>
        <a-form-item label="角色名称">
          <a-input v-model:value="draftQuery.roleName" placeholder="请输入角色名称" allow-clear />
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
        <a-button v-perm="'system:role:create'" type="primary" @click="formModalRef?.show('create')">
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
          <template v-if="column.dataIndex === 'dataScope'">
            <a-tag color="blue">{{ DATA_SCOPE_MAP[(record as RoleVO).dataScope] ?? '-' }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'isBuiltin'">
            <a-tag v-if="(record as RoleVO).isBuiltin === 1" color="purple">内置</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-switch
              :checked="(record as RoleVO).status === STATUS_ENABLED"
              :loading="statusSavingId === (record as RoleVO).id"
              :disabled="(record as RoleVO).isBuiltin === 1"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: boolean) => handleStatusChange(record as RoleVO, checked)"
            />
          </template>
          <template v-else-if="column.dataIndex === 'remark'">
            {{ (record as RoleVO).remark ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-perm="'system:role:update'"
                type="link"
                size="small"
                @click="formModalRef?.show('edit', { ...(record as RoleVO) })"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-button
                v-perm="'system:role:assignmenu'"
                type="link"
                size="small"
                @click="assignModalRef?.show(record as RoleVO)"
              >
                <ApartmentOutlined /> 分配菜单
              </a-button>
              <a-button
                v-perm="'system:role:delete'"
                type="link"
                size="small"
                danger
                :disabled="(record as RoleVO).isBuiltin === 1"
                @click="confirmDelete(record as RoleVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <RoleFormModal ref="formModalRef" @saved="reload" />
    <AssignMenusModal ref="assignModalRef" @saved="reload" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
