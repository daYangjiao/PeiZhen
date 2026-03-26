<template>
  <AppShell title="概览看板">
    <section class="hero-card">
      <p class="subtle" style="margin-top: 0">今天优先关注</p>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">总用户数</div>
          <div class="stat-value">{{ overview.totalUsers ?? '-' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">陪诊师总数</div>
          <div class="stat-value">{{ overview.totalAttendants ?? '-' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">待审核陪诊师</div>
          <div class="stat-value">{{ overview.pendingAttendantReviews ?? '-' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">争议订单</div>
          <div class="stat-value">{{ overview.disputeOrders ?? '-' }}</div>
        </div>
      </div>
    </section>

    <div class="panel-grid">
      <section class="panel-card">
        <div class="stats-grid">
          <div class="meta-card">
            <div class="meta-label">订单总量</div>
            <div class="meta-value">{{ overview.totalOrders ?? '-' }}</div>
          </div>
          <div class="meta-card">
            <div class="meta-label">今日新增订单</div>
            <div class="meta-value">{{ overview.todayOrders ?? '-' }}</div>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <div>
            <h3 style="margin:0">最近订单</h3>
            <p class="subtle" style="margin:6px 0 0">用来快速查看今天后台最容易跟进的单子。</p>
          </div>
        </div>

        <div v-if="loading" class="loading">加载中...</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>陪诊师</th>
              <th>状态</th>
              <th>金额</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in overview.recentOrders || []" :key="item.orderId">
              <td>
                <RouterLink :to="`/orders/${item.orderId}`">{{ item.orderNo }}</RouterLink>
              </td>
              <td>{{ item.userName || '-' }}</td>
              <td>{{ item.attendantName || '-' }}</td>
              <td><span class="badge badge-neutral">{{ item.orderStatusLabel }}</span></td>
              <td>{{ formatMoney(item.orderAmount) }}</td>
              <td>{{ formatDateTime(item.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import { fetchDashboard } from '../utils/admin-api'
import { formatDateTime, formatMoney } from '../utils/format'

const loading = ref(false)
const overview = ref({})

const loadData = async () => {
  loading.value = true
  try {
    overview.value = await fetchDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
