<script setup lang="ts">
// 登录页（基建期占位版：表单与登录链路已通，视觉细节随布局阶段统一打磨）
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons-vue'
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
    <a-card class="login-card" title="North30 ERP">
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
    </a-card>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-card {
  width: 380px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-image {
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
}

.captcha-image--loading {
  cursor: wait;
  opacity: 0.6;
}
</style>
