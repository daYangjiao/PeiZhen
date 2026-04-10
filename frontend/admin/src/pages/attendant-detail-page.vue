<template>
  <app-shell title="陪诊师详情">
    <div v-if="loading" class="panel-card loading">加载中...</div>
    <div v-else-if="detail" class="panel-grid">
      <section class="hero-card">
        <div style="display:flex;justify-content:space-between;gap:16px;align-items:flex-start;flex-wrap:wrap">
          <div>
            <p class="subtle" style="margin-top:0">资格审核中心</p>
            <h3 style="margin:0 0 8px">{{ detail.user?.name || '-' }}</h3>
            <p class="subtle" style="margin:0">{{ detail.user?.phone || '-' }} · {{ detail.attendant?.hospitalName || '未填写服务医院' }}</p>
          </div>
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <button v-if="detail.attendant?.status === 0" class="button-secondary" @click="review('approve')">审核通过</button>
            <button v-if="detail.attendant?.status === 0" class="button-danger" @click="reject">驳回</button>
            <button v-if="detail.attendant?.status === 1" class="button-danger" @click="ban">封禁</button>
            <button v-if="detail.attendant?.status === 2 || detail.attendant?.status === 3" class="button-secondary" @click="review('restore')">恢复</button>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <h3 style="margin-top:0">基础资料</h3>
        <div class="meta-grid">
          <div class="meta-card"><div class="meta-label">审核状态</div><div class="meta-value">{{ statusLabel(detail.attendant?.status) }}</div></div>
          <div class="meta-card"><div class="meta-label">账号状态</div><div class="meta-value">{{ detail.user?.status === 0 ? '禁用' : '正常' }}</div></div>
          <div class="meta-card"><div class="meta-label">从业年限</div><div class="meta-value">{{ detail.attendant?.experienceYears || 0 }} 年</div></div>
          <div class="meta-card"><div class="meta-label">累计服务</div><div class="meta-value">{{ detail.completedOrderCount || 0 }} 单</div></div>
          <div class="meta-card"><div class="meta-label">擅长领域</div><div class="meta-value">{{ detail.attendant?.professionalField || '-' }}</div></div>
          <div class="meta-card"><div class="meta-label">审核备注</div><div class="meta-value">{{ detail.attendant?.qualificationFailReason || '-' }}</div></div>
        </div>
        <div class="meta-card" style="margin-top:14px">
          <div class="meta-label">个人简介</div>
          <div class="meta-value">{{ detail.attendant?.introduction || '暂无简介' }}</div>
        </div>
      </section>

      <section class="panel-card">
        <h3 style="margin-top:0">三证材料</h3>
        <div class="detail-image-grid">
          <img class="detail-image" :src="detail.qualification?.idCardFrontFileUrl || detail.qualification?.idCardFileUrl || placeholder" alt="身份证正面" />
          <img class="detail-image" :src="detail.qualification?.idCardBackFileUrl || placeholder" alt="身份证反面" />
          <img class="detail-image" :src="detail.qualification?.practiceCertFileUrl || placeholder" alt="执业证" />
          <img class="detail-image" :src="detail.qualification?.healthCertFileUrl || placeholder" alt="健康证" />
        </div>
      </section>

      <section class="panel-card">
        <h3 style="margin-top:0">最近订单</h3>
        <table class="data-table">
          <thead><tr><th>订单号</th><th>用户</th><th>状态</th><th>金额</th></tr></thead>
          <tbody>
            <tr v-for="order in detail.recentOrders || []" :key="order.orderId">
              <td>{{ order.orderNo }}</td>
              <td>{{ order.userName || '-' }}</td>
              <td>{{ order.orderStatusLabel }}</td>
              <td>{{ formatMoney(order.orderAmount) }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </app-shell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '../components/app-shell.vue'
import { fetchAttendantDetail, reviewAttendant, updateAttendantStatus } from '../utils/admin-api'
import { formatMoney } from '../utils/format'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)
const placeholder = computed(() => '/uploads/user-placeholder.png')

const statusLabel = (status) => {
  if (status === 0) return '待审核'
  if (status === 1) return '正常'
  if (status === 2) return '封禁'
  if (status === 3) return '审核驳回'
  return '-'
}

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await fetchAttendantDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

const review = async (action) => {
  await reviewAttendant(route.params.id, { action })
  await loadDetail()
}

const reject = async () => {
  const reason = window.prompt('请输入驳回原因')
  if (!reason) return
  await reviewAttendant(route.params.id, { action: 'reject', reason })
  await loadDetail()
}

const ban = async () => {
  const reason = window.prompt('请输入封禁原因（可选）') || ''
  await updateAttendantStatus(route.params.id, { status: 2, reason })
  await loadDetail()
}

onMounted(loadDetail)
</script>
