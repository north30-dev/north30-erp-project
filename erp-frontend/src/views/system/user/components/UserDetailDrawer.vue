<script setup lang="ts">
// 用户详情抽屉（页面私有组件，只读）：按行 id 拉详情，卡片分节 + 轻量键值行展示（方案 10 第 4.5 节排版规范）。
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
        <div class="detail-section">
          <div class="detail-section__title">基本信息</div>
          <dl class="detail-kv">
            <div class="detail-kv__row">
              <dt>用户编码</dt>
              <dd>{{ detail.userCode }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>用户名</dt>
              <dd>{{ detail.username }}<a-tag v-if="detail.isAdmin === 1" color="purple" class="detail-kv__tag">内置</a-tag></dd>
            </div>
            <div class="detail-kv__row">
              <dt>姓名</dt>
              <dd>{{ detail.realName }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>部门</dt>
              <dd>{{ detail.deptName ?? '-' }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>手机号</dt>
              <dd>{{ detail.phone ?? '-' }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>邮箱</dt>
              <dd>{{ detail.email ?? '-' }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>性别</dt>
              <dd>{{ genderText(detail.gender) }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>状态</dt>
              <dd>
                <a-tag :color="detail.status === STATUS_ENABLED ? 'green' : 'red'">
                  {{ STATUS_MAP[detail.status] ?? '-' }}
                </a-tag>
              </dd>
            </div>
          </dl>
        </div>

        <div class="detail-section">
          <div class="detail-section__title">角色与仓库</div>
          <dl class="detail-kv">
            <div class="detail-kv__row">
              <dt>角色</dt>
              <dd>
                <a-space v-if="detail.roles?.length" wrap>
                  <a-tag v-for="role in detail.roles" :key="role" color="blue">{{ role }}</a-tag>
                </a-space>
                <span v-else>-</span>
              </dd>
            </div>
            <div class="detail-kv__row">
              <dt>仓库</dt>
              <dd>
                <span v-if="detail.warehouseIds?.length" class="detail-kv__hint">详情接口未返回仓库名称，暂不可展示</span>
                <span v-else>-</span>
              </dd>
            </div>
          </dl>
        </div>

        <div class="detail-section">
          <div class="detail-section__title">登录与安全</div>
          <dl class="detail-kv">
            <div class="detail-kv__row">
              <dt>最后登录时间</dt>
              <dd>{{ detail.lastLoginTime ?? '-' }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>登录失败次数</dt>
              <dd>{{ detail.loginFailCount }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>锁定时间</dt>
              <dd>{{ detail.lockUntil ?? '-' }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>密码更新时间</dt>
              <dd>{{ detail.passwordUpdateTime ?? '-' }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>创建时间</dt>
              <dd>{{ detail.createTime }}</dd>
            </div>
            <div class="detail-kv__row">
              <dt>备注</dt>
              <dd>{{ detail.remark ?? '-' }}</dd>
            </div>
          </dl>
        </div>
      </template>
      <a-empty v-else-if="!loading" description="未获取到用户详情" />
    </a-spin>
  </a-drawer>
</template>

<style scoped>
.detail-section {
  margin-top: 16px;
}

.detail-section:first-child {
  margin-top: 0;
}

.detail-section__title {
  margin-bottom: 8px;
  color: var(--erp-text);
  font-size: 14px;
  font-weight: 600;
}

.detail-kv {
  margin: 0;
}

.detail-kv__row {
  display: flex;
  padding: 6px 0;
  font-size: 14px;
  line-height: 22px;
  border-bottom: 1px solid var(--erp-border-line);
}

.detail-kv__row:last-child {
  border-bottom: none;
}

.detail-kv dt {
  flex: none;
  width: 110px;
  color: var(--erp-text-tertiary);
}

.detail-kv dd {
  flex: 1;
  min-width: 0;
  margin: 0;
  color: var(--erp-text);
  word-break: break-all;
}

.detail-kv__tag {
  margin-inline-start: 8px;
}

.detail-kv__hint {
  color: var(--erp-text-tertiary);
  font-size: 12px;
}
</style>
