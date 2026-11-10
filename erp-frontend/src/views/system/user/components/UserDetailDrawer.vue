<script setup lang="ts">
// 用户详情抽屉（页面私有组件，只读）：按行 id 拉详情，分区展示基本信息/角色与仓库/登录与安全。
import { ref } from 'vue'
import { getUserDetail } from '../../../../api/user'
import { GENDER_OPTIONS, STATUS_ENABLED, STATUS_MAP } from '../../../../constants'
import type { UserDetailVO } from '../../../../types/api'

const open = ref(false)
const loading = ref(false)
const detail = ref<UserDetailVO | null>(null)

/** 打开抽屉并拉取详情（列表 VO 不含性别/安全字段，须重新请求） */
async function show(id: string): Promise<void> {
  open.value = true
  loading.value = true
  detail.value = null
  try {
    detail.value = await getUserDetail(id)
  } finally {
    loading.value = false
  }
}

/** 性别反查（constants 仅提供选项，无反查表） */
function genderText(gender: number): string {
  return GENDER_OPTIONS.find((option) => option.value === gender)?.label ?? '-'
}

defineExpose({ show })
</script>

<template>
  <a-drawer v-model:open="open" title="用户详情" width="560" destroy-on-close>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions title="基本信息" :column="2" size="small" bordered>
          <a-descriptions-item label="用户编码">{{ detail.userCode }}</a-descriptions-item>
          <a-descriptions-item label="用户名">{{ detail.username }}</a-descriptions-item>
          <a-descriptions-item label="姓名">{{ detail.realName }}</a-descriptions-item>
          <a-descriptions-item label="部门">{{ detail.deptName ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ detail.phone ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ detail.email ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ genderText(detail.gender) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detail.status === STATUS_ENABLED ? 'green' : 'red'">
              {{ STATUS_MAP[detail.status] ?? '-' }}
            </a-tag>
            <a-tag v-if="detail.isAdmin === 1" color="purple">内置</a-tag>
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions title="角色与仓库" :column="1" size="small" bordered class="section">
          <a-descriptions-item label="角色">
            <a-space v-if="detail.roles?.length" wrap>
              <a-tag v-for="role in detail.roles" :key="role" color="blue">{{ role }}</a-tag>
            </a-space>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="仓库">
            <template v-if="detail.warehouseIds?.length">
              <a-space wrap>
                <a-tag v-for="warehouseId in detail.warehouseIds" :key="warehouseId">
                  {{ warehouseId }}
                </a-tag>
              </a-space>
              <div class="hint">详情接口未返回仓库名称，以上为仓库 ID</div>
            </template>
            <span v-else>-</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions title="登录与安全" :column="2" size="small" bordered class="section">
          <a-descriptions-item label="最后登录时间">{{ detail.lastLoginTime ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="登录失败次数">{{ detail.loginFailCount }}</a-descriptions-item>
          <a-descriptions-item label="锁定时间">{{ detail.lockUntil ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="密码更新时间">{{ detail.passwordUpdateTime ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detail.remark ?? '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
      <a-empty v-else-if="!loading" description="未获取到用户详情" />
    </a-spin>
  </a-drawer>
</template>

<style scoped>
.section {
  margin-top: 24px;
}

.hint {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
