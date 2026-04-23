<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand-card">
        <div class="brand-badge">元</div>
        <div>
          <p class="brand-eyebrow">愈安伴</p>
          <h1 class="brand-title">管理后台</h1>
          <p class="brand-copy">电脑端运营台，聚焦用户、陪诊师、订单与审核。</p>
        </div>
      </div>

      <nav class="nav-list" aria-label="管理后台导航">
        <RouterLink
          v-for="item in menuItems"
          :key="item.name"
          :to="item.path"
          class="nav-item"
          :class="{ readonly: item.readonly }"
        >
          <div>
            <p class="nav-title">{{ item.label }}</p>
            <p class="nav-caption">{{ item.caption }}</p>
          </div>
          <span v-if="item.readonly" class="nav-tag">只读</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <p class="page-eyebrow">{{ subtitle || '元伴陪诊运营管理' }}</p>
          <h2 class="page-title">{{ title }}</h2>
        </div>

        <div class="topbar-actions">
          <div class="topbar-user">
            <div class="topbar-avatar">
              {{ displayName.slice(0, 1) }}
            </div>
            <div>
              <p class="topbar-name">{{ displayName }}</p>
              <p class="topbar-role">{{ roleText }}</p>
            </div>
          </div>
          <button class="button button-secondary" type="button" @click="logout">
            退出登录
          </button>
        </div>
      </header>

      <section class="admin-content">
        <slot />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})

const router = useRouter()
const authStore = useAuthStore()
authStore.restore()

const menuItems = [
  { name: 'dashboard', path: '/dashboard', label: '首页概览', caption: '核心经营数据与最近订单' },
  { name: 'users', path: '/users', label: '用户管理', caption: '用户查询、状态管理与订单侧写' },
  { name: 'attendants', path: '/attendants', label: '陪诊师管理', caption: '资质审核、账号状态与服务能力' },
  { name: 'orders', path: '/orders', label: '订单管理', caption: '订单详情、取消与争议处理' },
  { name: 'service', path: '/service', label: '服务管理', caption: '当前仅保留只读说明', readonly: true },
  { name: 'system', path: '/system', label: '系统设置', caption: '管理员账号管理与系统说明' }
]

const displayName = computed(() => authStore.user?.name || authStore.user?.account || '管理员')
const roleText = computed(() => '平台管理员')

const logout = () => {
  authStore.clearSession()
  router.replace('/login')
}
</script>
