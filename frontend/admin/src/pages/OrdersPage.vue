<template>
  <AppShell title="订单管理">
    <section class="panel-card">
      <div class="toolbar">
        <input v-model="filters.keyword" placeholder="搜索订单号 / 用户 / 陪诊师 / 医院" />
        <select v-model="filters.orderStatus">
          <option value="">全部订单状态</option>
          <option value="0">待支付</option>
          <option value="1">待接单</option>
          <option value="2">待服务</option>
          <option value="3">服务中</option>
          <option value="4">待确认费用</option>
          <option value="5">争议处理中</option>
          <option value="6">已完成</option>
          <option value="7">已取消</option>
        </select>
        <select v-model="filters.paymentStatus">
          <option value="">全部支付状态</option>
          <option value="0">待支付</option>
          <option value="1">已支付</option>
        </select>
        <input v-model="filters.startDate" type="date" />
        <input v-model="filters.endDate" type="date" />
        <button class="button-primary" @click="loadData">查询</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>用户</th>
            <th>陪诊师</th>
            <th>服务时间</th>
            <th>状态</th>
            <th>金额</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in orders.content || []" :key="item.orderId">
            <td>{{ item.orderNo }}</td>
            <td><div>{{ item.userName || '-' }}</div><div class="subtle">{{ item.userPhone || '-' }}</div></td>
            <td><div>{{ item.attendantName || '-' }}</div><div class="subtle">{{ item.attendantPhone || '-' }}</div></td>
            <td>{{ item.serviceDate || '-' }} {{ item.serviceTimeSlot || '' }}</td>
            <td><span class="badge" :class="item.orderStatus === 5 ? 'badge-warning' : item.orderStatus === 7 ? 'badge-danger' : 'badge-neutral'">{{ item.orderStatusLabel }}</span></td>
            <td>{{ formatMoney(item.orderAmount) }}</td>
            <td style="display:flex;gap:8px;flex-wrap:wrap">
              <RouterLink class="button-ghost" :to="`/orders/${item.orderId}`">详情</RouterLink>
              <button v-if="item.orderStatus !== 6 && item.orderStatus !== 7" class="button-danger" @click="handleCancel(item.orderId)">取消</button>
              <button v-if="item.orderStatus === 5" class="button-secondary" @click="handleResolve(item.orderId)">处理争议</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import { cancelOrder, fetchOrders, resolveDispute } from '../utils/admin-api'
import { formatMoney } from '../utils/format'

const filters = reactive({
  keyword: '',
  orderStatus: '',
  paymentStatus: '',
  startDate: '',
  endDate: ''
})

const loading = ref(false)
const orders = ref({})

const loadData = async () => {
  loading.value = true
  try {
    orders.value = await fetchOrders({
      ...filters,
      page: 0,
      pageSize: 20
    })
  } finally {
    loading.value = false
  }
}

const handleCancel = async (id) => {
  const reason = window.prompt('请输入取消原因')
  if (!reason) return
  await cancelOrder(id, { reason })
  await loadData()
}

const handleResolve = async (id) => {
  const finalDuration = window.prompt('请输入最终确认的服务时长（小时）')
  if (!finalDuration) return
  const finalOrderAmount = window.prompt('请输入最终确认金额（元）')
  await resolveDispute(id, { finalDuration, finalOrderAmount, adminRemark: '后台已处理争议订单' })
  await loadData()
}

onMounted(loadData)
</script>
