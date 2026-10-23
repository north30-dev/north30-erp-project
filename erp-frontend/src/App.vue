<script setup lang="ts">
// 根组件：ConfigProvider 统一中文化（分页/空数据/弹窗按钮等内置文案），内层承载路由出口
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import 'dayjs/locale/zh-cn'
</script>

<template>
  <a-config-provider :locale="zhCN">
    <router-view />
  </a-config-provider>
</template>
