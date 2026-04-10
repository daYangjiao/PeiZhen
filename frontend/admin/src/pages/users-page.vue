<template>
  <app-shell title="用户管理">
    <div class="split-grid">
      <section class="panel-card">
        <div class="toolbar">
          <input v-model="filters.keyword" placeholder="搜索用户姓名 / 手机号 / ID" />
          <select v-model="filters.userType">
            <option value="">全部角色</option>
            <option value="0">普通用户</option>
            <option value="1">陪诊师</option>
            <option value="2">管理员</option>
          </select>
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="1">正常</option>
            <option value="0">禁用</option>
          </select>
          <button class="button-primary" @click="loadUsers">查询</button>
        </div>

        <div v-if="loading" class="loading">加载中...</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户</th>
              <th>角色</th>
              <th>状态</th>
              <th>订单数</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users.content || []" :key="item.id">
              <td>{{ item.id }}</td>
              <td>
                <div>{{ item.name || '-' }}</div>
                <div class="subtle">{{ item.phone || '-' }}</div>
              </td>
              <td>{{ item.userTypeLabel }}</td>
              <td><span class="badge" :class="item.status === 1 ? 'badge-success' : 'badge-danger'">{{ item.statusLabel }}</span></td>
              <td>{{ item.orderCount }}</td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td style="display:flex;gap:8px;flex-wrap:wrap">
                <button class="button-ghost" @click="selectUser(item.id)">详情</button>
                <button class="button-secondary" @click="toggleStatus(item)">
                  {{ item.status === 1 ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <span class="subtle">共 {{ users.totalElements || 0 }} 条</span>
          <button class="button-ghost" :disabled="(users.pageNumber || 0) === 0" @click="changePage(-1)">上一页</button>
          <button class="button-ghost" :disabled="users.last" @click="changePage(1)">下一页</button>
        </div>
      </section>

      <section class="panel-card">
        <div v-if="detailLoading" class="loading">详情加载中...</div>
        <div v-else-if="selectedUser" class="stack">
          <div>
            <h3 style="margin:0 0 6px">用户详情</h3>
            <p class="subtle" style="margin:0">可在这里确认账号信息与最近订单。</p>
          </div>
          <div class="meta-grid">
            <div class="meta-card"><div class="meta-label">姓名</div><div class="meta-value">{{ selectedUser.user?.name || '-' }}</div></div>
            <div class="meta-card"><div class="meta-label">手机号</div><div class="meta-value">{{ selectedUser.user?.phone || '-' }}</div></div>
            <div class="meta-card"><div class="meta-label">角色</div><div class="meta-value">{{ userTypeLabel(selectedUser.user?.userType) }}</div></div>
            <div class="meta-card"><div class="meta-label">状态</div><div class="meta-value">{{ selectedUser.user?.status === 0 ? '禁用' : '正常' }}</div></div>
            <div class="meta-card"><div class="meta-label">订单总数</div><div class="meta-value">{{ selectedUser.orderCount ?? 0 }}</div></div>
            <div class="meta-card"><div class="meta-label">完成订单</div><div class="meta-value">{{ selectedUser.completedOrderCount ?? 0 }}</div></div>
          </div>

          <div v-if="selectedUser.attendantProfile" class="stack">
            <h4 style="margin:0">陪诊师扩展资料</h4>
            <div class="meta-grid">
              <div class="meta-card"><div class="meta-label">医院</div><div class="meta-value">{{ selectedUser.attendantProfile.hospitalName || '-' }}</div></div>
              <div class="meta-card"><div class="meta-label">擅长领域</div><div class="meta-value">{{ selectedUser.attendantProfile.professionalField || '-' }}</div></div>
              <div class="meta-card"><div class="meta-label">从业年限</div><div class="meta-value">{{ selectedUser.attendantProfile.experienceYears || 0 }} 年</div></div>
            </div>
          </div>

          <div>
            <h4 style="margin:0 0 10px">最近订单</h4>
            <table class="data-table">
              <thead>
                <tr><th>订单号</th><th>状态</th><th>金额</th></tr>
              </thead>
              <tbody>
                <tr v-for="order in selectedUser.recentOrders || []" :key="order.orderId">
                  <td>{{ order.orderNo }}</td>
                  <td>{{ order.orderStatusLabel }}</td>
                  <td>{{ formatMoney(order.orderAmount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div v-else class="empty">选择左侧一位用户后，这里会显示详细信息。</div>
      </section>
    </div>
  </app-shell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppShell from '../components/app-shell.vue'
import { fetchUserDetail, fetchUsers, updateUserStatus } from '../utils/admin-api'
import { formatDateTime, formatMoney } from '../utils/format'

const filters = reactive({
  keyword: '',
  userType: '',
  status: ''
})

const loading = ref(false)
const detailLoading = ref(false)
const users = ref({})
const selectedUser = ref(null)

const userTypeLabel = (type) => {
  if (type === 0) return '普通用户'
  if (type === 1) return '陪诊师'
  if (type === 2) return '管理员'
  return '-'
}

const loadUsers = async (page = users.value.pageNumber || 0) => {
  loading.value = true
  try {
    users.value = await fetchUsers({
      ...filters,
      page,
      pageSize: 10
    })
    if (!selectedUser.value && users.value.content?.length) {
      await selectUser(users.value.content[0].id)
    }
  } finally {
    loading.value = false
  }
}

const selectUser = async (id) => {
  detailLoading.value = true
  try {
    selectedUser.value = await fetchUserDetail(id)
  } finally {
    detailLoading.value = false
  }
}

const toggleStatus = async (item) => {
  await updateUserStatus(item.id, item.status === 1 ? 0 : 1)
  await loadUsers(users.value.pageNumber || 0)
  if (selectedUser.value?.user?.id === item.id) {
    await selectUser(item.id)
  }
}

const changePage = async (step) => {
  const next = (users.value.pageNumber || 0) + step
  await loadUsers(next)
}

onMounted(() => {
  loadUsers()
})
</script>
