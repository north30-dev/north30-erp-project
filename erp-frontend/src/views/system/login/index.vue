<script setup lang="ts">
// 登录页（方案 A：左右分栏——左侧品牌区、右侧表单区；登录与验证码链路不变）
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ApartmentOutlined, AppstoreOutlined, CalculatorOutlined, DatabaseOutlined, LockOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons-vue'
import { createCaptcha } from '../../../api/auth'
import { useAuthStore } from '../../../stores/auth'
import { RequestError } from '../../../utils/request'
import type { CaptchaVO } from '../../../types/api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

/** 手动点击验证码的最小刷新间隔（避免连续刷码触发取码接口 IP 频控） */
const CAPTCHA_REFRESH_INTERVAL = 2000

const form = reactive({ username: '', password: '', captcha: '', captchaKey: '' })
const captcha = ref<CaptchaVO | null>(null)
const captchaLoading = ref(false)
const submitting = ref(false)
let lastCaptchaRefreshAt = 0

/** 品牌区固定三条特性点（文案复用 README 定位，图标映射见方案 4.1） */
const features = [
  { icon: ApartmentOutlined, title: '全链路一体化', desc: '销售接单 → MRP 运算 → 采购生产 → 财务凭证' },
  { icon: DatabaseOutlined, title: '库存精准管控', desc: '流水先行、余额可溯' },
  { icon: CalculatorOutlined, title: '产品级成本核算', desc: '材料人工制费归集到单' },
]

/**
 * 拉取图形验证码。
 * @param force 是否跳过最小刷新间隔（进页面与登录失败后必须强制刷新，因验证码一次性有效）
 */
async function refreshCaptcha(force = false): Promise<void> {
  if (captchaLoading.value) {
    return
  }
  if (!force && Date.now() - lastCaptchaRefreshAt < CAPTCHA_REFRESH_INTERVAL) {
    message.warning('验证码刷新过于频繁，请稍后再试')
    return
  }
  captchaLoading.value = true
  try {
    captcha.value = await createCaptcha()
    form.captchaKey = captcha.value.captchaKey
    lastCaptchaRefreshAt = Date.now()
  } catch {
    // 取码失败（如 18042 限流）保留现有验证码，错误提示由 request 拦截器统一弹出
  } finally {
    captchaLoading.value = false
  }
}
refreshCaptcha(true)

async function handleLogin(): Promise<void> {
  if (!form.username || !form.password || !form.captcha) {
    message.warning('请填写完整的登录信息')
    return
  }
  submitting.value = true
  try {
    await auth.login({ ...form })
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (error) {
    // 错误提示由 request 拦截器统一弹出；验证码一次性有效，普通失败后必须刷新。
    // 限流（18042）与参数校验失败（10001）不刷新，避免继续消耗取码配额。
    const code = error instanceof RequestError ? error.code : undefined
    if (code !== 18042 && code !== 10001) {
      await refreshCaptcha(true)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区（<768px 隐藏） -->
    <div class="login-page__brand">
      <div class="login-page__logo">
        <AppstoreOutlined class="login-page__logo-icon" />
        <span>North30 ERP</span>
      </div>
      <div class="login-page__slogan">业务财务一体化的 MTO 离散制造 ERP</div>
      <ul class="login-page__features">
        <li v-for="item in features" :key="item.title" class="login-page__feature">
          <component :is="item.icon" class="login-page__feature-icon" />
          <div>
            <div class="login-page__feature-title">{{ item.title }}</div>
            <div class="login-page__feature-desc">{{ item.desc }}</div>
          </div>
        </li>
      </ul>
    </div>
    <!-- 右侧表单区 -->
    <div class="login-page__panel">
      <div class="login-page__form-wrap">
        <h1 class="login-page__title">欢迎登录</h1>
        <p class="login-page__subtitle">请使用系统账号登录</p>
        <a-form layout="vertical" @submit.prevent="handleLogin">
          <a-form-item>
            <a-input v-model:value="form.username" placeholder="用户名" size="large">
              <template #prefix><UserOutlined /></template>
            </a-input>
          </a-form-item>
          <a-form-item>
            <a-input-password v-model:value="form.password" placeholder="密码" size="large">
              <template #prefix><LockOutlined /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item>
            <div class="captcha-row">
              <a-input v-model:value="form.captcha" placeholder="验证码" size="large">
                <template #prefix><SafetyOutlined /></template>
              </a-input>
              <img
                v-if="captcha"
                :src="captcha.captchaImage"
                :title="`点击刷新（${captcha.expireSeconds} 秒内有效）`"
                :class="['captcha-image', { 'captcha-image--loading': captchaLoading }]"
                alt="验证码"
                @click="refreshCaptcha()"
              />
            </div>
          </a-form-item>
          <a-button type="primary" html-type="submit" block size="large" :loading="submitting">
            登录
          </a-button>
        </a-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

.login-page__brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 32px;
  width: 55%;
  padding: 48px 64px;
  background: var(--erp-gradient-brand);
  color: #fff;
}

.login-page__logo {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.login-page__logo-icon {
  font-size: 24px;
}

.login-page__slogan {
  font-size: 14px;
  opacity: 0.85;
}

.login-page__features {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.login-page__feature {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.login-page__feature-icon {
  flex: none;
  margin-top: 2px;
  font-size: 18px;
  opacity: 0.9;
}

.login-page__feature-title {
  font-size: 15px;
  font-weight: 600;
}

.login-page__feature-desc {
  margin-top: 4px;
  font-size: 13px;
  opacity: 0.7;
}

.login-page__panel {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  background: var(--erp-bg-layout);
}

.login-page__form-wrap {
  width: 100%;
  max-width: 360px;
  padding: 0 16px;
  background: var(--erp-bg-layout);
}

.login-page__title {
  margin: 0;
  color: rgba(0, 0, 0, 0.88);
  font-size: 18px;
  font-weight: 600;
}

.login-page__subtitle {
  margin: 8px 0 24px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-image {
  height: 40px;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.captcha-image--loading {
  cursor: wait;
  opacity: 0.6;
}

@media (max-width: 768px) {
  .login-page__brand {
    display: none;
  }
}
</style>
