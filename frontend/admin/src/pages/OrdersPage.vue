<template>
  <AppShell title="订单管理" subtitle="订单定位与完整处理">
    <div class="page-stack">
      <section class="panel-card filter-card">
        <div class="section-heading filter-heading">
          <div>
            <h3 class="section-title">订单筛选</h3>
          </div>
        </div>

        <div class="filter-board">
          <div class="filter-fields-row">
            <label class="filter-field">
              <span>关键词</span>
              <input v-model.trim="filters.keyword" class="field-inline" type="text" placeholder="订单号、用户、陪诊师、医院" @keyup.enter="submitFilters" />
            </label>
            <label class="filter-field">
              <span>订单状态</span>
              <BaseSelect v-model="filters.orderStatus" :options="orderStatusOptions" />
            </label>
            <label class="filter-field">
              <span>支付状态</span>
              <BaseSelect v-model="filters.paymentStatus" :options="paymentStatusOptions" />
            </label>
            <label class="filter-field">
              <span>开始日期</span>
              <BaseDateInput v-model="filters.startDate" placeholder="开始日期" />
            </label>
            <label class="filter-field">
              <span>结束日期</span>
              <BaseDateInput v-model="filters.endDate" placeholder="结束日期" />
            </label>
          </div>
          <div class="filter-actions-row">
            <button class="button button-primary" type="button" :disabled="loading" @click="submitFilters">查询</button>
            <button class="button button-ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">订单列表</h3>
            <p class="section-copy">点击列表行或查看记录打开处理抽屉。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="orders.length" class="record-list order-record-list">
            <article
              v-for="order in orders"
              :key="order.orderId"
              class="record-item order-record-item"
              :class="{ 'selected-row': selectedOrderId === order.orderId }"
              @click="openOrderDrawer(order.orderId)"
            >
              <div class="record-main">
                <div class="record-summary">
                  <div class="record-summary-copy">
                    <p class="record-title">{{ order.orderNo }}</p>
                    <p class="record-copy">创建于 {{ formatDateTime(order.createTime) }}</p>
                  </div>
                  <div class="record-chip-row status-badge-group">
                    <span class="status-badge">
                      <span class="status-badge-label">订单</span>
                      <span class="badge" :class="getOrderStatusBadge(order.orderStatus)">{{ getOrderStatusLabel(order.orderStatus, order.orderStatusLabel || '--') }}</span>
                    </span>
                    <span class="status-badge">
                      <span class="status-badge-label">支付</span>
                      <span class="badge" :class="getPaymentStatusBadge(order.paymentStatus)">{{ getPaymentStatusLabel(order.paymentStatus, order.paymentStatusLabel || '--') }}</span>
                    </span>
                  </div>
                </div>

                <div class="record-meta-grid order-meta-grid">
                  <div class="record-stat">
                    <p class="record-label">患者与联系人</p>
                    <p class="record-value">{{ getPatientName(order) }} / {{ formatPatientProfile(order) }}</p>
                    <p class="record-note">{{ getContactName(order) }} / {{ getContactPhone(order) }}</p>
                  </div>
                  <div class="record-stat">
                    <p class="record-label">陪诊师</p>
                    <p class="record-value">{{ order.attendantName || '暂未接单' }}</p>
                    <p class="record-note">{{ order.attendantPhone || '-' }}</p>
                  </div>
                  <div class="record-stat">
                    <p class="record-label">服务安排</p>
                    <p class="record-value">{{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</p>
                    <p class="record-note">{{ order.hospital || '-' }}</p>
                  </div>
                  <div class="record-stat">
                    <p class="record-label">金额</p>
                    <p class="record-value">{{ formatMoney(order.orderAmount) }}</p>
                    <p class="record-note">支付：{{ formatDateTime(getPaymentTime(order)) }}</p>
                  </div>
                </div>
              </div>

              <div class="record-side" @click.stop>
                <button class="button button-secondary" type="button" @click="openOrderDrawer(order.orderId)">查看记录</button>
              </div>
            </article>
          </div>
          <div v-else class="empty-card">未查询到符合条件的订单。</div>
        </template>

        <div class="pagination">
          <p class="pagination-copy">共 {{ total }} 条，当前第 {{ page + 1 }} / {{ totalPages }} 页</p>
          <div class="pagination-controls">
            <BaseSelect v-model="pageSize" :options="pageSizeOptions" @change="changePageSize" />
            <button class="button button-ghost" type="button" :disabled="page === 0 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="button button-ghost" type="button" :disabled="page + 1 >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>

      <BaseDrawer
        v-model="detailDrawerOpen"
        title="订单详情"
        :description="currentOrder ? `${currentOrder.orderNo || '未命名订单'} · ${currentOrder.hospital || '未填写医院'}` : '完整订单资料与处理操作'"
        width="940px"
      >
        <div class="section-heading">
          <div>
            <h3 class="section-title">完整记录</h3>
          </div>
          <div v-if="currentOrder" class="toolbar-group">
            <span class="status-badge">
              <span class="status-badge-label">订单</span>
              <span class="badge" :class="getOrderStatusBadge(currentOrder.orderStatus)">{{ getOrderStatusLabel(currentOrder.orderStatus, currentOrder.orderStatusLabel || '--') }}</span>
            </span>
            <span class="status-badge">
              <span class="status-badge-label">支付</span>
              <span class="badge" :class="getPaymentStatusBadge(currentOrder.paymentStatus)">{{ getPaymentStatusLabel(currentOrder.paymentStatus, currentOrder.paymentStatusLabel || '--') }}</span>
            </span>
            <button v-if="canCancel(currentOrder.orderStatus)" class="button button-danger" type="button" @click="openCancelDialog(currentOrder)">取消订单</button>
            <button v-if="currentOrder.orderStatus === 5" class="button button-primary" type="button" @click="openDisputeWorkbench(currentOrder.orderId)">进入工作台处理</button>
            <button v-if="currentOrder.orderStatus === 10" class="button button-primary" type="button" @click="openRefundDialog(currentOrder)">确认已退款</button>
            <button class="button button-ghost" type="button" @click="openStandaloneDetail">打开独立详情</button>
          </div>
        </div>

        <div v-if="detailLoading" class="skeleton"></div>
        <template v-else-if="selectedOrderDetail">
          <div class="page-stack">
            <section class="detail-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">订单基础信息</h4>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <p class="kv-label">订单号</p>
                  <p class="kv-value">{{ currentOrder.orderNo || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">创建时间</p>
                  <p class="kv-value">{{ formatDateTime(currentOrder.createTime) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">支付时间</p>
                  <p class="kv-value">{{ formatDateTime(getPaymentTime(currentOrder)) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">订单状态</p>
                  <p class="kv-value">{{ getOrderStatusLabel(currentOrder.orderStatus, currentOrder.orderStatusLabel || '--') }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">支付状态</p>
                  <p class="kv-value">{{ getPaymentStatusLabel(currentOrder.paymentStatus, currentOrder.paymentStatusLabel || '--') }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">服务医院</p>
                  <p class="kv-value">{{ currentOrder.hospital || '-' }}</p>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">患者与联系人</h4>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <p class="kv-label">患者</p>
                  <p class="kv-value">{{ getPatientName(currentOrder) }} / {{ formatPatientProfile(currentOrder) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">联系人</p>
                  <p class="kv-value">{{ getContactName(currentOrder) }} / {{ getContactPhone(currentOrder) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">下单用户</p>
                  <p class="kv-value">{{ selectedOrderDetail.user?.name || currentOrder.userName || '-' }} / {{ selectedOrderDetail.user?.phone || currentOrder.userPhone || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">陪诊师</p>
                  <p class="kv-value">{{ selectedOrderDetail.attendant?.name || currentOrder.attendantName || '暂未接单' }} / {{ selectedOrderDetail.attendant?.phone || currentOrder.attendantPhone || '-' }}</p>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">服务信息</h4>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <p class="kv-label">服务内容</p>
                  <p class="kv-value">{{ getServiceContent(currentOrder) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">服务时间</p>
                  <p class="kv-value">{{ currentOrder.serviceDate || '-' }} {{ currentOrder.serviceTimeSlot || '' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">特殊需求</p>
                  <p class="kv-value">{{ getSpecialRequirement(currentOrder) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">接单时间</p>
                  <p class="kv-value">{{ formatDateTime(getAcceptTime(currentOrder)) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">服务开始 / 结束</p>
                  <p class="kv-value">{{ formatDateTime(getServiceStartTime(currentOrder)) }} / {{ formatDateTime(getServiceEndTime(currentOrder)) }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">实际时长</p>
                  <p class="kv-value">{{ formatDurationHour(getActualDuration(currentOrder)) }}</p>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">金额结算</h4>
              </div>
              <div class="amount-breakdown-grid">
                <div class="amount-breakdown-item">
                  <p class="amount-breakdown-label">订单金额</p>
                  <p class="amount-breakdown-value">{{ formatMoney(currentOrder.orderAmount) }}</p>
                </div>
                <div class="amount-breakdown-item">
                  <p class="amount-breakdown-label">最终金额</p>
                  <p class="amount-breakdown-value">{{ formatMoney(getFinalAmount(currentOrder)) }}</p>
                </div>
                <div class="amount-breakdown-item">
                  <p class="amount-breakdown-label">差额金额</p>
                  <p class="amount-breakdown-value">{{ formatMoney(getBalanceAmount(currentOrder)) }}</p>
                </div>
                <div class="amount-breakdown-item">
                  <p class="amount-breakdown-label">退款金额</p>
                  <p class="amount-breakdown-value">{{ formatMoney(getRefundAmount(currentOrder)) }}</p>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">后台处理记录</h4>
              </div>
              <div class="detail-row">
                <div class="detail-row-label">争议说明</div>
                <div class="detail-row-value">{{ getDisputeReason(currentOrder) }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-row-label">陪诊师说明</div>
                <div class="detail-row-value">{{ currentOrder.attendantTimeRemark || '-' }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-row-label">取消原因</div>
                <div class="detail-row-value">{{ currentOrder.cancelReason || '-' }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-row-label">取消时间</div>
                <div class="detail-row-value">{{ formatDateTime(currentOrder.cancelTime) }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-row-label">后台备注</div>
                <div class="detail-row-value">{{ getAdminRemark(currentOrder) }}</div>
              </div>
              <div v-if="selectedOrderDetail.disputeResolverName" class="detail-row">
                <div class="detail-row-label">争议处理人</div>
                <div class="detail-row-value">{{ selectedOrderDetail.disputeResolverName }} / {{ mapAdminRole(selectedOrderDetail.disputeResolverRole) }} / {{ selectedOrderDetail.disputeResolverPhoneMasked || '-' }}</div>
              </div>
            </section>
          </div>
        </template>
        <div v-else class="empty-card">当前页没有可展示的订单详情。</div>
      </BaseDrawer>
    </div>

    <BaseDialog v-model="cancelDialogOpen" title="取消订单" description="填写取消原因、退款金额与处理备注。" width="620px">
      <div class="page-stack">
        <div class="kv-grid" v-if="currentOrder">
          <div class="kv-item">
            <p class="kv-label">订单号</p>
            <p class="kv-value">{{ currentOrder.orderNo }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前订单金额</p>
            <p class="kv-value">{{ formatMoney(currentOrder.orderAmount) }}</p>
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

    <BaseDialog v-model="disputeDialogOpen" title="处理争议订单" description="补差额进入用户支付，退差价进入待平台退款，零差额才直接完成。" width="620px">
      <div class="page-stack">
        <div class="kv-grid" v-if="currentOrder">
          <div class="kv-item">
            <p class="kv-label">订单号</p>
            <p class="kv-value">{{ currentOrder.orderNo }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前金额</p>
            <p class="kv-value">{{ formatMoney(currentOrder.orderAmount) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">陪诊师提交时长</p>
            <p class="kv-value">{{ formatDurationHour(getActualDuration(currentOrder)) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">用户认可时长</p>
            <p class="kv-value">{{ formatDurationHour(currentOrder.timeDisputeUserDuration) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">争议原因</p>
            <p class="kv-value">{{ getDisputeReason(currentOrder) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">裁定后差额</p>
            <p class="kv-value">{{ formatMoney(disputeBalancePreview) }}</p>
          </div>
        </div>
        <label class="login-field">
          <span>最终服务时长（小时）</span>
          <input v-model.trim="disputeForm.finalDuration" class="field" type="number" min="0" step="0.5" placeholder="例如 3.5" @input="recalculateDisputeAmount" />
        </label>
        <label class="login-field">
          <span>最终订单金额</span>
          <input v-model.trim="disputeForm.finalOrderAmount" class="field" type="number" min="0.01" step="0.01" placeholder="例如 268.00" />
        </label>
        <label class="login-field">
          <span>后台备注</span>
          <textarea v-model.trim="disputeForm.adminRemark" class="filter-textarea" placeholder="请输入处理依据和结果"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="disputeDialogOpen = false">取消</button>
        <button class="button button-primary" type="button" :disabled="actionLoading" @click="submitDispute">
          {{ actionLoading ? '提交中...' : '确认争议处理' }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="refundDialogOpen" title="确认退款完成" description="仅在平台已完成原路退款或线下退款后确认，确认后订单才会完成。" width="560px">
      <div class="page-stack">
        <div class="kv-grid" v-if="currentOrder">
          <div class="kv-item">
            <p class="kv-label">订单号</p>
            <p class="kv-value">{{ currentOrder.orderNo }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">退款金额</p>
            <p class="kv-value">{{ formatMoney(currentOrder.refundAmount) }}</p>
          </div>
        </div>
        <label class="login-field">
          <span>退款备注</span>
          <textarea v-model.trim="refundForm.adminRemark" class="filter-textarea" placeholder="例如：已完成原路退款"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="refundDialogOpen = false">取消</button>
        <button class="button button-primary" type="button" :disabled="actionLoading" @click="submitRefundComplete">
          {{ actionLoading ? '提交中...' : '确认退款完成' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import BaseDateInput from '../components/BaseDateInput.vue'
import BaseDrawer from '../components/BaseDrawer.vue'
import BaseSelect from '../components/BaseSelect.vue'
import { useUiStore } from '../stores/ui'
import { cancelOrder, completeOrderRefund, fetchOrderDetail, fetchOrders, resolveDispute } from '../utils/admin-api'
import { getOrderStatusBadge, getOrderStatusLabel, getPaymentStatusBadge, getPaymentStatusLabel, orderStatusOptions, paymentStatusOptions, toQueryValue } from '../utils/admin-view'
import { suggestDisputeFinalAmount } from '../utils/dispute-settlement'
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
const detailLoading = ref(false)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)
const pageSizeOptions = [
  { label: '10 条 / 页', value: 10 },
  { label: '20 条 / 页', value: 20 },
  { label: '50 条 / 页', value: 50 }
]
const selectedOrderId = ref(null)
const selectedOrderDetail = ref(null)
const detailDrawerOpen = ref(false)

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

const refundDialogOpen = ref(false)
const refundForm = reactive({
  adminRemark: '已完成原路退款'
})

const ignoreNextQueryWatch = ref(false)

const firstValidValue = (payload, keys) => {
  for (const key of keys) {
    const value = payload?.[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

const normalizeSex = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return '未知'
  const lower = raw.toLowerCase()
  if (['1', 'male', '男', 'man'].includes(lower)) return '男'
  if (['2', 'female', '女', 'woman'].includes(lower)) return '女'
  return raw
}

const getPatientName = (order) => firstValidValue(order, ['patientName', 'userName', 'name']) || '-'
const getPatientSex = (order) => normalizeSex(firstValidValue(order, ['patientSex', 'sex', 'gender']))
const getPatientAge = (order) => firstValidValue(order, ['patientAge', 'age', 'userAge']) || '-'
const formatPatientProfile = (order) => `${getPatientSex(order)} / ${getPatientAge(order)}`
const getContactName = (order) => firstValidValue(order, ['contactPerson', 'contactName', 'emergencyContact', 'userName']) || '-'
const getContactPhone = (order) => firstValidValue(order, ['contactPhone', 'contactMobile', 'userPhone', 'phone']) || '-'
const getServiceContent = (order) => firstValidValue(order, ['serviceContent', 'serviceTypeName', 'serviceType', 'serviceProject']) || '-'
const getSpecialRequirement = (order) => firstValidValue(order, ['specialRequirements', 'customRequirement', 'remark', 'note']) || '-'
const getPaymentTime = (order) => firstValidValue(order, ['paymentTime', 'payTime', 'paidTime'])
const getAcceptTime = (order) => firstValidValue(order, ['acceptTime', 'takeOrderTime', 'receiveOrderTime'])
const getServiceStartTime = (order) => firstValidValue(order, ['serviceStartTime', 'startTime'])
const getServiceEndTime = (order) => firstValidValue(order, ['serviceEndTime', 'endTime'])
const getActualDuration = (order) => firstValidValue(order, ['actualDuration', 'serviceDuration', 'finalDuration', 'timeDisputeFinalDuration', 'timeDisputeUserDuration'])
const getFinalAmount = (order) => firstValidValue(order, ['finalOrderAmount', 'settlementAmount', 'actualPayAmount', 'orderAmount'])
const getBalanceAmount = (order) => firstValidValue(order, ['balanceAmount', 'differenceAmount'])
const getRefundAmount = (order) => firstValidValue(order, ['refundAmount', 'refundFee'])
const getAdminRemark = (order) => firstValidValue(order, ['adminRemark', 'remark', 'backendRemark']) || '-'
const getDisputeReason = (order) => firstValidValue(order, ['timeDisputeReason', 'disputeReason', 'disputeRemark']) || '-'

const currentOrder = computed(() => selectedOrderDetail.value?.order || orders.value.find((item) => item.orderId === selectedOrderId.value) || null)
const disputeBalancePreview = computed(() => {
  const finalAmount = Number(disputeForm.finalOrderAmount)
  const currentAmount = Number(currentOrder.value?.orderAmount || 0)
  if (!Number.isFinite(finalAmount)) return ''
  return finalAmount - currentAmount
})

const formatDurationHour = (value) => {
  if (value === '' || value === null || value === undefined) return '-'
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) return `${value}`
  return `${numericValue} 小时`
}

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

const parseSelectedId = (value) => {
  const raw = readQueryValue(value)
  if (raw === '' || raw === undefined || raw === null) return null
  const numericValue = Number(raw)
  return Number.isFinite(numericValue) ? numericValue : null
}

const applyQueryFilters = (query) => {
  filters.keyword = typeof readQueryValue(query.keyword) === 'string' ? readQueryValue(query.keyword) : ''
  filters.orderStatus = toFilterValue(query.orderStatus)
  filters.paymentStatus = toFilterValue(query.paymentStatus)
  filters.startDate = typeof readQueryValue(query.startDate) === 'string' ? readQueryValue(query.startDate) : ''
  filters.endDate = typeof readQueryValue(query.endDate) === 'string' ? readQueryValue(query.endDate) : ''
  selectedOrderId.value = parseSelectedId(query.selectedId)
  detailDrawerOpen.value = selectedOrderId.value !== null

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

const buildQuery = (overrides = {}) => {
  const query = {
    page: String(page.value),
    pageSize: String(pageSize.value)
  }
  if (filters.keyword) query.keyword = filters.keyword
  if (filters.orderStatus !== '') query.orderStatus = String(filters.orderStatus)
  if (filters.paymentStatus !== '') query.paymentStatus = String(filters.paymentStatus)
  if (filters.startDate) query.startDate = filters.startDate
  if (filters.endDate) query.endDate = filters.endDate
  if (selectedOrderId.value !== null && selectedOrderId.value !== undefined) query.selectedId = String(selectedOrderId.value)

  Object.entries(overrides).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') {
      delete query[key]
    } else {
      query[key] = String(value)
    }
  })
  return query
}

const isSameQuery = (nextQuery) => {
  const current = route.query
  const currentKeys = Object.keys(current)
  const nextKeys = Object.keys(nextQuery)
  if (currentKeys.length !== nextKeys.length) return false
  return nextKeys.every((key) => String(readQueryValue(current[key]) ?? '') === String(nextQuery[key] ?? ''))
}

const syncQuery = async (overrides = {}) => {
  const nextQuery = buildQuery(overrides)
  if (isSameQuery(nextQuery)) return false
  ignoreNextQueryWatch.value = true
  await router.replace({ query: nextQuery })
  return true
}

const canCancel = (status) => ![6, 7, 10].includes(Number(status))

const loadSelectedOrderDetail = async (orderId) => {
  if (!orderId) {
    selectedOrderDetail.value = null
    return
  }
  detailLoading.value = true
  try {
    selectedOrderDetail.value = await fetchOrderDetail(orderId)
  } catch (error) {
    selectedOrderDetail.value = null
    uiStore.toast(error.message || '订单详情加载失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

const resolveSelectedOrderId = () => {
  if (!orders.value.length) return null
  if (selectedOrderId.value !== null && orders.value.some((item) => item.orderId === selectedOrderId.value)) return selectedOrderId.value
  return orders.value[0].orderId
}

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

    const nextSelectedId = resolveSelectedOrderId()
    const selectionChanged = nextSelectedId !== selectedOrderId.value
    selectedOrderId.value = nextSelectedId
    if (selectionChanged) {
      await syncQuery({ selectedId: nextSelectedId })
    }
    await loadSelectedOrderDetail(selectedOrderId.value)
  } catch (error) {
    orders.value = []
    selectedOrderId.value = null
    selectedOrderDetail.value = null
    total.value = 0
    totalPages.value = 1
    uiStore.toast(error.message || '订单列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = async () => {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    uiStore.toast('开始日期不能晚于结束日期', 'error')
    return
  }
  page.value = 0
  selectedOrderId.value = null
  await syncQuery({ page: 0, selectedId: null })
  await loadOrders()
}

const resetFilters = async () => {
  filters.keyword = ''
  filters.orderStatus = ''
  filters.paymentStatus = ''
  filters.startDate = ''
  filters.endDate = ''
  page.value = 0
  selectedOrderId.value = null
  await syncQuery({
    keyword: '',
    orderStatus: '',
    paymentStatus: '',
    startDate: '',
    endDate: '',
    page: 0,
    selectedId: null
  })
  await loadOrders()
}

const changePage = async (nextPage) => {
  page.value = nextPage
  selectedOrderId.value = null
  await syncQuery({ page: nextPage, selectedId: null })
  await loadOrders()
}

const changePageSize = async () => {
  page.value = 0
  selectedOrderId.value = null
  await syncQuery({ page: 0, pageSize: pageSize.value, selectedId: null })
  await loadOrders()
}

const openOrderDrawer = async (orderId) => {
  if (!orderId) return
  detailDrawerOpen.value = true
  selectedOrderId.value = orderId
  const changed = await syncQuery({ selectedId: orderId })
  if (!changed || selectedOrderDetail.value?.order?.orderId !== orderId) {
    await loadSelectedOrderDetail(orderId)
  }
}

const openStandaloneDetail = () => {
  if (!selectedOrderId.value) return
  router.push(`/orders/${selectedOrderId.value}`)
}

const openDisputeWorkbench = (orderId) => {
  if (!orderId) return
  router.push({ name: 'workbench', query: { type: 'ORDER_DISPUTE', targetId: String(orderId) } })
}

const openCancelDialog = (order) => {
  selectedOrderId.value = order.orderId
  cancelForm.reason = ''
  cancelForm.refundAmount = order.orderAmount ? String(order.orderAmount) : ''
  cancelForm.adminRemark = ''
  cancelDialogOpen.value = true
}

const openDisputeDialog = (order) => {
  selectedOrderId.value = order.orderId
  disputeForm.finalDuration = order.timeDisputeUserDuration ? String(order.timeDisputeUserDuration) : (getActualDuration(order) ? String(getActualDuration(order)) : '')
  disputeForm.finalOrderAmount = getSuggestedFinalAmount(order, disputeForm.finalDuration)
  disputeForm.adminRemark = ''
  disputeDialogOpen.value = true
}

const openRefundDialog = (order) => {
  selectedOrderId.value = order.orderId
  refundForm.adminRemark = '已完成原路退款'
  refundDialogOpen.value = true
}

const recalculateDisputeAmount = () => {
  if (!currentOrder.value || !disputeForm.finalDuration) return
  disputeForm.finalOrderAmount = getSuggestedFinalAmount(currentOrder.value, disputeForm.finalDuration)
}

const refreshAfterAction = async () => {
  await loadOrders()
}

const submitCancel = async () => {
  if (!currentOrder.value) return
  if (!cancelForm.reason) {
    uiStore.toast('请填写取消原因', 'error')
    return
  }

  actionLoading.value = true
  try {
    await cancelOrder(currentOrder.value.orderId, {
      reason: cancelForm.reason,
      refundAmount: cancelForm.refundAmount ? Number(cancelForm.refundAmount) : undefined,
      adminRemark: cancelForm.adminRemark
    })
    uiStore.toast('订单已取消', 'success')
    cancelDialogOpen.value = false
    await refreshAfterAction()
  } catch (error) {
    uiStore.toast(error.message || '取消订单失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const submitDispute = async () => {
  if (!currentOrder.value) return
  if (!disputeForm.finalDuration || !disputeForm.finalOrderAmount) {
    uiStore.toast('请填写最终时长和最终金额', 'error')
    return
  }
  const finalAmount = Number(disputeForm.finalOrderAmount)
  if (!Number.isFinite(finalAmount) || finalAmount <= 0) {
    uiStore.toast('最终金额必须大于0', 'error')
    return
  }
  if (!disputeForm.adminRemark) {
    uiStore.toast('请填写争议处理备注', 'error')
    return
  }

  actionLoading.value = true
  try {
    await resolveDispute(currentOrder.value.orderId, {
      finalDuration: Number(disputeForm.finalDuration),
      finalOrderAmount: finalAmount,
      adminRemark: disputeForm.adminRemark
    })
    uiStore.toast('争议订单处理成功', 'success')
    disputeDialogOpen.value = false
    await refreshAfterAction()
  } catch (error) {
    uiStore.toast(error.message || '争议处理失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const submitRefundComplete = async () => {
  if (!currentOrder.value) return
  if (!refundForm.adminRemark) {
    uiStore.toast('请填写退款备注', 'error')
    return
  }
  actionLoading.value = true
  try {
    await completeOrderRefund(currentOrder.value.orderId, {
      adminRemark: refundForm.adminRemark
    })
    uiStore.toast('退款已确认，订单已完成', 'success')
    refundDialogOpen.value = false
    await refreshAfterAction()
  } catch (error) {
    uiStore.toast(error.message || '确认退款失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const getSuggestedFinalAmount = (order, duration = getActualDuration(order)) => suggestDisputeFinalAmount(order, duration)

const mapAdminRole = (role) => (role === 'SUPER_ADMIN' ? '超级管理员' : role === 'ADMIN' ? '管理员' : '-')

watch(
  () => route.query,
  async (query) => {
    if (ignoreNextQueryWatch.value) {
      ignoreNextQueryWatch.value = false
      return
    }
    applyQueryFilters(query)
    page.value = toFilterValue(query.page) || 0
    pageSize.value = toFilterValue(query.pageSize) || 10
    await loadOrders()
  },
  { immediate: true }
)
</script>

<style scoped>
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

.selected-row {
  border-color: rgba(42, 120, 255, 0.3);
  background: linear-gradient(180deg, rgba(237, 244, 255, 0.98) 0%, rgba(248, 251, 255, 0.98) 100%);
  box-shadow: 0 12px 28px rgba(42, 120, 255, 0.1);
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 16px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.96);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease, background-color 180ms ease;
}

.record-item:hover {
  border-color: rgba(42, 120, 255, 0.22);
  box-shadow: 0 10px 24px rgba(33, 71, 126, 0.08);
  transform: translateY(-1px);
}

.record-main,
.record-summary,
.record-summary-copy,
.record-meta-grid,
.record-stat {
  min-width: 0;
}

.record-main {
  display: grid;
  gap: 14px;
}

.record-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.record-title,
.record-value {
  margin: 0;
  font-weight: 700;
  line-height: 1.45;
  word-break: break-word;
}

.record-copy,
.record-label,
.record-note {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.5;
  word-break: break-word;
}

.record-copy + .record-copy,
.record-label + .record-value,
.record-value + .record-note {
  margin-top: 4px;
}

.record-label {
  font-size: 12px;
}

.record-note {
  font-size: 13px;
}

.record-chip-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.record-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.order-meta-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.record-stat {
  padding: 12px 14px;
  border: 1px solid rgba(216, 228, 242, 0.92);
  border-radius: 16px;
  background: var(--surface-soft);
}

.record-side {
  display: grid;
  align-content: center;
  justify-items: stretch;
  gap: 10px;
  min-width: 112px;
}

.detail-section {
  display: grid;
  gap: 12px;
}

.compact-heading {
  margin-bottom: 0;
}

.detail-row-value {
  text-align: left;
}

@media (max-width: 1320px) {
  .order-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-fields-row {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
  }
}

@media (max-width: 820px) {
  .record-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .filter-fields-row {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 620px) {
  .record-summary {
    flex-direction: column;
    align-items: stretch;
  }

  .record-chip-row {
    justify-content: flex-start;
  }

  .order-meta-grid,
  .filter-fields-row {
    grid-template-columns: 1fr;
  }

  .record-side {
    min-width: 0;
  }
}
</style>
