<script setup lang="ts">
// 列表页两卡结构公共组件（方案 10 第 4.4 节）：搜索卡（查询条件 + 右侧主操作）+ 表格卡。
// 纯布局壳，不含业务语义；无 #search 插槽时不渲染搜索卡（menu/dept 等树形页）。
</script>

<template>
  <div class="app-page-container">
    <a-card v-if="$slots.search" class="app-page-container__search" :bordered="false">
      <div class="app-page-container__search-row">
        <div class="app-page-container__search-fields"><slot name="search" /></div>
        <div v-if="$slots.actions" class="app-page-container__actions">
          <slot name="actions" />
        </div>
      </div>
    </a-card>
    <a-card v-else-if="$slots.actions" class="app-page-container__search" :bordered="false">
      <div class="app-page-container__actions app-page-container__actions--alone">
        <slot name="actions" />
      </div>
    </a-card>
    <a-card class="app-page-container__table" :bordered="false">
      <slot />
    </a-card>
  </div>
</template>

<style scoped>
.app-page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.app-page-container__search-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.app-page-container__search-fields {
  flex: 1;
  min-width: 0;
}

/* 覆盖 AppSearchForm 的页脚 margin（间距改由两卡结构承担） */
.app-page-container__search-fields :deep(.app-search-form) {
  margin-bottom: 0;
}

.app-page-container__actions {
  flex: none;
  display: flex;
  gap: 8px;
  align-items: center;
}

.app-page-container__actions--alone {
  justify-content: flex-end;
}
</style>
