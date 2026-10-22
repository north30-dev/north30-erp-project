<script setup lang="ts">
// 搜索区公共组件（编码规范第 5 节三件套之一）：纯布局壳，不含业务语义。
// 字段由页面经默认插槽提供，查询/重置直接绑 useTable 的 search/reset。
defineProps<{
  /** 查询条件对象（useTable 的 draftQuery），插槽内字段直接 v-model:value 绑定其属性 */
  model: Record<string, unknown>
  /** 提交中禁用查询按钮（一般绑 useTable 的 loading） */
  loading?: boolean
}>()

const emit = defineEmits<{
  search: []
  reset: []
}>()
</script>

<template>
  <a-form layout="inline" class="app-search-form" :model="model">
    <slot />
    <a-form-item>
      <a-space>
        <a-button type="primary" :loading="loading" @click="emit('search')">查询</a-button>
        <a-button :disabled="loading" @click="emit('reset')">重置</a-button>
      </a-space>
    </a-form-item>
  </a-form>
</template>

<style scoped>
.app-search-form {
  margin-bottom: 16px;
}

.app-search-form :deep(.ant-form-item) {
  margin-right: 16px;
  margin-bottom: 12px;
}
</style>
