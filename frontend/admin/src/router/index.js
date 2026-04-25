import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginPage from '../pages/LoginPage.vue'
import DashboardPage from '../pages/DashboardPage.vue'
import UsersPage from '../pages/UsersPage.vue'
import AttendantsPage from '../pages/AttendantsPage.vue'
import AttendantDetailPage from '../pages/AttendantDetailPage.vue'
import OrdersPage from '../pages/OrdersPage.vue'
import OrderDetailPage from '../pages/OrderDetailPage.vue'
import LogsPage from '../pages/LogsPage.vue'
import SystemPage from '../pages/SystemPage.vue'


const routes = [
  { path: '/login', name: 'login', component: LoginPage, meta: { guestOnly: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: DashboardPage, meta: { requiresAuth: true } },
  { path: '/users', name: 'users', component: UsersPage, meta: { requiresAuth: true } },
  { path: '/attendants', name: 'attendants', component: AttendantsPage, meta: { requiresAuth: true } },
  { path: '/attendants/:id', name: 'attendant-detail', component: AttendantDetailPage, meta: { requiresAuth: true } },
  { path: '/orders', name: 'orders', component: OrdersPage, meta: { requiresAuth: true } },
  { path: '/orders/:id', name: 'order-detail', component: OrderDetailPage, meta: { requiresAuth: true } },
  { path: '/logs', name: 'logs', component: LogsPage, meta: { requiresAuth: true, requiresSuperAdmin: true } },
  { path: '/system', name: 'system', component: SystemPage, meta: { requiresAuth: true, requiresSuperAdmin: true } }
]


const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  authStore.restore()
  if (to.meta.requiresAuth && !authStore.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresSuperAdmin && !authStore.isSuperAdmin) {
    return { name: 'dashboard' }
  }
  if (to.meta.guestOnly && authStore.token) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
