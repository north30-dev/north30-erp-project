<script setup lang="ts">
// 404 兜底页：动态路由之外的任意 URL（含 menus 为空时的首页）
import { useRouter } from 'vue-router'
import { HomeOutlined } from '@ant-design/icons-vue'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <a-result status="404" title="404" sub-title="抱歉，您访问的页面不存在或无权访问">
      <template #extra>
        <a-button type="primary" @click="router.replace('/')">
          <template #icon><HomeOutlined /></template>
          返回首页
        </a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}
</style>
