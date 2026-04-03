<template>
  <div class="login-shell">
    <div class="login-card">
      <section class="login-hero">
        <p class="subtle" style="color: rgba(255,255,255,0.72); margin-top: 0">Yuanban Operations Console</p>
        <h1 class="login-title">让审核、订单和账号处理更稳。</h1>
        <p class="subtle" style="color: rgba(255,255,255,0.78)">
          统一查看待审核陪诊师、争议订单和用户状态，减少跨页面来回查找。
        </p>
        <div class="stats-grid" style="margin-top: 28px">
          <div class="stat-card" style="background: rgba(255,255,255,0.08); color:white">
            <div class="stat-label" style="color: rgba(255,255,255,0.68)">核心动作</div>
            <div class="stat-value" style="font-size: 24px">审核</div>
          </div>
          <div class="stat-card" style="background: rgba(255,255,255,0.08); color:white">
            <div class="stat-label" style="color: rgba(255,255,255,0.68)">统一视图</div>
            <div class="stat-value" style="font-size: 24px">订单</div>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <p class="subtle" style="margin-top: 0">管理员登录</p>
        <h2 class="page-title" style="font-size: 32px; margin-bottom: 18px">进入后台</h2>

        <form class="form-card" @submit.prevent="handleSubmit">
          <input v-model="form.account" placeholder="请输入管理员手机号" />
          <input v-model="form.password" placeholder="请输入密码" type="password" />
          <div v-if="errorMessage" class="danger-text">{{ errorMessage }}</div>
          <button class="button-primary" :disabled="submitting">
            {{ submitting ? '登录中...' : '登录后台' }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginAdmin } from '../utils/admin-api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  account: '',
  password: ''
})

const submitting = ref(false)
const errorMessage = ref('')

const handleSubmit = async () => {
  errorMessage.value = ''
  submitting.value = true
  try {
    const data = await loginAdmin(form)
    authStore.setSession(data.token, data.userInfo)
    router.push(route.query.redirect || '/dashboard')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    submitting.value = false
  }
}
</script>
