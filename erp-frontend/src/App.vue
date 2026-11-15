<script setup lang="ts">
// 根组件：ConfigProvider 统一中文化（分页/空数据/弹窗按钮等内置文案），
// 并注入主题 token（品牌主/中性底/圆角，见 styles/theme.css 注释）；后续暗色模式在此切换 darkAlgorithm。
// 顶部 3px 路由切换进度条（方案 4.6）：router 前后置钩子驱动，纯 CSS 流动动画，无第三方依赖。
import { onBeforeUnmount, ref } from 'vue'
import { theme } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import 'dayjs/locale/zh-cn'
import router from './router'

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

/** 进度条状态：active=加载中（流动动画），done=完成（满宽淡出后隐藏） */
const progressActive = ref(false)
const progressDone = ref(false)
let hideTimer: ReturnType<typeof setTimeout> | undefined

const removeBefore = router.beforeEach(() => {
  clearTimeout(hideTimer)
  progressDone.value = false
  progressActive.value = true
})
const removeAfter = router.afterEach(() => {
  progressActive.value = false
  progressDone.value = true
  hideTimer = setTimeout(() => {
    progressDone.value = false
  }, 300)
})
const removeError = router.onError(() => {
  progressActive.value = false
  progressDone.value = true
  hideTimer = setTimeout(() => {
    progressDone.value = false
  }, 300)
})
onBeforeUnmount(() => {
  removeBefore()
  removeAfter()
  removeError()
  clearTimeout(hideTimer)
})
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="antTheme">
    <div
      v-if="progressActive || progressDone"
      :class="['route-progress', { 'route-progress--active': progressActive, 'route-progress--done': progressDone }]"
    />
    <router-view />
  </a-config-provider>
</template>

<style>
.route-progress {
  position: fixed;
  z-index: 2000;
  top: 0;
  right: 0;
  left: 0;
  height: 3px;
  background: var(--erp-brand);
  transition: width 0.3s ease, opacity 0.3s ease;
}

/* 加载中：窄条流动动画 */
.route-progress--active {
  width: 40%;
  opacity: 1;
  animation: route-progress-slide 1.2s ease-in-out infinite;
}

/* 完成：满宽后淡出 */
.route-progress--done {
  width: 100%;
  opacity: 0;
}

@keyframes route-progress-slide {
  0% {
    margin-left: 0;
  }

  100% {
    margin-left: 60%;
  }
}
</style>
