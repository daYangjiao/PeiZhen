<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="login-badge">元</div>
        <div>
          <p class="login-eyebrow">愈安伴后台</p>
          <h1>登录管理台</h1>
          <p>接入本地真实接口，专注桌面端管理操作。</p>
        </div>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <label class="login-field">
          <span>手机号</span>
          <input v-model.trim="form.account" class="field" type="text" placeholder="请输入管理员手机号" />
        </label>
        <label class="login-field">
          <span>密码</span>
          <input v-model.trim="form.password" class="field" type="password" placeholder="请输入管理员密码" />
        </label>

        <p v-if="errorMessage" class="login-error">{{ errorMessage }}</p>

        <button class="button button-primary login-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="login-footer">
        <p>默认通过 `/api/admin/auth/login` 与本地后端联调。</p>
        <p>登录后会自动恢复后台会话并接入 `/api/admin/*` 实际数据。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { loginAdmin } from '../utils/admin-api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUiStore()

const form = reactive({
  account: '',
  password: ''
})

const loading = ref(false)
const errorMessage = ref('')

const handleSubmit = async () => {
  if (!form.account || !form.password) {
    errorMessage.value = '请先填写完整账号和密码。'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await loginAdmin(form)
    authStore.setSession(response.token, response.userInfo)
    uiStore.toast('登录成功', 'success')
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard')
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请检查账号密码。'
    uiStore.toast(errorMessage.value, 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: min(100%, 460px);
  padding: 30px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(220, 232, 251, 0.92);
  box-shadow: 0 24px 54px rgba(23, 68, 150, 0.12);
}

.login-brand {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 16px;
  align-items: center;
  margin-bottom: 24px;
}

.login-badge {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #2479ff 0%, #52b0ff 100%);
}

.login-eyebrow {
  margin: 0 0 6px;
  color: #6b7a96;
  font-size: 12px;
}

.login-brand h1 {
  margin: 0;
  font-size: 30px;
}

.login-brand p:last-child {
  margin: 10px 0 0;
  color: #6b7a96;
  line-height: 1.6;
}

.login-form {
  display: grid;
  gap: 16px;
}

.login-field {
  display: grid;
  gap: 8px;
  color: #45536f;
  font-size: 14px;
}

.login-submit {
  width: 100%;
  min-height: 46px;
}

.login-error {
  margin: 0;
  color: #e45555;
  font-size: 13px;
}

.login-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(220, 232, 251, 0.9);
  color: #6b7a96;
  font-size: 12px;
  line-height: 1.7;
}

.login-footer p {
  margin: 0;
}
</style>
