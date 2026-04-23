<template>
  <AppShell title="订单管理" subtitle="订单详情、取消与争议处理">
    <div class="page-stack">
      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">筛选条件</h3>
            <p class="section-copy">按订单状态、支付状态、时间区间检索订单。</p>
          </div>
        </div>

        <div class="toolbar filter-toolbar">
          <div class="toolbar-group filter-fields-row">
            <input v-model.trim="filters.keyword" class="field-inline" type="text" placeholder="订单号、用户、陪诊师、医院" @keyup.enter="submitFilters" />
            <select v-model="filters.orderStatus" class="filter-select">
              <option v-for="option in orderStatusOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
            </select>
            <select v-model="filters.paymentStatus" class="filter-select">
              <option v-for="option in paymentStatusOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
            </select>
            <input v-model="filters.startDate" class="filter-input" type="date" />
            <input v-model="filters.endDate" class="filter-input" type="date" />
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
            <h3 class="section-title">订单列表</h3>
            <p class="section-copy">查看订单详情，处理取消与争议。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="orders.length" class="table-wrap">
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
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orders" :key="order.orderId">
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
                  <td>
                    <div class="table-actions">
                      <button class="button button-secondary" type="button" @click="goToDetail(order.orderId)">查看详情</button>
                      <button
                        v-if="canCancel(order.orderStatus)"
                        class="button button-danger"
                        type="button"
                        @click="openCancelDialog(order)"
                      >
                        取消订单
                      </button>
                      <button
                        v-if="order.orderStatus === 5"
                        class="button button-primary"
                        type="button"
                        @click="openDisputeDialog(order)"
                      >
                        处理争议
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">未查询到符合条件的订单。</div>
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

    <BaseDialog v-model="cancelDialogOpen" title="取消订单" description="填写取消原因、退款金额与处理备注。" width="620px">
      <div class="page-stack">
        <div class="kv-grid" v-if="selectedOrder">
          <div class="kv-item">
            <p class="kv-label">订单号</p>
            <p class="kv-value">{{ selectedOrder.orderNo }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前订单金额</p>
            <p class="kv-value">{{ formatMoney(selectedOrder.orderAmount) }}</p>
          </div>
        </div>
        <label class="login-field">
          <span>取消原因</span>
          <textarea v-model.trim="cancelForm.reason" class="filter-textarea" placeholder="请输入取消原因"></textarea>
        </label>
        <label class="login-field">
          <span>退款金额</span>
          <input v-model.trim="cancelForm.refundAmount" class="field" type="number" min="0" step="0.01" placeholder="不填则按后台默认逻辑处理" />
        </label>
        <label class="login-field">
          <span>后台备注</span>
          <textarea v-model.trim="cancelForm.adminRemark" class="filter-textarea" placeholder="可选，记录后台处理备注"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="cancelDialogOpen = false">取消</button>
        <button class="button button-danger" type="button" :disabled="actionLoading" @click="submitCancel">
          {{ actionLoading ? '提交中...' : '确认取消订单' }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="disputeDialogOpen" title="处理争议订单" description="确认最终时长和金额后，订单会从争议状态转为已完成。" width="620px">
      <div class="page-stack">
        <div class="kv-grid" v-if="selectedOrder">
          <div class="kv-item">
            <p class="kv-label">订单号</p>
            <p class="kv-value">{{ selectedOrder.orderNo }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前金额</p>
            <p class="kv-value">{{ formatMoney(selectedOrder.orderAmount) }}</p>
          </div>
        </div>
        <label class="login-field">
          <span>最终服务时长（小时）</span>
          <input v-model.trim="disputeForm.finalDuration" class="field" type="number" min="0" step="0.5" placeholder="例如 3.5" />
        </label>
        <label class="login-field">
          <span>最终订单金额</span>
          <input v-model.trim="disputeForm.finalOrderAmount" class="field" type="number" min="0" step="0.01" placeholder="例如 268.00" />
        </label>
        <label class="login-field">
          <span>后台备注</span>
          <textarea v-model.trim="disputeForm.adminRemark" class="filter-textarea" placeholder="请输入争议处理备注"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="disputeDialogOpen = false">取消</button>
        <button class="button button-primary" type="button" :disabled="actionLoading" @click="submitDispute">
          {{ actionLoading ? '提交中...' : '确认争议处理' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useUiStore } from '../stores/ui'
import { cancelOrder, fetchOrders, resolveDispute } from '../utils/admin-api'
import { getOrderStatusBadge, getOrderStatusLabel, getPaymentStatusBadge, getPaymentStatusLabel, orderStatusOptions, paymentStatusOptions, toQueryValue } from '../utils/admin-view'
import { formatDateTime, formatMoney } from '../utils/format'

const router = useRouter()
const route = useRoute()
const uiStore = useUiStore()

const filters = reactive({
  keyword: '',
  orderStatus: '',
  paymentStatus: '',
  startDate: '',
  endDate: ''
})

const orders = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)

const selectedOrder = ref(null)
const actionLoading = ref(false)

const cancelDialogOpen = ref(false)
const cancelForm = reactive({
  reason: '',
  refundAmount: '',
  adminRemark: ''
})

const disputeDialogOpen = ref(false)
const disputeForm = reactive({
  finalDuration: '',
  finalOrderAmount: '',
  adminRemark: ''
})

const readQueryValue = (queryValue) => (Array.isArray(queryValue) ? queryValue[0] : queryValue)

const toFilterValue = (queryValue) => {
  const value = readQueryValue(queryValue)
  if (value === '' || value === undefined || value === null) return ''
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : ''
}

const formatLocalDate = (date) => {
  const year = String(date.getFullYear())
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const applyQueryFilters = (query) => {
  filters.keyword = typeof readQueryValue(query.keyword) === 'string' ? readQueryValue(query.keyword) : ''
  filters.orderStatus = toFilterValue(query.orderStatus)
  filters.paymentStatus = toFilterValue(query.paymentStatus)
  filters.startDate = typeof readQueryValue(query.startDate) === 'string' ? readQueryValue(query.startDate) : ''
  filters.endDate = typeof readQueryValue(query.endDate) === 'string' ? readQueryValue(query.endDate) : ''

  const quick = readQueryValue(query.quick)
  if (quick === 'today') {
    const today = formatLocalDate(new Date())
    filters.startDate = today
    filters.endDate = today
  }
  if (quick === 'dispute' && filters.orderStatus === '') {
    filters.orderStatus = 5
  }
}

const canCancel = (status) => status !== 6 && status !== 7

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await fetchOrders({
      keyword: filters.keyword,
      orderStatus: toQueryValue(filters.orderStatus),
      paymentStatus: toQueryValue(filters.paymentStatus),
      startDate: filters.startDate,
      endDate: filters.endDate,
      page: page.value,
      pageSize: pageSize.value
    })
    orders.value = response.content || []
    total.value = response.totalElements || 0
    totalPages.value = Math.max(response.totalPages || 1, 1)
  } catch (error) {
    uiStore.toast(error.message || '订单列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = () => {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    uiStore.toast('开始日期不能晚于结束日期', 'error')
    return
  }
  page.value = 0
  loadOrders()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.orderStatus = ''
  filters.paymentStatus = ''
  filters.startDate = ''
  filters.endDate = ''
  submitFilters()
}

const changePage = (nextPage) => {
  page.value = nextPage
  loadOrders()
}

const changePageSize = () => {
  page.value = 0
  loadOrders()
}

const goToDetail = (id) => {
  router.push(`/orders/${id}`)
}

const openCancelDialog = (order) => {
  selectedOrder.value = order
  cancelForm.reason = ''
  cancelForm.refundAmount = order.orderAmount ? String(order.orderAmount) : ''
  cancelForm.adminRemark = ''
  cancelDialogOpen.value = true
}

const openDisputeDialog = (order) => {
  selectedOrder.value = order
  disputeForm.finalDuration = ''
  disputeForm.finalOrderAmount = order.orderAmount ? String(order.orderAmount) : ''
  disputeForm.adminRemark = ''
  disputeDialogOpen.value = true
}

const submitCancel = async () => {
  if (!selectedOrder.value) return
  if (!cancelForm.reason) {
    uiStore.toast('请填写取消原因', 'error')
    return
  }

  actionLoading.value = true
  try {
    await cancelOrder(selectedOrder.value.orderId, {
      reason: cancelForm.reason,
      refundAmount: cancelForm.refundAmount ? Number(cancelForm.refundAmount) : undefined,
      adminRemark: cancelForm.adminRemark
    })
    uiStore.toast('订单已取消', 'success')
    cancelDialogOpen.value = false
    await loadOrders()
  } catch (error) {
    uiStore.toast(error.message || '取消订单失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const submitDispute = async () => {
  if (!selectedOrder.value) return
  if (!disputeForm.finalDuration || !disputeForm.finalOrderAmount) {
    uiStore.toast('请填写最终时长和最终金额', 'error')
    return
  }

  actionLoading.value = true
  try {
    await resolveDispute(selectedOrder.value.orderId, {
      finalDuration: Number(disputeForm.finalDuration),
      finalOrderAmount: Number(disputeForm.finalOrderAmount),
      adminRemark: disputeForm.adminRemark
    })
    uiStore.toast('争议订单处理成功', 'success')
    disputeDialogOpen.value = false
    await loadOrders()
  } catch (error) {
    uiStore.toast(error.message || '争议处理失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => route.query,
  (query) => {
    applyQueryFilters(query)
    page.value = 0
    loadOrders()
  },
  { immediate: true }
)
</script>

<style scoped>
.filter-toolbar {
  display: grid;
  grid-template-rows: auto auto;
  gap: 12px;
}

.filter-fields-row {
  display: grid;
  grid-template-columns: minmax(260px, 1.8fr) repeat(2, minmax(150px, 1fr)) repeat(2, minmax(160px, 1fr));
  gap: 12px;
  width: 100%;
}

.filter-fields-row .field-inline,
.filter-fields-row .filter-select,
.filter-fields-row .filter-input {
  width: 100%;
  min-width: 0;
}

.filter-actions-row {
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 1320px) {
  .filter-fields-row {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
  }
}

@media (max-width: 820px) {
  .filter-fields-row {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 620px) {
  .filter-fields-row {
    grid-template-columns: 1fr;
  }
}
</style>
