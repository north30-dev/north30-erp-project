<script setup lang="ts">
// 根组件：ConfigProvider 统一中文化（分页/空数据/弹窗按钮等内置文案），
// 并注入主题 token（品牌主/中性底/圆角，见 styles/theme.css 注释）；后续暗色模式在此切换 darkAlgorithm
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import 'dayjs/locale/zh-cn'

const antTheme = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: '#2f6fe4',
    colorBgLayout: '#f5f6f8',
    borderRadius: 6,
    // 全站基础字号上调（antd 默认 14，100% 缩放下偏小）
    fontSize: 15,
  },
}
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="antTheme">
    <router-view />
  </a-config-provider>
</template>
