<template>
  <AppShell title="用户管理" subtitle="用户查询、状态变更与订单侧写">
    <div class="page-stack">
      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">筛选条件</h3>
            <p class="section-copy">支持按关键词、角色和账号状态查询用户。</p>
          </div>
        </div>

        <div class="toolbar filter-toolbar">
          <div class="toolbar-group filter-fields-row">
            <input v-model.trim="filters.keyword" class="field-inline" type="text" placeholder="姓名、手机号或 ID" @keyup.enter="submitFilters" />
            <select v-model="filters.userType" class="filter-select">
              <option v-for="option in userTypeOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
            </select>
            <select v-model="filters.status" class="filter-select">
              <option v-for="option in userStatusOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
          <div class="toolbar-group filter-actions-row">
            <button class="button button-primary" type="button" @click="submitFilters" :disabled="loading">查询</button>
            <button class="button button-ghost" type="button" @click="resetFilters" :disabled="loading">重置</button>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">用户列表</h3>
            <p class="section-copy">查看用户资料并管理账号状态。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="users.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户信息</th>
                  <th>角色</th>
                  <th>状态</th>
                  <th>订单数</th>
                  <th>注册时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in users" :key="user.id">
                  <td>{{ user.id }}</td>
                  <td>
                    <p class="table-cell-title">{{ user.name || '未命名用户' }}</p>
                    <p class="table-cell-copy">{{ user.phone || '-' }}</p>
                  </td>
                  <td>{{ getUserTypeLabel(user.userType, user.userTypeLabel || '--') }}</td>
                  <td><span class="badge" :class="getUserStatusBadge(user.status)">{{ getUserStatusLabel(user.status, user.statusLabel || '--') }}</span></td>
                  <td>{{ user.orderCount }}</td>
                  <td>{{ formatDateTime(user.createTime) }}</td>
                  <td>
                    <div class="table-actions">
                      <button class="button button-secondary" type="button" @click="openUserDetail(user)">查看详情</button>
                      <button class="button" :class="user.status === 1 ? 'button-danger' : 'button-primary'" type="button" @click="promptStatusChange(user)">
                        {{ user.status === 1 ? '禁用' : '恢复' }}
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">未查询到符合条件的用户。</div>
        </template>

        <div class="pagination">
          <p class="pagination-copy">共 {{ total }} 条，当前第 {{ page + 1 }} / {{ totalPages }} 页</p>
          <div class="pagination-controls">
            <select v-model.number="pageSize" class="filter-select" @change="changePageSize">
              <option :value="10">10 条 / 页</option>
              <option :value="20">20 条 / 页</option>
              <option :value="50">50 条 / 页</option>
            </select>
            <button class="button button-ghost" type="button" :disabled="page === 0 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="button button-ghost" type="button" :disabled="page + 1 >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <BaseDialog v-model="detailOpen" title="用户详情" description="用户资料与最近订单。" width="860px">
      <div v-if="detailLoading" class="skeleton"></div>
      <div v-else-if="selectedUserDetail" class="page-stack">
        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h4 class="section-title">基础信息</h4>
            </div>
          </div>
          <div class="kv-grid">
            <div class="kv-item">
              <p class="kv-label">姓名</p>
              <p class="kv-value">{{ selectedUserDetail.user.name || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">手机号</p>
              <p class="kv-value">{{ selectedUserDetail.user.phone || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">性别 / 年龄</p>
              <p class="kv-value">{{ selectedUserDetail.user.sex || '未知' }} / {{ selectedUserDetail.user.age || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">账号状态</p>
              <p class="kv-value">{{ getUserStatusLabel(selectedUserDetail.user.status, selectedUserDetail.user.statusLabel || '--') }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">总订单数</p>
              <p class="kv-value">{{ selectedUserDetail.orderCount }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">已完成订单数</p>
              <p class="kv-value">{{ selectedUserDetail.completedOrderCount }}</p>
            </div>
          </div>
        </section>

        <section v-if="selectedUserDetail.attendantProfile" class="panel-card">
          <div class="section-heading">
            <div>
              <h4 class="section-title">陪诊师附加资料</h4>
            </div>
          </div>
          <div class="kv-grid">
            <div class="kv-item">
              <p class="kv-label">常驻医院</p>
              <p class="kv-value">{{ selectedUserDetail.attendantProfile.hospitalName || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">擅长领域</p>
              <p class="kv-value">{{ selectedUserDetail.attendantProfile.professionalField || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">从业年限</p>
              <p class="kv-value">{{ selectedUserDetail.attendantProfile.experienceYears || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">资质状态</p>
              <p class="kv-value">{{ getAttendantStatusLabel(selectedUserDetail.attendantProfile.status, selectedUserDetail.attendantProfile.statusLabel || '--') }}</p>
            </div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h4 class="section-title">最近订单</h4>
            </div>
          </div>
          <div v-if="selectedUserDetail.recentOrders?.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>医院</th>
                  <th>服务时间</th>
                  <th>金额</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in selectedUserDetail.recentOrders" :key="order.orderId">
                  <td>{{ order.orderNo }}</td>
                  <td>{{ order.hospital || '-' }}</td>
                  <td>{{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</td>
                  <td>{{ formatMoney(order.orderAmount) }}</td>
                  <td><span class="badge" :class="getOrderStatusBadge(order.orderStatus)">{{ getOrderStatusLabel(order.orderStatus, order.orderStatusLabel || '--') }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">暂无最近订单。</div>
        </section>
      </div>
    </BaseDialog>

    <BaseDialog
      v-model="confirmOpen"
      title="确认状态变更"
      :description="confirmUser ? `将用户 ${confirmUser.name || confirmUser.phone || confirmUser.id} ${confirmUser.status === 1 ? '禁用' : '恢复'}。` : ''"
      width="520px"
    >
      <p class="section-copy">确认后将更新用户状态。</p>
      <template #footer>
        <button class="button button-ghost" type="button" @click="confirmOpen = false">取消</button>
        <button class="button" :class="confirmUser?.status === 1 ? 'button-danger' : 'button-primary'" type="button" :disabled="actionLoading" @click="confirmStatusChange">
          {{ actionLoading ? '提交中...' : confirmUser?.status === 1 ? '确认禁用' : '确认恢复' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useUiStore } from '../stores/ui'
import { fetchUserDetail, fetchUsers, updateUserStatus } from '../utils/admin-api'
import { getAttendantStatusLabel, getOrderStatusBadge, getOrderStatusLabel, getUserStatusBadge, getUserStatusLabel, getUserTypeLabel, toQueryValue, userStatusOptions, userTypeOptions } from '../utils/admin-view'
import { formatDateTime, formatMoney } from '../utils/format'

const uiStore = useUiStore()

const filters = reactive({
  keyword: '',
  userType: '',
  status: ''
})

const users = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)

const detailOpen = ref(false)
const detailLoading = ref(false)
const selectedUserDetail = ref(null)
const confirmOpen = ref(false)
const confirmUser = ref(null)
const actionLoading = ref(false)

const loadUsers = async () => {
  loading.value = true
  try {
    const response = await fetchUsers({
      keyword: filters.keyword,
      userType: toQueryValue(filters.userType),
      status: toQueryValue(filters.status),
      page: page.value,
      pageSize: pageSize.value
    })
    users.value = response.content || []
    total.value = response.totalElements || 0
    totalPages.value = Math.max(response.totalPages || 1, 1)
  } catch (error) {
    uiStore.toast(error.message || '用户列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = () => {
  page.value = 0
  loadUsers()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.userType = ''
  filters.status = ''
  submitFilters()
}

const changePage = (nextPage) => {
  page.value = nextPage
  loadUsers()
}

const changePageSize = () => {
  page.value = 0
  loadUsers()
}

const openUserDetail = async (user) => {
  detailOpen.value = true
  detailLoading.value = true
  selectedUserDetail.value = null
  try {
    selectedUserDetail.value = await fetchUserDetail(user.id)
  } catch (error) {
    detailOpen.value = false
    uiStore.toast(error.message || '用户详情加载失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

const promptStatusChange = (user) => {
  confirmUser.value = user
  confirmOpen.value = true
}

const confirmStatusChange = async () => {
  if (!confirmUser.value) return
  actionLoading.value = true
  try {
    await updateUserStatus(confirmUser.value.id, confirmUser.value.status === 1 ? 0 : 1)
    uiStore.toast('用户状态更新成功', 'success')
    confirmOpen.value = false
    await loadUsers()
    if (selectedUserDetail.value?.user?.id === confirmUser.value.id) {
      selectedUserDetail.value = await fetchUserDetail(confirmUser.value.id)
    }
  } catch (error) {
    uiStore.toast(error.message || '用户状态更新失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.filter-toolbar {
  display: grid;
  grid-template-rows: auto auto;
  gap: 12px;
}

.filter-fields-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) repeat(2, minmax(170px, 1fr));
  gap: 12px;
  width: 100%;
}

.filter-fields-row .field-inline,
.filter-fields-row .filter-select {
  width: 100%;
  min-width: 0;
}

.filter-actions-row {
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 1080px) {
  .filter-fields-row {
    grid-template-columns: repeat(2, minmax(170px, 1fr));
  }
}

@media (max-width: 640px) {
  .filter-fields-row {
    grid-template-columns: 1fr;
  }
}
</style>
