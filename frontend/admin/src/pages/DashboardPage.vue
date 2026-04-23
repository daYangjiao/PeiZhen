<template>
  <AppShell title="首页概览" subtitle="核心数据与最近订单">
    <div class="page-stack">
      <div v-if="loading" class="overview-grid">
        <div v-for="item in 4" :key="item" class="skeleton"></div>
      </div>

      <template v-else>
        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">经营概览</h3>
              <p class="section-copy">全部数据来自 `/api/admin/dashboard/overview`，不再展示模拟趋势图。</p>
            </div>
            <button class="button button-secondary" type="button" @click="loadDashboard" :disabled="reloading">
              {{ reloading ? '刷新中...' : '刷新数据' }}
            </button>
          </div>

          <div class="overview-grid">
            <StatCard label="累计用户" :value="dashboard.totalUsers" hint="平台注册用户总量" badge="用户" tone="neutral" />
            <StatCard label="累计陪诊师" :value="dashboard.totalAttendants" hint="已进入后台视图的陪诊师数量" badge="陪诊师" tone="success" />
            <StatCard label="待审核陪诊师" :value="dashboard.pendingAttendantReviews" hint="待处理的资质审核数量" badge="审核" tone="warning" />
            <StatCard label="累计订单" :value="dashboard.totalOrders" hint="全部历史订单" badge="订单" tone="neutral" />
            <StatCard label="今日订单" :value="dashboard.todayOrders" hint="今日创建订单数量" badge="今日" tone="success" />
            <StatCard label="争议订单" :value="dashboard.disputeOrders" hint="当前待处理争议订单数量" badge="争议" tone="danger" />
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">待处理事项</h3>
              <p class="section-copy">基于概览指标的一键入口，跳转后自动带入默认筛选条件。</p>
            </div>
          </div>

          <div class="quick-entry-grid">
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
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">最近订单</h3>
              <p class="section-copy">用于后台快速确认最近服务动态与异常订单入口。</p>
            </div>
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
                  <th>订单状态</th>
                  <th>支付状态</th>
                  <th>创建时间</th>
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
                  <td><span class="badge" :class="getOrderStatusBadge(order.orderStatus)">{{ getOrderStatusLabel(order.orderStatus, order.orderStatusLabel || '--') }}</span></td>
                  <td><span class="badge" :class="getPaymentStatusBadge(order.paymentStatus)">{{ getPaymentStatusLabel(order.paymentStatus, order.paymentStatusLabel || '--') }}</span></td>
                  <td>{{ formatDateTime(order.createTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">当前没有最近订单数据。</div>
        </section>

        <section v-if="errorMessage" class="empty-card">
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
import { formatDateTime, formatMoney } from '../utils/format'

const router = useRouter()
const uiStore = useUiStore()

const dashboard = reactive({
  totalUsers: 0,
  totalAttendants: 0,
  pendingAttendantReviews: 0,
  totalOrders: 0,
  todayOrders: 0,
  disputeOrders: 0,
  recentOrders: []
})

const loading = ref(true)
const reloading = ref(false)
const errorMessage = ref('')

const quickEntries = computed(() => ([
  {
    key: 'pending-attendant-review',
    title: '待审核陪诊师',
    count: dashboard.pendingAttendantReviews,
    copy: '进入陪诊师管理并预置待审核筛选',
    route: { name: 'attendants', query: { quick: 'pending-review', auditStatus: '0' } }
  },
  {
    key: 'dispute-orders',
    title: '争议处理中订单',
    count: dashboard.disputeOrders,
    copy: '进入订单管理并预置争议状态筛选',
    route: { name: 'orders', query: { quick: 'dispute', orderStatus: '5' } }
  },
  {
    key: 'today-orders',
    title: '今日新订单',
    count: dashboard.todayOrders,
    copy: '进入订单管理并筛选今日创建订单',
    route: { name: 'orders', query: { quick: 'today' } }
  }
]))

const jumpTo = (route) => {
  router.push(route)
}

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
.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.quick-entry-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  padding: 14px;
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
  font-size: 0.85rem;
  color: #475569;
}

.quick-entry-count {
  margin: 8px 0 6px;
  font-size: 1.6rem;
  line-height: 1;
  font-weight: 700;
  color: #0f172a;
}

.quick-entry-copy {
  margin: 0;
  font-size: 0.84rem;
  color: #64748b;
}

@media (max-width: 1080px) {
  .quick-entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .quick-entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
