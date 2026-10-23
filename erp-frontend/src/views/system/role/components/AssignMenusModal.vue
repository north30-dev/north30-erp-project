<script setup lang="ts">
// 分配菜单弹窗（页面私有组件）：a-tree 勾选（父子联动、半选呈现），全量覆盖提交（PUT /{id}/menus）。
// 提交 menuIds = 勾选 + 半选（后端全量覆盖语义，半选父节点必须一并提交，否则父菜单丢失）。
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Key } from 'ant-design-vue/es/table/interface'
import { assignRoleMenus, getRoleMenuIds } from '../../../../api/role'
import { getMenuTree } from '../../../../api/menu'
import type { MenuTreeVO, RoleVO } from '../../../../types/api'

const emit = defineEmits<{ saved: [] }>()

const open = ref(false)
const submitting = ref(false)
const treeLoading = ref(false)
const detail = ref<RoleVO | null>(null)
const treeData = ref<MenuTreeVO[]>([])
const checkedKeys = ref<Key[]>([])
const halfCheckedKeys = ref<Key[]>([])

/** MenuTreeVO 字段映射（menuId 作 key、menuName 作标题） */
const fieldNames = { title: 'menuName', key: 'menuId', children: 'children' }

/** 收集树中所有父节点 ID（有 children 的节点） */
function collectParentIds(nodes: MenuTreeVO[], acc: Set<string>): Set<string> {
  for (const node of nodes) {
    if (node.children?.length) {
      acc.add(node.menuId)
      collectParentIds(node.children, acc)
    }
  }
  return acc
}

async function show(record: RoleVO): Promise<void> {
  detail.value = record
  open.value = true
  treeLoading.value = false
  checkedKeys.value = []
  halfCheckedKeys.value = []
  try {
    // 树数据与已勾选集合并行加载
    const [tree, assigned] = await Promise.all([getMenuTree(), getRoleMenuIds(record.id)])
    treeData.value = tree
    // 回显只回填叶子 ID：antd 父子联动下，全选父自动呈选中、部分选父自动呈半选；
    // 若回填半选父节点会被级联成全选，污染回显
    const parentIds = collectParentIds(tree, new Set())
    checkedKeys.value = assigned.menuIds.filter((id) => !parentIds.has(id))
  } finally {
    treeLoading.value = false
  }
}

/** 勾选变化：记录全选集合与半选集合，提交时合并（后端全量覆盖语义） */
function handleCheck(keys: Key[], info: { halfCheckedKeys: Key[] }): void {
  checkedKeys.value = keys
  halfCheckedKeys.value = info.halfCheckedKeys
}

async function handleOk(): Promise<void> {
  submitting.value = true
  try {
    const menuIds = [...checkedKeys.value, ...halfCheckedKeys.value].map(String)
    const result = await assignRoleMenus(detail.value!.id, { menuIds })
    message.success(`分配菜单成功，已授权 ${result.permCount} 个权限点`)
    open.value = false
    emit('saved')
  } catch {
    // 失败提示由 request 拦截器统一弹出，保持弹窗打开
  } finally {
    submitting.value = false
  }
}

defineExpose({ show })
</script>

<template>
  <a-modal
    v-model:open="open"
    :title="`分配菜单 - ${detail?.roleName ?? ''}`"
    :confirm-loading="submitting"
    :mask-closable="false"
    destroy-on-close
    @ok="handleOk"
  >
    <a-spin :spinning="treeLoading">
      <a-tree
        v-if="treeData.length"
        checkable
        :tree-data="treeData"
        :field-names="fieldNames"
        :checked-keys="checkedKeys"
        default-expand-all
        @check="handleCheck"
      />
      <a-empty v-else-if="!treeLoading" description="暂无菜单数据" />
    </a-spin>
  </a-modal>
</template>
