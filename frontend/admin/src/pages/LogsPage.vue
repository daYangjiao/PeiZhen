<template>
  <AppShell title="操作日志" subtitle="后台处理记录">
    <div class="page-stack">
      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">筛选条件</h3>
          </div>
          <button class="button button-secondary" type="button" :disabled="loading" @click="loadLogs">刷新</button>
        </div>

        <div class="toolbar log-filter-grid">
          <input v-model.trim="filters.keyword" class="field-inline" type="text" placeholder="管理员、对象、备注" @keyup.enter="submitFilters" />
          <select v-model="filters.module" class="filter-select">
            <option value="">全部模块</option>
            <option value="USER">用户管理</option>
            <option value="ATTENDANT">陪诊师审核</option>
            <option value="ORDER">订单管理</option>
            <option value="ADMIN_ACCOUNT">管理员账号</option>
          </select>
          <select v-model="filters.operatorRole" class="filter-select">
            <option value="">全部账号类型</option>
            <option value="SUPER_ADMIN">超级管理员</option>
            <option value="ADMIN">管理员</option>
          </select>
          <input v-model="filters.startTime" class="filter-input" type="datetime-local" />
          <input v-model="filters.endTime" class="filter-input" type="datetime-local" />
          <button class="button button-primary" type="button" :disabled="loading" @click="submitFilters">查询</button>
          <button class="button button-ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
        </div>
      </section>

      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">日志列表</h3>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <div v-else-if="logs.length" class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>时间</th>
                <th>处理人</th>
                <th>模块</th>
                <th>动作</th>
                <th>对象</th>
                <th>状态变化</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id">
                <td class="time-cell">{{ formatDateTime(log.createTime) }}</td>
                <td>
                  <p class="table-cell-title">{{ log.operatorName || '-' }}</p>
                  <p class="table-cell-copy">{{ mapRole(log.operatorRole) }} · {{ log.operatorPhoneMasked || '-' }}</p>
                </td>
                <td><span class="badge badge-blue">{{ mapModule(log.module) }}</span></td>
                <td>{{ mapAction(log.action) }}</td>
                <td>
                  <p class="table-cell-title">{{ log.targetLabel || '-' }}</p>
                  <p class="table-cell-copy">{{ log.targetType || '-' }} #{{ log.targetId || '-' }}</p>
                </td>
                <td>{{ formatStatusChange(log) }}</td>
                <td>{{ log.remark || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="empty-card">暂无操作日志</div>

        <div class="pagination">
          <p class="pagination-copy">共 {{ total }} 条，当前第 {{ page + 1 }} / {{ totalPages }} 页</p>
          <div class="pagination-controls">
            <select v-model.number="pageSize" class="filter-select" @change="changePageSize">
              <option :value="10">10 条 / 页</option>
              <option :value="20">20 条 / 页</option>
              <option :value="50">50 条 / 页</option>
              <option :value="100">100 条 / 页</option>
            </select>
            <button class="button button-ghost" type="button" :disabled="page === 0 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="button button-ghost" type="button" :disabled="page + 1 >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import { useUiStore } from '../stores/ui'
import { fetchOperationLogs } from '../utils/admin-api'
import { formatDateTime } from '../utils/format'

const uiStore = useUiStore()

const filters = reactive({
  keyword: '',
  module: '',
  operatorRole: '',
  startTime: '',
  endTime: ''
})

const logs = ref([])
const loading = ref(false)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)

const normalizeDateTime = (value) => (value ? value.replace('T', ' ') + ':00' : '')

const loadLogs = async () => {
  loading.value = true
  try {
    const response = await fetchOperationLogs({
      keyword: filters.keyword,
      module: filters.module,
      operatorRole: filters.operatorRole,
      startTime: normalizeDateTime(filters.startTime),
      endTime: normalizeDateTime(filters.endTime),
      page: page.value,
      pageSize: pageSize.value
    })
    logs.value = response.content || []
    total.value = response.totalElements || 0
    totalPages.value = Math.max(response.totalPages || 1, 1)
  } catch (error) {
    uiStore.toast(error.message || '操作日志加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = () => {
  page.value = 0
  loadLogs()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.module = ''
  filters.operatorRole = ''
  filters.startTime = ''
  filters.endTime = ''
  page.value = 0
  loadLogs()
}

const changePage = (nextPage) => {
  page.value = nextPage
  loadLogs()
}

const changePageSize = () => {
  page.value = 0
  loadLogs()
}

const mapRole = (role) => (role === 'SUPER_ADMIN' ? '超级管理员' : role === 'ADMIN' ? '管理员' : '-')
const mapModule = (module) => ({
  USER: '用户管理',
  ATTENDANT: '陪诊师审核',
  ORDER: '订单管理',
  ADMIN_ACCOUNT: '管理员账号'
})[module] || module || '-'
const mapAction = (action) => ({
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

const formatStatusChange = (log) => {
  const fromStatus = log.fromStatus ?? '-'
  const toStatus = log.toStatus ?? '-'
  if (fromStatus === '-' && toStatus === '-') return '-'
  return `${fromStatus} -> ${toStatus}`
}

onMounted(loadLogs)
</script>

<style scoped>
.log-filter-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) repeat(2, minmax(140px, 0.8fr)) repeat(2, minmax(190px, 1fr)) auto auto;
  gap: 10px;
  align-items: center;
}

.log-filter-grid > * {
  min-width: 0;
}

@media (max-width: 1200px) {
  .log-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
