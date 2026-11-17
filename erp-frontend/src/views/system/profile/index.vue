<script setup lang="ts">
// 个人中心（我的）：展示当前登录用户档案与权限信息，提供本人改密入口。
import { computed, onMounted, ref } from 'vue'
import { KeyOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../../../stores/auth'
import { DATA_SCOPE_MAP } from '../../../constants'
import ChangePasswordModal from './components/ChangePasswordModal.vue'

const auth = useAuthStore()

/** 用户档案取自 auth store（路由守卫/登录后已加载，onMounted 再刷新一次保证时效） */
const user = computed(() => auth.userInfo)
const loading = ref(false)
const modalRef = ref<InstanceType<typeof ChangePasswordModal> | null>(null)

/** 空值统一占位 */
function text(value?: string | null): string {
  return value || '-'
}

/** 数据范围反查：常量表缺失时直接显示原值 */
const dataScopeText = computed(() => {
  const scope = user.value?.dataScope
  if (scope === undefined || scope === null) {
    return '-'
  }
  return DATA_SCOPE_MAP[scope] ?? String(scope)
})

onMounted(async () => {
  loading.value = true
  try {
    await auth.loadProfile()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="profile-page">
    <a-card title="个人中心">
      <template #extra>
        <a-button type="primary" @click="modalRef?.show()">
          <template #icon><KeyOutlined /></template>
          修改密码
        </a-button>
      </template>
      <a-spin :spinning="loading">
        <template v-if="user">
          <a-descriptions title="基本信息" :column="2" size="small" bordered>
            <a-descriptions-item label="用户编码">{{ text(user.userCode) }}</a-descriptions-item>
            <a-descriptions-item label="用户名">{{ text(user.username) }}</a-descriptions-item>
            <a-descriptions-item label="姓名">{{ text(user.realName) }}</a-descriptions-item>
            <a-descriptions-item label="部门">{{ text(user.deptName) }}</a-descriptions-item>
            <a-descriptions-item label="手机号">{{ text(user.phone) }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ text(user.email) }}</a-descriptions-item>
          </a-descriptions>

          <a-descriptions title="权限与登录" :column="2" size="small" bordered class="section">
            <a-descriptions-item label="角色">
              <a-space v-if="user.roles?.length" wrap>
                <a-tag v-for="role in user.roles" :key="role" color="blue">{{ role }}</a-tag>
              </a-space>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="数据范围">{{ dataScopeText }}</a-descriptions-item>
            <a-descriptions-item label="最后登录时间">{{ text(user.lastLoginTime) }}</a-descriptions-item>
            <a-descriptions-item label="最后登录 IP">{{ text(user.lastLoginIp) }}</a-descriptions-item>
          </a-descriptions>
        </template>
        <a-empty v-else-if="!loading" description="未获取到用户信息" />
      </a-spin>
    </a-card>

    <ChangePasswordModal ref="modalRef" />
  </div>
</template>

<style scoped>
.section {
  margin-top: 24px;
}
</style>
