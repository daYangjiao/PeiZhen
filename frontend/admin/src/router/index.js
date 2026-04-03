import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginPage from '../pages/login-page.vue'
import DashboardPage from '../pages/dashboard-page.vue'
import UsersPage from '../pages/users-page.vue'
import AttendantsPage from '../pages/attendants-page.vue'
import AttendantDetailPage from '../pages/attendant-detail-page.vue'
import OrdersPage from '../pages/orders-page.vue'
import OrderDetailPage from '../pages/order-detail-page.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginPage, meta: { guestOnly: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: DashboardPage, meta: { requiresAuth: true } },
  { path: '/users', name: 'users', component: UsersPage, meta: { requiresAuth: true } },
  { path: '/attendants', name: 'attendants', component: AttendantsPage, meta: { requiresAuth: true } },
  { path: '/attendants/:id', name: 'attendant-detail', component: AttendantDetailPage, meta: { requiresAuth: true } },
  { path: '/orders', name: 'orders', component: OrdersPage, meta: { requiresAuth: true } },
  { path: '/orders/:id', name: 'order-detail', component: OrderDetailPage, meta: { requiresAuth: true } }
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
  if (to.meta.guestOnly && authStore.token) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
