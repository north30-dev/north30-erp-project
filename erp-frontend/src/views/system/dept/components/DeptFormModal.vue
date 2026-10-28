<script setup lang="ts">
// 部门新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，字段与后端 DeptCreateDTO/DeptUpdateDTO 对齐。
// 换父防环（排除自身及后代）与层级上限（5 级）为弱约束提示，最终以后端 10001/18027 校验为准。
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { DefaultOptionType } from 'ant-design-vue/es/vc-tree-select/TreeSelect'
import AppCrudModal from '../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudMode, CrudRules } from '../../../../components/AppCrudModal/type'
import { createDept, updateDept } from '../../../../api/dept'
import {
  DEPT_TYPE_DEPT,
  DEPT_TYPE_OPTIONS,
  MAX_DEPT_DEPTH,
  STATUS_ENABLED,
  STATUS_OPTIONS,
} from '../../../../constants'
import type { DeptTreeVO } from '../../../../types/api'

const props = defineProps<{
  /** 父级选择数据源（页面传入的完整部门树） */
  depts: DeptTreeVO[]
}>()

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<CrudMode>('create')
const editId = ref<string | null>(null)
/** create 模式的父级预设（「新增下级」时传入） */
const presetParentId = ref('0')

/** 收集自身及全部后代 id（edit 换父级防环：候选中排除） */
function collectSelfAndDescendants(nodes: DeptTreeVO[], rootId: string, acc: Set<string>): void {
  for (const node of nodes) {
    if (node.id === rootId) {
      collectAllIds(node, acc)
      return
    }
    if (node.children?.length) {
      collectSelfAndDescendants(node.children, rootId, acc)
    }
  }
}

function collectAllIds(node: DeptTreeVO, acc: Set<string>): void {
  acc.add(node.id)
  for (const child of node.children ?? []) {
    collectAllIds(child, acc)
  }
}

/**
 * 父级选择树：edit 模式排除自身子树（防环）、
 * 深度达上限的节点保留展示但禁用（弱约束，最终以后端 18027 兜底）。
 */
const parentTreeData = computed<DefaultOptionType[]>(() => {
  const forbidden = new Set<string>()
  if (mode.value === 'edit' && editId.value) {
    collectSelfAndDescendants(props.depts, editId.value, forbidden)
  }
  return [
    { label: '顶级组织', value: '0', children: toParentOptions(props.depts, forbidden) },
  ]
})

function toParentOptions(nodes: DeptTreeVO[], forbidden: Set<string>): DefaultOptionType[] {
  return nodes
    .filter((node) => !forbidden.has(node.id))
    .map((node) => {
      // 父深度 + 1 ≤ MAX_DEPT_DEPTH：深度已达上限的节点不可作父级
      const tooDeep = node.deptLevel >= MAX_DEPT_DEPTH
      return {
        label: tooDeep ? `${node.deptName}（已达最大层级）` : node.deptName,
        value: node.id,
        disabled: tooDeep,
        children: node.children?.length
          ? toParentOptions(node.children, forbidden)
          : undefined,
      }
    })
}

/** create 模式初始值工厂（AppCrudModal 每次打开时调用；默认值与后端一致） */
function initialForm(): CrudForm {
  return {
    deptCode: '',
    deptName: '',
    parentId: presetParentId.value,
    deptType: DEPT_TYPE_DEPT,
    leader: '',
    phone: '',
    deptSort: 0,
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 必填/长度规则与后端 DeptCreateDTO/DeptUpdateDTO 校验注解一致 */
const rules: CrudRules = {
  deptCode: [
    { required: true, message: '请输入组织编码' },
    { max: 50, message: '组织编码长度不能超过 50' },
  ],
  deptName: [
    { required: true, message: '请输入组织名称' },
    { max: 100, message: '组织名称长度不能超过 100' },
  ],
  parentId: [{ required: true, message: '请选择上级组织' }],
  deptType: [{ required: true, message: '请选择组织类型' }],
  leader: [{ max: 50, message: '负责人长度不能超过 50' }],
  phone: [{ max: 30, message: '联系电话长度不能超过 30' }],
}

/** 空串收敛为 null，避免向 DTO 传空字符串 */
function toNullable(value: unknown): string | null {
  const text = typeof value === 'string' ? value.trim() : ''
  return text === '' ? null : text
}

async function handleSubmit(form: CrudForm): Promise<void> {
  const dto = {
    deptName: form.deptName as string,
    parentId: form.parentId as string,
    deptType: form.deptType as number,
    leader: toNullable(form.leader),
    phone: toNullable(form.phone),
    deptSort: Number(form.deptSort ?? 0),
    status: form.status as number,
    remark: toNullable(form.remark),
  }
  if (mode.value === 'create') {
    await createDept({ ...dto, deptCode: form.deptCode as string })
    message.success('新增组织成功')
  } else {
    await updateDept(editId.value as string, { ...dto, version: form.version as number })
    message.success('修改组织成功')
  }
  emit('saved')
}

/** 打开弹窗：edit 用行数据回填（含 remark/version）；create 可传父节点预设父级 */
function show(nextMode: CrudMode, record?: Record<string, unknown>, parent?: DeptTreeVO): void {
  mode.value = nextMode
  editId.value = nextMode === 'edit' ? ((record?.id as string) ?? null) : null
  presetParentId.value = (parent?.id as string | undefined) ?? '0'
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="组织部门"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col v-if="mode === 'create'" :span="12">
          <a-form-item label="组织编码" name="deptCode">
            <a-input v-model:value="(form.deptCode as string)" placeholder="如 ORG001" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="组织名称" name="deptName">
            <a-input v-model:value="(form.deptName as string)" placeholder="请输入组织名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="上级组织"
            name="parentId"
            extra="父级不可选择自身或自身下级，最多 5 级"
          >
            <a-tree-select
              v-model:value="(form.parentId as string)"
              :tree-data="parentTreeData"
              placeholder="请选择上级组织"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="label"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="组织类型" name="deptType">
            <a-select
              v-model:value="(form.deptType as number)"
              :options="[...DEPT_TYPE_OPTIONS]"
              placeholder="请选择组织类型"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="负责人" name="leader">
            <a-input v-model:value="(form.leader as string)" placeholder="选填" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话" name="phone">
            <a-input v-model:value="(form.phone as string)" placeholder="选填" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="显示顺序" name="deptSort">
            <a-input-number v-model:value="(form.deptSort as number)" :min="0" class="w-full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="(form.status as number)" :options="[...STATUS_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea
              v-model:value="(form.remark as string)"
              :rows="2"
              :maxlength="500"
              placeholder="选填"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </template>
  </AppCrudModal>
</template>

<style scoped>
.w-full {
  width: 100%;
}
</style>
