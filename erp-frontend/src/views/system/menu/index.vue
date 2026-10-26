<script setup lang="ts">
// 菜单管理列表页（F3）：树形表格变体，AppSearchForm + a-table 树 + AppCrudModal。
// 树查询接口无分页，故不使用 useTable；父级防环/层级/子节点/角色引用校验以后端为准。
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
import { deleteMenu, getMenuTree } from '../../../api/menu'
import {
  MENU_TYPE_BUTTON,
  MENU_TYPE_DIR,
  MENU_TYPE_MAP,
  MENU_TYPE_MENU,
  STATUS_MAP,
  STATUS_OPTIONS,
  VISIBLE_MAP,
} from '../../../constants'
import type { MenuNodeVO, MenuTreeQueryDTO } from '../../../types/api'
import MenuFormModal from './components/MenuFormModal.vue'

/** 菜单类型标签配色（仅预设 color 名，不使用魔法色值） */
const MENU_TYPE_COLOR: Record<number, string> = {
  [MENU_TYPE_DIR]: 'blue',
  [MENU_TYPE_MENU]: 'green',
  [MENU_TYPE_BUTTON]: 'orange',
}

/** 列定义（列名与后端 MenuNodeVO 字段一一对应） */
const columns: ColumnsType<MenuNodeVO> = [
  { title: '菜单名称', dataIndex: 'menuName', width: 220 },
  { title: '类型', dataIndex: 'menuType', width: 90 },
  { title: '路由地址', dataIndex: 'path', width: 160 },
  { title: '组件路径', dataIndex: 'component', width: 180 },
  { title: '权限标识', dataIndex: 'perms', width: 180 },
  { title: '图标', dataIndex: 'icon', width: 120 },
  { title: '排序', dataIndex: 'menuSort', width: 80 },
  { title: '可见', dataIndex: 'visible', width: 80 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' },
]

const initialQuery: MenuTreeQueryDTO = { menuName: '', perms: '', status: undefined }

const query = ref<MenuTreeQueryDTO>({ ...initialQuery })
const draftQuery = ref<MenuTreeQueryDTO>({ ...initialQuery })
const treeData = ref<MenuNodeVO[]>([])
const expandedRowKeys = ref<string[]>([])
const loading = ref(false)
const formModalRef = ref<InstanceType<typeof MenuFormModal> | null>(null)

/** 递归收集树中全部节点 ID（用于加载/过滤后全展开） */
function collectIds(nodes: MenuNodeVO[]): string[] {
  const ids: string[] = []
  for (const node of nodes) {
    ids.push(node.id)
    if (node.children?.length) {
      ids.push(...collectIds(node.children))
    }
  }
  return ids
}

function matchesQuery(node: MenuNodeVO): boolean {
  const menuName = query.value.menuName?.trim()
  const perms = query.value.perms?.trim()
  if (menuName && !node.menuName.includes(menuName)) {
    return false
  }
  if (perms && !node.perms?.includes(perms)) {
    return false
  }
  if (query.value.status !== undefined && node.status !== query.value.status) {
    return false
  }
  return true
}

/**
 * 客户端过滤树并保留祖先链：后端树查询会先按条件过滤平铺记录再组树，
 * 命中节点若父级未命中会成为孤儿被丢弃，故本页拉全量树后本地过滤。
 */
function filterTree(nodes: MenuNodeVO[]): MenuNodeVO[] {
  const result: MenuNodeVO[] = []
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
    treeData.value = await getMenuTree()
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

function openCreate(parent?: MenuNodeVO): void {
  formModalRef.value?.show('create', undefined, parent)
}

function openEdit(record: MenuNodeVO): void {
  formModalRef.value?.show('edit', { ...record })
}

/** 删除确认（有子节点时按钮禁用；角色引用由后端 18021 拦截） */
function confirmDelete(record: MenuNodeVO): void {
  Modal.confirm({
    title: '删除菜单',
    content: `确定删除「${record.menuName}」吗？删除后不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteMenu(record.id)
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
  <div class="menu-page">
    <a-card>
      <AppSearchForm :model="draftQuery" :loading="loading" @search="search" @reset="reset">
        <a-form-item label="菜单名称">
          <a-input v-model:value="draftQuery.menuName" placeholder="请输入菜单名称" allow-clear />
        </a-form-item>
        <a-form-item label="权限标识">
          <a-input v-model:value="draftQuery.perms" placeholder="请输入权限标识" allow-clear />
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
        <a-button v-perm="'system:menu:create'" type="primary" @click="openCreate()">
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
        :scroll="{ x: 1450 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'menuType'">
            <a-tag :color="MENU_TYPE_COLOR[(record as MenuNodeVO).menuType]">
              {{ MENU_TYPE_MAP[(record as MenuNodeVO).menuType] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'path'">
            {{ (record as MenuNodeVO).path ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'component'">
            {{ (record as MenuNodeVO).component ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'perms'">
            {{ (record as MenuNodeVO).perms ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'icon'">
            {{ (record as MenuNodeVO).icon ?? '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'visible'">
            <a-tag :color="(record as MenuNodeVO).visible === 1 ? 'cyan' : 'default'">
              {{ VISIBLE_MAP[(record as MenuNodeVO).visible] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="(record as MenuNodeVO).status === 1 ? 'success' : 'default'">
              {{ STATUS_MAP[(record as MenuNodeVO).status] ?? '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="(record as MenuNodeVO).menuType !== MENU_TYPE_BUTTON"
                v-perm="'system:menu:create'"
                type="link"
                size="small"
                @click="openCreate(record as MenuNodeVO)"
              >
                <PlusSquareOutlined /> 新增下级
              </a-button>
              <a-button
                v-perm="'system:menu:update'"
                type="link"
                size="small"
                @click="openEdit(record as MenuNodeVO)"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-button
                v-perm="'system:menu:delete'"
                type="link"
                size="small"
                danger
                :disabled="Boolean((record as MenuNodeVO).children?.length)"
                @click="confirmDelete(record as MenuNodeVO)"
              >
                <DeleteOutlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <MenuFormModal ref="formModalRef" :menus="treeData" @saved="load" />
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
