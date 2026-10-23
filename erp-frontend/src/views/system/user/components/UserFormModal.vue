<script setup lang="ts">
// 用户新增/编辑弹窗（页面私有组件）：包装 AppCrudModal，表单字段与后端 UserCreateDTO/UserUpdateDTO 对齐。
// 编辑模式不展示 userCode/username/password（后端 update 接口不接收），仓库分配待 base 阶段后回接。
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import AppCrudModal from '../../../../components/AppCrudModal/index.vue'
import type { CrudForm, CrudRules } from '../../../../components/AppCrudModal/type'
import { getDeptTree } from '../../../../api/dept'
import { createUser, updateUser } from '../../../../api/user'
import { GENDER_UNKNOWN, GENDER_OPTIONS, STATUS_ENABLED, STATUS_OPTIONS } from '../../../../constants'
import { isPasswordComplexityValid, PASSWORD_COMPLEXITY_MESSAGE } from '../../../../utils/validators'
import type { DeptTreeVO } from '../../../../types/api'

const emit = defineEmits<{ saved: [] }>()

const crudRef = ref<InstanceType<typeof AppCrudModal> | null>(null)
const mode = ref<'create' | 'edit'>('create')
const editId = ref<string | null>(null)
const deptTree = ref<DeptTreeVO[]>([])
/** 密码提示交互状态：首次聚焦前为灰色提示，聚焦后按校验结果变红/变绿 */
const pwdTouched = ref(false)

/** 密码框校验状态：未交互不标色，交互后按复杂度给出 success/error */
function pwdStatus(value: string): '' | 'success' | 'error' {
  if (!pwdTouched.value) {
    return ''
  }
  return isPasswordComplexityValid(value ?? '') ? 'success' : 'error'
}

onMounted(async () => {
  deptTree.value = await getDeptTree()
})

/** a-tree-select 数据源：children 为 null 时收敛为 undefined，避免空子级渲染 */
const deptTreeData = computed(() => toTreeNodes(deptTree.value))
function toTreeNodes(nodes: DeptTreeVO[]): DeptTreeVO[] {
  return nodes.map((node) => ({
    ...node,
    children: node.children?.length ? toTreeNodes(node.children) : undefined,
  })) as DeptTreeVO[]
}

/** create 模式初始值工厂（AppCrudModal 每次打开时调用） */
function initialForm(): CrudForm {
  return {
    userCode: '',
    username: '',
    password: '',
    realName: '',
    deptId: null,
    phone: '',
    email: '',
    gender: GENDER_UNKNOWN,
    status: STATUS_ENABLED,
    remark: '',
    version: 0,
  }
}

/** 口令复杂度仅做前置提示，最终以后端校验（18009）为准 */
const rules: CrudRules = {
  userCode: [{ required: true, message: '请输入用户编码' }],
  username: [{ required: true, message: '请输入用户名' }],
  password: [
    { required: true, message: '请输入初始密码' },
    {
      validator: (_rule, value: string) =>
        isPasswordComplexityValid(value ?? '')
          ? Promise.resolve()
          : Promise.reject(PASSWORD_COMPLEXITY_MESSAGE),
      trigger: ['change', 'blur'],
    },
  ],
  realName: [{ required: true, message: '请输入真实姓名' }],
  email: [{ type: 'email', message: '邮箱格式不正确' }],
}

async function handleSubmit(form: CrudForm): Promise<void> {
  if (mode.value === 'create') {
    await createUser({
      userCode: form.userCode as string,
      username: form.username as string,
      password: form.password as string,
      realName: form.realName as string,
      deptId: (form.deptId as string | null) ?? null,
      warehouseIds: null,
      phone: (form.phone as string) || null,
      email: (form.email as string) || null,
      gender: form.gender as number,
      status: form.status as number,
      roleIds: null,
      remark: (form.remark as string) || null,
    })
    message.success('新增用户成功')
  } else {
    await updateUser(editId.value as string, {
      realName: form.realName as string,
      deptId: (form.deptId as string | null) ?? null,
      warehouseIds: null,
      phone: (form.phone as string) || null,
      email: (form.email as string) || null,
      gender: form.gender as number,
      status: form.status as number,
      remark: (form.remark as string) || null,
      version: form.version as number,
    })
    message.success('修改用户成功')
  }
  emit('saved')
}

/** 打开弹窗（edit 时 record 为后端 UserDetailVO，含 id/version 回填） */
function show(nextMode: 'create' | 'edit', record?: Record<string, unknown>): void {
  mode.value = nextMode
  editId.value = nextMode === 'edit' ? (record?.id as string) : null
  pwdTouched.value = false
  crudRef.value?.show(nextMode, record)
}

defineExpose({ show })
</script>

<template>
  <AppCrudModal
    ref="crudRef"
    title="用户"
    :initial-form="initialForm"
    :rules="rules"
    :on-submit="handleSubmit"
  >
    <template #default="{ form }">
      <a-row :gutter="16">
        <a-col v-if="mode === 'create'" :span="12">
          <a-form-item label="用户编码" name="userCode">
            <a-input v-model:value="(form.userCode as string)" placeholder="唯一编码，如 emp001" autocomplete="off" />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'create'" :span="12">
          <a-form-item label="用户名" name="username">
            <a-input v-model:value="(form.username as string)" placeholder="登录账号" autocomplete="off" />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'create'" :span="12">
          <a-form-item
            label="初始密码"
            name="password"
            :validate-status="pwdStatus(form.password as string)"
          >
            <a-input-password
              v-model:value="(form.password as string)"
              placeholder="请输入初始密码"
              autocomplete="new-password"
              @focus="pwdTouched = true"
            />
            <template #help>
              <span
                :class="[
                  'pwd-hint',
                  pwdTouched
                    ? isPasswordComplexityValid((form.password as string) ?? '')
                      ? 'pwd-hint--ok'
                      : 'pwd-hint--error'
                    : '',
                ]"
              >
                {{ PASSWORD_COMPLEXITY_MESSAGE }}
              </span>
            </template>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="真实姓名" name="realName">
            <a-input v-model:value="(form.realName as string)" placeholder="请输入姓名" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="所属部门" name="deptId">
            <a-tree-select
              v-model:value="(form.deptId as string | null)"
              :tree-data="deptTreeData"
              :field-names="{ label: 'deptName', value: 'id', children: 'children' }"
              allow-clear
              tree-default-expand-all
              placeholder="请选择部门"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="性别" name="gender">
            <a-select v-model:value="(form.gender as number)" :options="[...GENDER_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="(form.phone as string)" placeholder="选填" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="(form.email as string)" placeholder="选填" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="(form.status as number)" :options="[...STATUS_OPTIONS]" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="(form.remark as string)" :rows="2" :maxlength="500" placeholder="选填" />
          </a-form-item>
        </a-col>
      </a-row>
    </template>
  </AppCrudModal>
</template>

<style scoped>
/* 密码复杂度提示三态：默认灰、校验通过绿、不通过红（取 antd 标准色板） */
.pwd-hint {
  color: rgba(0, 0, 0, 0.45);
}
.pwd-hint--ok {
  color: #52c41a;
}
.pwd-hint--error {
  color: #ff4d4f;
}
</style>
