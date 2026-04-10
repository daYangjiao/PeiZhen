<template>
  <app-shell title="陪诊师审核">
    <section class="panel-card" style="margin-bottom: 18px">
      <div class="meta-grid">
        <div class="meta-card">
          <div class="meta-label">待审核陪诊师</div>
          <div class="meta-value">{{ pendingCount }}</div>
        </div>
      </div>
    </section>

    <section class="panel-card">
      <div class="toolbar">
        <input v-model="filters.keyword" placeholder="搜索姓名 / 手机号 / 医院 / 擅长领域" />
        <select v-model="filters.auditStatus">
          <option value="">全部审核状态</option>
          <option value="0">待审核</option>
          <option value="1">正常</option>
          <option value="2">封禁</option>
          <option value="3">审核驳回</option>
        </select>
        <button class="button-primary" @click="loadData">查询</button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>陪诊师</th>
            <th>服务医院</th>
            <th>擅长领域</th>
            <th>审核状态</th>
            <th>账号状态</th>
            <th>服务次数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in attendants.content || []" :key="item.id">
            <td>
              <div>{{ item.name }}</div>
              <div class="subtle">{{ item.phone }}</div>
            </td>
            <td>{{ item.hospitalName || '-' }}</td>
            <td>{{ item.professionalField || '-' }}</td>
            <td><span class="badge" :class="statusBadge(item.status)">{{ item.statusLabel }}</span></td>
            <td><span class="badge" :class="item.userStatus === 1 ? 'badge-success' : 'badge-danger'">{{ item.userStatusLabel }}</span></td>
            <td>{{ item.serviceCount || 0 }}</td>
            <td style="display:flex;gap:8px;flex-wrap:wrap">
              <RouterLink class="button-ghost" :to="`/attendants/${item.id}`">查看</RouterLink>
              <button v-if="item.status === 0" class="button-secondary" @click="review(item.id, 'approve')">通过</button>
              <button v-if="item.status === 0" class="button-danger" @click="reject(item.id)">驳回</button>
              <button v-if="item.status === 1" class="button-danger" @click="ban(item.id)">封禁</button>
              <button v-if="item.status === 2 || item.status === 3" class="button-secondary" @click="review(item.id, 'restore')">恢复</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </app-shell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppShell from '../components/app-shell.vue'
import { fetchAttendants, reviewAttendant, updateAttendantStatus } from '../utils/admin-api'

const filters = reactive({
  keyword: '',
  auditStatus: ''
})

const loading = ref(false)
const attendants = ref({})
const pendingCount = ref(0)

const statusBadge = (status) => {
  if (status === 0) return 'badge-warning'
  if (status === 1) return 'badge-success'
  if (status === 2) return 'badge-danger'
  return 'badge-neutral'
}

const loadData = async () => {
  loading.value = true
  try {
    attendants.value = await fetchAttendants({
      ...filters,
      page: 0,
      pageSize: 20
    })
    const pending = await fetchAttendants({
      auditStatus: 0,
      page: 0,
      pageSize: 1
    })
    pendingCount.value = pending.totalElements || 0
  } finally {
    loading.value = false
  }
}

const review = async (id, action) => {
  await reviewAttendant(id, { action })
  await loadData()
}

const reject = async (id) => {
  const reason = window.prompt('请输入驳回原因')
  if (!reason) return
  await reviewAttendant(id, { action: 'reject', reason })
  await loadData()
}

const ban = async (id) => {
  const reason = window.prompt('请输入封禁原因（可选）') || ''
  await updateAttendantStatus(id, { status: 2, reason })
  await loadData()
}

onMounted(loadData)
</script>
