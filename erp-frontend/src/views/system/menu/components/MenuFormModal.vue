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
  MAX_MENU_DEPTH,
  MENU_TYPE_BUTTON,
  MENU_TYPE_DIR,
  MENU_TYPE_MENU,
  MENU_TYPE_OPTIONS,
  PERMS_ACTION_SUGGESTIONS,
  PERMS_MODULE_OPTIONS,
  SEGMENT_PATTERN,
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

/** 父级类型配对规则（与后端 SysMenu.requireValidChildType 同语义）：返回 childType 允许的父级类型集合 */
function allowedParentTypes(childType: number): number[] {
  if (childType === MENU_TYPE_DIR) {
    return [MENU_TYPE_DIR]
  }
  if (childType === MENU_TYPE_MENU) {
    return [MENU_TYPE_DIR, MENU_TYPE_MENU]
  }
  // 按钮：仅菜单
  return [MENU_TYPE_MENU]
}

/** 在树中查找节点类型（找不到返回 0，等价顶级） */
function findNodeType(nodes: MenuNodeVO[], id: string): number {
  for (const node of nodes) {
    if (node.id === id) {
      return node.menuType
    }
    if (node.children?.length) {
      const found = findNodeType(node.children, id)
      if (found !== 0) {
        return found
      }
    }
  }
  return 0
}

/** 收集自身及全部后代 id（edit 换父级防环：候选中排除） */
function collectSelfAndDescendants(nodes: MenuNodeVO[], rootId: string, acc: Set<string>): void {
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

function collectAllIds(node: MenuNodeVO, acc: Set<string>): void {
  acc.add(node.id)
  for (const child of node.children ?? []) {
    collectAllIds(child, acc)
  }
}

/** 计算各节点深度（顶级=1） */
function buildDepthMap(nodes: MenuNodeVO[], depth = 1, map = new Map<string, number>()): Map<string, number> {
  for (const node of nodes) {
    map.set(node.id, depth)
    if (node.children?.length) {
      buildDepthMap(node.children, depth + 1, map)
    }
  }
  return map
}

/**
 * 父级选择树：按当前类型配对（顶级仅目录/菜单，按钮无顶级）、edit 模式排除自身子树（防环）、
 * 深度达上限的节点保留展示但禁用（弱约束，最终以后端 18028 兜底）。
 */
const parentTreeData = computed<DefaultOptionType[]>(() => {
  const childType = currentMenuType.value
  const forbidden = new Set<string>()
  if (mode.value === 'edit' && editId.value) {
    collectSelfAndDescendants(props.menus, editId.value, forbidden)
  }
  const depthMap = buildDepthMap(props.menus)
  if (childType === MENU_TYPE_BUTTON) {
    // 按钮只能挂菜单下，无顶级选项
    return toParentOptions(props.menus, childType, forbidden, depthMap, 1)
  }
  return [{ label: '顶级菜单', value: '0', children: toParentOptions(props.menus, childType, forbidden, depthMap, 1) }]
})

function toParentOptions(
  nodes: MenuNodeVO[],
  childType: number,
  forbidden: Set<string>,
  depthMap: Map<string, number>,
  depth: number,
): DefaultOptionType[] {
  const allowedTypes = allowedParentTypes(childType)
  return nodes
    .filter((node) => !forbidden.has(node.id))
    .map((node) => {
      // 父深度 + 1 ≤ MAX_MENU_DEPTH：深度已达上限的节点不可作父级
      const tooDeep = (depthMap.get(node.id) ?? depth) >= MAX_MENU_DEPTH
      const typeMismatch = !allowedTypes.includes(node.menuType)
      const disabled = typeMismatch || tooDeep
      return {
        label: tooDeep && !typeMismatch ? `${node.menuName}（已达最大层级）` : node.menuName,
        value: node.id,
        disabled,
        children: node.children?.length
          ? toParentOptions(node.children, childType, forbidden, depthMap, depth + 1)
          : undefined,
      }
    })
}

/** create 模式初始值工厂（AppCrudModal 每次打开时调用；默认值与后端一致） */
function initialForm(): CrudForm {
  return {
    menuName: '',
    parentId: presetParentId.value,
    menuType: presetMenuType.value,
    path: '',
    component: '',
    permsModule: '',
    permsObject: '',
    permsAction: '',
    icon: '',
    visible: VISIBLE_SHOWN,
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 与后端 SysMenu 正则同语义：path 小写、component 允许大驼峰（与前端页面组件命名一致） */
const PATH_PATTERN = /^\/[a-z0-9-]+(?:\/[a-z0-9-]+)*$/
const COMPONENT_PATTERN = /^[a-zA-Z0-9-]+(?:\/[a-zA-Z0-9-]+)+$/

/** 必填与格式规则与后端充血校验一致：目录必填 path（格式）；菜单必填 path+component（格式）；按钮必填 perms（格式） */
const rules = computed<CrudRules>(() => {
  const type = currentMenuType.value
  const rulesMap: CrudRules = {
    menuName: [
      { required: true, message: '请输入菜单名称' },
      { max: 100, message: '菜单名称长度不能超过 100' },
    ],
    parentId: [{ required: true, message: '请选择上级菜单' }],
    menuType: [{ required: true, message: '请选择菜单类型' }],
    path: [
      { max: 200, message: '路由地址长度不能超过 200' },
      { pattern: PATH_PATTERN, message: '格式如 /system/menu（以 / 开头，小写字母/数字/中划线）' },
    ],
    component: [
      { max: 200, message: '前端组件路径长度不能超过 200' },
      { pattern: COMPONENT_PATTERN, message: '格式如 system/loginLog/index（至少两级，不以 / 开头、不以 .vue 结尾）' },
    ],
  }
  if (type === MENU_TYPE_DIR || type === MENU_TYPE_MENU) {
    rulesMap.path?.push({ required: true, message: '目录/菜单必须配置路由地址' })
  }
  if (type === MENU_TYPE_MENU) {
    rulesMap.component?.push({ required: true, message: '菜单必须配置前端组件路径' })
  }
  if (type === MENU_TYPE_BUTTON) {
    // 按钮的权限点必填；目录/菜单选填（提交时校验"填则三段完整"）
    rulesMap.permsModule = [
      { required: true, message: '请选择模块段' },
      { pattern: SEGMENT_PATTERN, message: '小写字母开头，仅小写字母/数字/中划线' },
    ]
    rulesMap.permsObject = [
      { required: true, message: '请输入对象段' },
      { pattern: SEGMENT_PATTERN, message: '小写字母开头，仅小写字母/数字/中划线' },
    ]
    rulesMap.permsAction = [
      { required: true, message: '请输入动作段' },
      { pattern: SEGMENT_PATTERN, message: '小写字母开头，仅小写字母/数字/中划线' },
    ]
  } else {
    // 选填：仅在已填内容时校验段格式；跨段完整性由提交时兜底
    rulesMap.permsModule = [{ pattern: SEGMENT_PATTERN, message: '小写字母开头，仅小写字母/数字/中划线' }]
    rulesMap.permsObject = [{ pattern: SEGMENT_PATTERN, message: '小写字母开头，仅小写字母/数字/中划线' }]
    rulesMap.permsAction = [{ pattern: SEGMENT_PATTERN, message: '小写字母开头，仅小写字母/数字/中划线' }]
  }
  return rulesMap
})

/** 模块段下拉候选（固定域） */
const permsModuleOptions = PERMS_MODULE_OPTIONS.map((value) => ({ label: value, value }))

/** 动作段自动完成候选（开放集合，允许自填） */
const permsActionOptions = PERMS_ACTION_SUGGESTIONS.map((value) => ({ label: value, value }))

/** 类型切换：同步外部类型状态、重算条件渲染，并按新类型重置非法父级 */
function handleMenuTypeChange(event: RadioChangeEvent, form: CrudForm): void {
  currentMenuType.value = Number(event.target.value)
  const parentId = (form.parentId as string | undefined) ?? ''
  const parentType = parentId === '0' || parentId === '' ? 0 : findNodeType(props.menus, parentId)
  if (currentMenuType.value === MENU_TYPE_DIR && parentType === MENU_TYPE_MENU) {
    // 菜单→目录属类型倒挂，父级重置为顶级
    form.parentId = '0'
  }
  if (currentMenuType.value === MENU_TYPE_BUTTON && parentType !== MENU_TYPE_MENU) {
    // 按钮只能挂菜单下，清空非法父级由用户重选
    form.parentId = undefined
  }
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

/** 三段是否全空 */
function permsEmpty(form: CrudForm): boolean {
  return [form.permsModule, form.permsObject, form.permsAction].every(
    (seg) => !(typeof seg === 'string' && seg.trim() !== ''),
  )
}

async function handleSubmit(form: CrudForm): Promise<void> {
  const type = form.menuType as number
  // 选填类型（目录/菜单）：填了任意一段则三段必须完整
  if (type !== MENU_TYPE_BUTTON && !permsEmpty(form)) {
    const missing = [form.permsModule, form.permsObject, form.permsAction].some(
      (seg) => !(typeof seg === 'string' && seg.trim() !== ''),
    )
    if (missing) {
      message.warning('填写权限标识时，模块/对象/动作三段必须完整')
      throw new Error('perms incomplete')
    }
  }
  const segments = [form.permsModule, form.permsObject, form.permsAction].map((seg) =>
    typeof seg === 'string' ? seg.trim() : '',
  )
  const dto = {
    menuName: form.menuName as string,
    parentId: form.parentId as string,
    menuType: type,
    // 按钮不承载路由信息；目录不承载组件路径（后端也会清空 button 的 path/component）
    path: type === MENU_TYPE_BUTTON ? null : toNullable(form.path),
    component: type === MENU_TYPE_MENU ? toNullable(form.component) : null,
    // 三段全空 = 不配置权限；否则拼接 a:b:c
    perms: permsEmpty(form) ? null : segments.join(':'),
    icon: toNullable(form.icon),
    // menuSort 不再手填：新增走后端同级 max+10 缺省，调整顺序用列表页上移/下移（08 文档阶段四）
    visible: form.visible as number,
    status: form.status as number,
    remark: toNullable(form.remark),
  }
  if (mode.value === 'create') {
    await createMenu(dto)
    message.success('新增菜单成功')
  } else {
    // 状态变更统一走列表页专用开关接口（/status），update 载荷不含状态字段
    const { status: _ignoredStatus, ...updateDto } = dto
    await updateMenu(editId.value as string, { ...updateDto, version: form.version as number })
    message.success('修改菜单成功')
  }
  emit('saved')
}

/** 编辑回填：按第一个冒号拆分 perms 到三段（目录/菜单/按钮统一）；非三段值整串放入对象段由用户修正 */
function splitPerms(record: Record<string, unknown>): void {
  const perms = (record.perms as string | undefined)?.trim() ?? ''
  const [module = '', object = '', action = ''] = perms.split(':')
  if (module && object && action) {
    record.permsModule = module
    record.permsObject = object
    record.permsAction = action
  } else {
    record.permsModule = ''
    record.permsObject = perms
    record.permsAction = ''
  }
}

/** 打开弹窗：edit 用行数据回填（含 remark/version）；create 可传父节点预设父级与类型 */
function show(nextMode: CrudMode, record?: Record<string, unknown>, parent?: MenuNodeVO): void {
  mode.value = nextMode
  editId.value = nextMode === 'edit' ? ((record?.id as string) ?? null) : null
  presetParentId.value = (parent?.id as string | undefined) ?? '0'
  presetMenuType.value = defaultChildType(parent)
  currentMenuType.value =
    nextMode === 'edit' ? Number(record?.menuType ?? MENU_TYPE_DIR) : presetMenuType.value
  if (nextMode === 'edit') {
    splitPerms(record as Record<string, unknown>)
  }
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="菜单"
    :width="760"
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
            extra="父级不可选择自身或自身下级，最多 4 级；顶级仅可选目录/菜单"
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
              @change="(event: RadioChangeEvent) => handleMenuTypeChange(event, form)"
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
        <a-col :span="24">
          <!-- 权限标识三段式：模块段下拉（固定域）、对象段自由输入、动作段自动完成（可自填）；按钮必填，目录/菜单选填 -->
          <!-- required 仅为红星视觉标记（必填规则挂在内层三段 form-item 上） -->
          <a-form-item
            label="权限标识"
            :required="currentMenuType === MENU_TYPE_BUTTON"
            :extra="
              currentMenuType === MENU_TYPE_BUTTON
                ? '格式：{模块}:{对象}:{动作}，按钮必填'
                : '选填，格式：{模块}:{对象}:{动作}；填写时三段需完整'
            "
          >
            <a-row :gutter="8">
              <a-col :span="8">
                <a-form-item name="permsModule">
                  <a-select
                    v-model:value="(form.permsModule as string)"
                    :options="permsModuleOptions"
                    placeholder="模块，如 system"
                    show-search
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item name="permsObject">
                  <a-input v-model:value="(form.permsObject as string)" placeholder="对象，如 menu" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item name="permsAction">
                  <a-auto-complete
                    v-model:value="(form.permsAction as string)"
                    :options="permsActionOptions"
                    placeholder="动作，如 create"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="图标" name="icon">
            <a-input v-model:value="(form.icon as string)" placeholder="图标组件名，如 Menu" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="是否显示" name="visible">
            <a-radio-group v-model:value="(form.visible as number)" :options="[...VISIBLE_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'create'" :span="8">
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
