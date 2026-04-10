<template>
  <app-shell title="订单详情">
    <div v-if="loading" class="panel-card loading">加载中...</div>
    <div v-else-if="detail" class="panel-grid">
      <section class="hero-card">
        <p class="subtle" style="margin-top:0">订单编号</p>
        <h3 style="margin:0 0 8px">{{ detail.order?.orderNo }}</h3>
        <p class="subtle" style="margin:0">状态：{{ orderStatusLabel(detail.order?.orderStatus) }} · 支付：{{ detail.order?.paymentStatus === 1 ? '已支付' : '待支付' }}</p>
      </section>

      <section class="panel-card">
        <h3 style="margin-top:0">订单信息</h3>
        <div class="meta-grid">
          <div class="meta-card"><div class="meta-label">用户</div><div class="meta-value">{{ detail.user?.name || '-' }} / {{ detail.user?.phone || '-' }}</div></div>
          <div class="meta-card"><div class="meta-label">陪诊师</div><div class="meta-value">{{ detail.attendant?.name || detail.order?.attendantName || '-' }}</div></div>
          <div class="meta-card"><div class="meta-label">患者</div><div class="meta-value">{{ detail.order?.patientName || '-' }}</div></div>
          <div class="meta-card"><div class="meta-label">医院</div><div class="meta-value">{{ detail.order?.hospital || '-' }}</div></div>
          <div class="meta-card"><div class="meta-label">服务时间</div><div class="meta-value">{{ detail.order?.serviceDate || '-' }} {{ detail.order?.serviceTimeSlot || '' }}</div></div>
          <div class="meta-card"><div class="meta-label">订单金额</div><div class="meta-value">{{ formatMoney(detail.order?.orderAmount) }}</div></div>
          <div class="meta-card"><div class="meta-label">争议时长</div><div class="meta-value">{{ detail.order?.timeDisputeUserDuration || '-' }}</div></div>
          <div class="meta-card"><div class="meta-label">后台备注</div><div class="meta-value">{{ detail.order?.adminRemark || '-' }}</div></div>
        </div>
        <div class="meta-card" style="margin-top:14px">
          <div class="meta-label">争议说明 / 取消原因</div>
          <div class="meta-value">{{ detail.order?.timeDisputeReason || detail.order?.cancelReason || '暂无' }}</div>
        </div>
      </section>

      <section class="panel-card">
        <div class="toolbar" style="margin-bottom:0">
          <button v-if="detail.order?.orderStatus !== 6 && detail.order?.orderStatus !== 7" class="button-danger" @click="handleCancel">取消订单</button>
          <button v-if="detail.order?.orderStatus === 5" class="button-secondary" @click="handleResolve">处理争议</button>
        </div>
      </section>
    </div>
  </app-shell>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '../components/app-shell.vue'
import { cancelOrder, fetchOrderDetail, resolveDispute } from '../utils/admin-api'
import { formatMoney } from '../utils/format'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)

const orderStatusLabel = (status) => {
  const map = {
    0: '待支付',
    1: '待接单',
    2: '待服务',
    3: '服务中',
    4: '待确认费用',
    5: '争议处理中',
    6: '已完成',
    7: '已取消'
  }
  return map[status] || '-'
}

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await fetchOrderDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

const handleCancel = async () => {
  const reason = window.prompt('请输入取消原因')
  if (!reason) return
  await cancelOrder(route.params.id, { reason, adminRemark: '后台订单详情页取消' })
  await loadDetail()
}

const handleResolve = async () => {
  const finalDuration = window.prompt('请输入最终确认的服务时长（小时）')
  if (!finalDuration) return
  const finalOrderAmount = window.prompt('请输入最终确认金额（元）')
  await resolveDispute(route.params.id, { finalDuration, finalOrderAmount, adminRemark: '后台订单详情页处理争议' })
  await loadDetail()
}

onMounted(loadDetail)
</script>
