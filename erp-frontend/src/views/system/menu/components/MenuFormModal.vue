<script setup lang="ts">
// 菜单新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，字段与后端 MenuCreateDTO/MenuUpdateDTO 对齐。
// 类型切换时按后端 SysMenu.requireTypeFieldsValid() 规则联动必填项；父级防环/层级以后端校验为准。
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { RadioChangeEvent } from 'ant-design-vue/es/radio'
import type { DefaultOptionType } from 'ant-design-vue/es/vc-tree-select/TreeSelect'
import AppCrudModal from '../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudMode, CrudRules } from '../../../../components/AppCrudModal/type'
import { createMenu, updateMenu } from '../../../../api/menu'
import {
  MENU_TYPE_BUTTON,
  MENU_TYPE_DIR,
  MENU_TYPE_MENU,
  MENU_TYPE_OPTIONS,
  STATUS_ENABLED,
  STATUS_OPTIONS,
  VISIBLE_OPTIONS,
  VISIBLE_SHOWN,
} from '../../../../constants'
import type { MenuNodeVO } from '../../../../types/api'

const props = defineProps<{
  /** 父级选择数据源（页面传入的完整菜单树） */
  menus: MenuNodeVO[]
}>()

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<CrudMode>('create')
const editId = ref<string | null>(null)
/** create 模式的父级/类型预设（AppCrudModal 打开时经 initialForm 读取） */
const presetParentId = ref('0')
const presetMenuType = ref<number>(MENU_TYPE_DIR)
/** 当前表单类型（驱动条件渲染与必填规则） */
const currentMenuType = ref<number>(MENU_TYPE_DIR)

/** 父级选择树：顶级（0）+ 菜单树（按钮类型不可作父级） */
const parentTreeData = computed<DefaultOptionType[]>(() => [
  { label: '顶级菜单', value: '0', children: toParentOptions(props.menus) },
])

function toParentOptions(nodes: MenuNodeVO[]): DefaultOptionType[] {
  return nodes
    .filter((node) => node.menuType !== MENU_TYPE_BUTTON)
    .map((node) => ({
      label: node.menuName,
      value: node.id,
      children: node.children?.length ? toParentOptions(node.children) : undefined,
    }))
}

/** create 模式初始值工厂（AppCrudModal 每次打开时调用；默认值与后端一致） */
function initialForm(): CrudForm {
  return {
    menuName: '',
    parentId: presetParentId.value,
    menuType: presetMenuType.value,
    path: '',
    component: '',
    perms: '',
    icon: '',
    menuSort: 0,
    visible: VISIBLE_SHOWN,
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 必填规则与后端充血校验一致：目录必填 path；菜单必填 path+component；按钮必填 perms */
const rules = computed<CrudRules>(() => {
  const type = currentMenuType.value
  const rulesMap: CrudRules = {
    menuName: [
      { required: true, message: '请输入菜单名称' },
      { max: 100, message: '菜单名称长度不能超过 100' },
    ],
    parentId: [{ required: true, message: '请选择上级菜单' }],
    menuType: [{ required: true, message: '请选择菜单类型' }],
    path: [{ max: 200, message: '路由地址长度不能超过 200' }],
    component: [{ max: 200, message: '前端组件路径长度不能超过 200' }],
    perms: [{ max: 100, message: '权限标识长度不能超过 100' }],
  }
  if (type === MENU_TYPE_DIR || type === MENU_TYPE_MENU) {
    rulesMap.path?.push({ required: true, message: '目录/菜单必须配置路由地址' })
  }
  if (type === MENU_TYPE_MENU) {
    rulesMap.component?.push({ required: true, message: '菜单必须配置前端组件路径' })
  }
  if (type === MENU_TYPE_BUTTON) {
    rulesMap.perms?.push({ required: true, message: '按钮必须配置权限标识' })
  }
  return rulesMap
})

/** 类型切换：同步外部类型状态，触发表单项显隐与规则重算 */
function handleMenuTypeChange(event: RadioChangeEvent): void {
  currentMenuType.value = Number(event.target.value)
}

/** 新增下级时的默认类型：目录→菜单，菜单→按钮 */
function defaultChildType(parent?: MenuNodeVO): number {
  if (parent?.menuType === MENU_TYPE_DIR) {
    return MENU_TYPE_MENU
  }
  if (parent?.menuType === MENU_TYPE_MENU) {
    return MENU_TYPE_BUTTON
  }
  return MENU_TYPE_DIR
}

/** 空串收敛为 null，避免向 DTO 传空字符串 */
function toNullable(value: unknown): string | null {
  const text = typeof value === 'string' ? value.trim() : ''
  return text === '' ? null : text
}

async function handleSubmit(form: CrudForm): Promise<void> {
  const type = form.menuType as number
  const dto = {
    menuName: form.menuName as string,
    parentId: form.parentId as string,
    menuType: type,
    // 按钮不承载路由信息；目录不承载组件路径（后端也会清空 button 的 path/component）
    path: type === MENU_TYPE_BUTTON ? null : toNullable(form.path),
    component: type === MENU_TYPE_MENU ? toNullable(form.component) : null,
    perms: toNullable(form.perms),
    icon: toNullable(form.icon),
    menuSort: Number(form.menuSort ?? 0),
    visible: form.visible as number,
    status: form.status as number,
    remark: toNullable(form.remark),
  }
  if (mode.value === 'create') {
    await createMenu(dto)
    message.success('新增菜单成功')
  } else {
    await updateMenu(editId.value as string, { ...dto, version: form.version as number })
    message.success('修改菜单成功')
  }
  emit('saved')
}

/** 打开弹窗：edit 用行数据回填（含 remark/version）；create 可传父节点预设父级与类型 */
function show(nextMode: CrudMode, record?: Record<string, unknown>, parent?: MenuNodeVO): void {
  mode.value = nextMode
  editId.value = nextMode === 'edit' ? ((record?.id as string) ?? null) : null
  presetParentId.value = (parent?.id as string | undefined) ?? '0'
  presetMenuType.value = defaultChildType(parent)
  currentMenuType.value =
    nextMode === 'edit' ? Number(record?.menuType ?? MENU_TYPE_DIR) : presetMenuType.value
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="菜单"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="菜单名称" name="menuName">
            <a-input v-model:value="(form.menuName as string)" placeholder="请输入菜单名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="上级菜单"
            name="parentId"
            extra="父级不可选择自身或自身下级，最多三级"
          >
            <a-tree-select
              v-model:value="(form.parentId as string)"
              :tree-data="parentTreeData"
              placeholder="请选择上级菜单"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="label"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="菜单类型" name="menuType">
            <a-radio-group
              v-model:value="(form.menuType as number)"
              :options="[...MENU_TYPE_OPTIONS]"
              @change="handleMenuTypeChange"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="currentMenuType !== MENU_TYPE_BUTTON" :span="12">
          <a-form-item label="路由地址" name="path">
            <a-input v-model:value="(form.path as string)" placeholder="如 /system/menu" />
          </a-form-item>
        </a-col>
        <a-col v-if="currentMenuType === MENU_TYPE_MENU" :span="12">
          <a-form-item label="前端组件路径" name="component">
            <a-input
              v-model:value="(form.component as string)"
              placeholder="如 system/menu/index"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item
            label="权限标识"
            name="perms"
            :extra="currentMenuType === MENU_TYPE_BUTTON ? '格式：{模块}:{对象}:{动作}' : '选填'"
          >
            <a-input
              v-model:value="(form.perms as string)"
              placeholder="如 system:menu:create"
              autocomplete="off"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="图标" name="icon">
            <a-input v-model:value="(form.icon as string)" placeholder="图标组件名，如 Menu" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="排序号" name="menuSort">
            <a-input-number v-model:value="(form.menuSort as number)" :min="0" class="w-full" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="是否显示" name="visible">
            <a-radio-group v-model:value="(form.visible as number)" :options="[...VISIBLE_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
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
