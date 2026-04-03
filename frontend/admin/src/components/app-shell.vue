<template>
  <div class="page-grid">
    <aside class="sidebar">
      <div class="brand-mark">
        <h1 class="brand-title">元伴后台</h1>
        <p class="brand-subtitle">围绕用户、陪诊师、订单与审核的运营工作台。</p>
      </div>

      <nav class="nav-list">
        <RouterLink class="nav-item" to="/dashboard">概览看板</RouterLink>
        <RouterLink class="nav-item" to="/users">用户管理</RouterLink>
        <RouterLink class="nav-item" to="/attendants">陪诊师审核</RouterLink>
        <RouterLink class="nav-item" to="/orders">订单管理</RouterLink>
      </nav>
    </aside>

    <main class="content-shell">
      <header class="topbar">
        <div>
          <p class="subtle" style="margin: 0 0 4px">运营管理台</p>
          <h2 class="page-title">{{ title }}</h2>
        </div>
        <div style="display:flex;align-items:center;gap:12px">
          <div class="meta-card">
            <div class="meta-label">当前管理员</div>
            <div class="meta-value">{{ authStore.user?.name || authStore.user?.phone || '未登录' }}</div>
          </div>
          <button class="button-ghost" @click="logout">退出登录</button>
        </div>
      </header>

      <slot />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

defineProps({
  title: {
    type: String,
    required: true
  }
})

const router = useRouter()
const authStore = useAuthStore()

const logout = () => {
  authStore.clearSession()
  router.push({ name: 'login' })
}
</script>
