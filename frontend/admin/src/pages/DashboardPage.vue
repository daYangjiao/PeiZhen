<template>
  <AppShell title="运营控制台" subtitle="指标与队列">
    <div class="dashboard-console">
      <section class="panel-card console-header">
        <div>
          <h3 class="section-title">运营总览</h3>
          <p class="section-copy">实时状态</p>
        </div>
        <button class="button button-secondary" type="button" @click="loadDashboard" :disabled="reloading || loading">
          {{ reloading ? '刷新中...' : '刷新' }}
        </button>
      </section>

      <template v-if="loading">
        <section class="overview-grid stats-grid">
          <div v-for="item in 6" :key="item" class="skeleton stat-skeleton"></div>
        </section>
        <section class="quick-entry-grid">
          <div v-for="item in 3" :key="`queue-${item}`" class="skeleton queue-skeleton"></div>
        </section>
        <section class="panel-card">
          <div class="skeleton table-skeleton"></div>
        </section>
      </template>

      <template v-else>
        <section class="overview-grid stats-grid">
          <StatCard
            v-for="card in dashboardMetricCards"
            :key="card.label"
            :label="card.label"
            :value="card.value"
            :hint="card.hint"
            :badge="card.badge"
            :tone="card.tone"
          />
        </section>

        <section class="quick-entry-grid">
          <button
            v-for="entry in quickEntries"
            :key="entry.key"
            class="quick-entry-card"
            type="button"
            @click="jumpTo(entry.route)"
          >
            <p class="quick-entry-title">{{ entry.title }}</p>
            <p class="quick-entry-count">{{ entry.count }}</p>
            <p class="quick-entry-copy">{{ entry.copy }}</p>
          </button>
        </section>

        <section v-if="dashboard.recentOperationLogs?.length" class="panel-card recent-orders">
          <div class="section-heading">
            <h3 class="section-title">最近操作</h3>
            <button class="button button-secondary" type="button" @click="jumpTo({ name: 'logs' })">查看日志</button>
          </div>
          <div class="operation-log-list">
            <div v-for="log in dashboard.recentOperationLogs" :key="log.id" class="operation-log-item">
              <div>
                <p class="table-cell-title">{{ mapLogAction(log.action) }}</p>
                <p class="table-cell-copy">{{ log.targetLabel || '-' }} · {{ formatDateTime(log.createTime) }}</p>
              </div>
              <span class="badge badge-blue">{{ mapLogModule(log.module) }}</span>
            </div>
          </div>
        </section>

        <section class="panel-card recent-orders">
          <div class="section-heading">
            <h3 class="section-title">最近订单</h3>
          </div>

          <div v-if="dashboard.recentOrders?.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>患者 / 用户</th>
                  <th>陪诊师</th>
                  <th>服务信息</th>
                  <th>金额</th>
                  <th class="status-cell">订单状态</th>
                  <th class="payment-cell">支付状态</th>
                  <th class="time-cell">创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in dashboard.recentOrders" :key="order.orderId">
                  <td>{{ order.orderNo }}</td>
                  <td>
                    <p class="table-cell-title">{{ order.patientName || order.userName || '-' }}</p>
                    <p class="table-cell-copy">{{ order.userPhone || '-' }}</p>
                  </td>
                  <td>
                    <p class="table-cell-title">{{ order.attendantName || '暂未接单' }}</p>
                    <p class="table-cell-copy">{{ order.attendantPhone || '-' }}</p>
                  </td>
                  <td>
                    <p class="table-cell-title">{{ order.hospital || '-' }}</p>
                    <p class="table-cell-copy">{{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</p>
                  </td>
                  <td>{{ formatMoney(order.orderAmount) }}</td>
                  <td class="status-cell"><span class="badge" :class="getOrderStatusBadge(order.orderStatus)">{{ getOrderStatusLabel(order.orderStatus, order.orderStatusLabel || '--') }}</span></td>
                  <td class="payment-cell"><span class="badge" :class="getPaymentStatusBadge(order.paymentStatus)">{{ getPaymentStatusLabel(order.paymentStatus, order.paymentStatusLabel || '--') }}</span></td>
                  <td class="time-cell">{{ formatDateTime(order.createTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">暂无最近订单</div>
        </section>

        <section v-if="errorMessage" class="empty-card error-card">
          {{ errorMessage }}
        </section>
      </template>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import StatCard from '../components/StatCard.vue'
import { useUiStore } from '../stores/ui'
import { fetchDashboard } from '../utils/admin-api'
import { getOrderStatusBadge, getOrderStatusLabel, getPaymentStatusBadge, getPaymentStatusLabel } from '../utils/admin-view'
import { buildDashboardMetricCards } from '../utils/dashboard-metrics'
import { formatDateTime, formatMoney } from '../utils/format'

const router = useRouter()
const uiStore = useUiStore()

const dashboard = reactive({
  totalUsers: 0,
  totalPatientUsers: 0,
  totalAttendants: 0,
  pendingAttendantReviews: 0,
  totalOrders: 0,
  todayOrders: 0,
  disputeOrders: 0,
  pendingDisputeOrders: 0,
  todayOperationCount: 0,
  recentOrders: [],
  recentOperationLogs: []
})

const loading = ref(true)
const reloading = ref(false)
const errorMessage = ref('')

const dashboardMetricCards = computed(() => buildDashboardMetricCards(dashboard))

const quickEntries = computed(() => ([
  {
    key: 'pending-attendant-review',
    title: '待审核陪诊师',
    count: dashboard.pendingAttendantReviews,
    copy: '待处理',
    route: { name: 'workbench', query: { type: 'ATTENDANT_REVIEW' } }
  },
  {
    key: 'today-orders',
    title: '今日新订单',
    count: dashboard.todayOrders,
    copy: '今日队列',
    route: { name: 'orders', query: { quick: 'today' } }
  },
  {
    key: 'dispute-orders',
    title: '争议处理中订单',
    count: dashboard.disputeOrders,
    copy: '优先处理',
    route: { name: 'workbench', query: { type: 'ORDER_DISPUTE' } }
  }
]))

const jumpTo = (route) => {
  router.push(route)
}

const mapLogModule = (module) => ({
  USER: '用户',
  ATTENDANT: '陪诊师',
  ORDER: '订单',
  ADMIN_ACCOUNT: '管理员'
})[module] || module || '-'

const mapLogAction = (action) => ({
  ENABLE_USER: '启用用户',
  DISABLE_USER: '禁用用户',
  APPROVE: '通过审核',
  REJECT: '驳回审核',
  BAN: '封禁',
  RESTORE: '恢复',
  CANCEL_ORDER: '取消订单',
  RESOLVE_DISPUTE: '处理争议',
  CREATE_ADMIN: '创建管理员',
  ENABLE_ADMIN: '启用管理员',
  DISABLE_ADMIN: '禁用管理员',
  DELETE_ADMIN: '删除管理员'
})[action] || action || '-'

const loadDashboard = async () => {
  const isFirstLoad = loading.value
  if (!isFirstLoad) {
    reloading.value = true
  }
  errorMessage.value = ''

  try {
    const response = await fetchDashboard()
    Object.assign(dashboard, response)
  } catch (error) {
    errorMessage.value = error.message || '首页数据加载失败'
    uiStore.toast(errorMessage.value, 'error')
  } finally {
    loading.value = false
    reloading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-console {
  display: grid;
  gap: 14px;
}

.console-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.console-header .section-title {
  margin: 0;
  font-size: 1.02rem;
}

.console-header .section-copy {
  margin: 2px 0 0;
  font-size: 0.82rem;
}

.stats-grid {
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: 10px;
}

.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.quick-entry-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: var(--radius-lg);
  background: #fff;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.quick-entry-card:hover {
  transform: translateY(-1px);
  border-color: rgba(14, 116, 144, 0.28);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.quick-entry-title {
  margin: 0;
  font-size: 0.82rem;
  color: #475569;
}

.quick-entry-count {
  margin: 6px 0 4px;
  font-size: 1.34rem;
  line-height: 1;
  font-weight: 700;
  color: #0f172a;
}

.quick-entry-copy {
  margin: 0;
  font-size: 0.78rem;
  color: #64748b;
}

.operation-log-list {
  display: grid;
  gap: 10px;
}

.operation-log-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface-soft);
}

.recent-orders {
  padding: 16px;
}

.recent-orders .section-heading {
  margin-bottom: 12px;
}

.stat-skeleton {
  min-height: 108px;
}

.queue-skeleton {
  min-height: 92px;
  border-radius: var(--radius-lg);
}

.table-skeleton {
  min-height: 220px;
  border-radius: var(--radius-lg);
}

.error-card {
  border-color: rgba(228, 85, 85, 0.3);
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .quick-entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .console-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-entry-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
