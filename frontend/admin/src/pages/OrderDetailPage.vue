<template>
  <AppShell title="订单详情" subtitle="订单快照、服务信息与后台处理">
    <div class="page-stack">
      <section v-if="loading" class="skeleton"></section>

      <template v-else-if="detail">
        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">{{ order.orderNo }}</h3>
              <p class="section-copy">{{ order.hospital || '-' }} · {{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</p>
            </div>
            <div class="toolbar-group">
              <span class="status-badge">
                <span class="status-badge-label">订单</span>
                <span class="badge no-wrap" :class="getOrderStatusBadge(order.orderStatus)">{{ orderStatusLabel }}</span>
              </span>
              <span class="status-badge">
                <span class="status-badge-label">支付</span>
                <span class="badge no-wrap" :class="getPaymentStatusBadge(order.paymentStatus)">{{ paymentStatusLabel }}</span>
              </span>
              <button class="button button-ghost" type="button" @click="router.push('/orders')">返回列表</button>
            </div>
          </div>

          <div class="kv-grid">
            <div class="kv-item">
              <p class="kv-label">订单编号</p>
              <p class="kv-value">{{ order.orderNo || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">创建时间</p>
              <p class="kv-value">{{ formatDateTime(order.createTime) }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">支付时间</p>
              <p class="kv-value">{{ formatDateTime(getPaymentTime(order)) }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">患者信息</p>
              <p class="kv-value">{{ getPatientName(order) }} / {{ formatSex(getPatientSex(order)) }} / {{ getPatientAge(order) }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">联系人</p>
              <p class="kv-value">{{ getContactName(order) }} / {{ getContactPhone(order) }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">服务内容</p>
              <p class="kv-value">{{ getServiceContent(order) }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">特殊需求</p>
              <p class="kv-value">{{ getSpecialRequirement(order) }}</p>
            </div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">服务进展</h3>
              <p class="section-copy">接单、服务开始与结束、时长与争议记录。</p>
            </div>
            <div class="toolbar-group">
              <button v-if="canCancel(order.orderStatus)" class="button button-danger" type="button" @click="openCancelDialog">取消订单</button>
              <button v-if="order.orderStatus === 5" class="button button-primary" type="button" @click="openDisputeWorkbench">进入工作台处理</button>
            </div>
          </div>

          <div class="detail-row">
            <div class="detail-row-label">订单状态</div>
            <div class="detail-row-value no-wrap">{{ orderStatusLabel }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">支付状态</div>
            <div class="detail-row-value no-wrap">{{ paymentStatusLabel }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">接单时间</div>
            <div class="detail-row-value">{{ formatDateTime(getAcceptTime(order)) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">服务开始 / 结束</div>
            <div class="detail-row-value">{{ formatDateTime(getServiceStartTime(order)) }} / {{ formatDateTime(getServiceEndTime(order)) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">实际服务时长</div>
            <div class="detail-row-value">{{ formatDuration(getActualDuration(order)) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">服务进度步骤</div>
            <div class="detail-row-value">{{ order.serviceProgressStep || '-' }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">争议说明</div>
            <div class="detail-row-value">{{ getDisputeReason(order) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">陪诊师说明</div>
            <div class="detail-row-value">{{ order.attendantTimeRemark || '-' }}</div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">金额结算</h3>
              <p class="section-copy">订单金额、退款、差额与最终结算。</p>
            </div>
          </div>

          <div class="amount-breakdown-grid">
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">订单金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(order.orderAmount) }}</p>
            </div>
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">最终金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(getFinalOrderAmount(order)) }}</p>
            </div>
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">差额金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(getBalanceAmount(order)) }}</p>
            </div>
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">退款金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(getRefundAmount(order)) }}</p>
            </div>
          </div>

          <div class="detail-row">
            <div class="detail-row-label">后台备注</div>
            <div class="detail-row-value">{{ getAdminRemark(order) }}</div>
          </div>
          <div v-if="detail.disputeResolverName" class="detail-row">
            <div class="detail-row-label">争议处理人</div>
            <div class="detail-row-value">{{ detail.disputeResolverName }} / {{ mapAdminRole(detail.disputeResolverRole) }} / {{ detail.disputeResolverPhoneMasked || '-' }}</div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">参与人信息</h3>
              <p class="section-copy">下单用户、联系人与陪诊师资料。</p>
            </div>
          </div>

          <div class="detail-row">
            <div class="detail-row-label">下单用户</div>
            <div class="detail-row-value">{{ detail.user?.name || order.userName || '-' }} / {{ detail.user?.phone || order.userPhone || '-' }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">联系人</div>
            <div class="detail-row-value">{{ getContactName(order) }} / {{ getContactPhone(order) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">陪诊师</div>
            <div class="detail-row-value">{{ detail.attendant?.name || order.attendantName || '暂未接单' }} / {{ detail.attendant?.phone || order.attendantPhone || '-' }}</div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">后台处理记录</h3>
              <p class="section-copy">取消、争议与备注信息。</p>
            </div>
          </div>

          <div class="detail-row">
            <div class="detail-row-label">取消原因</div>
            <div class="detail-row-value">{{ order.cancelReason || '-' }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">取消时间</div>
            <div class="detail-row-value">{{ formatDateTime(order.cancelTime) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">争议备注</div>
            <div class="detail-row-value">{{ getDisputeReason(order) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">后台备注</div>
            <div class="detail-row-value">{{ getAdminRemark(order) }}</div>
          </div>
        </section>
      </template>

      <section v-else class="empty-card">未找到该订单信息。</section>
    </div>

    <BaseDialog v-model="cancelDialogOpen" title="取消订单" description="订单详情页可直接执行取消。" width="620px">
      <div class="page-stack">
        <label class="login-field">
          <span>取消原因</span>
          <textarea v-model.trim="cancelForm.reason" class="filter-textarea" placeholder="请输入取消原因"></textarea>
        </label>
        <label class="login-field">
          <span>退款金额</span>
          <input v-model.trim="cancelForm.refundAmount" class="field" type="number" min="0" step="0.01" placeholder="可选" />
        </label>
        <label class="login-field">
          <span>后台备注</span>
          <textarea v-model.trim="cancelForm.adminRemark" class="filter-textarea" placeholder="可选"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="cancelDialogOpen = false">取消</button>
        <button class="button button-danger" type="button" :disabled="actionLoading" @click="submitCancel">
          {{ actionLoading ? '提交中...' : '确认取消' }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="disputeDialogOpen" title="处理争议订单" description="最终金额高于当前已付金额时，订单会进入待用户补差额。" width="620px">
      <div class="page-stack">
        <div class="kv-grid">
          <div class="kv-item">
            <p class="kv-label">陪诊师提交时长</p>
            <p class="kv-value">{{ formatDuration(getActualDuration(order)) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">用户认可时长</p>
            <p class="kv-value">{{ formatDuration(order.timeDisputeUserDuration) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前金额</p>
            <p class="kv-value">{{ formatMoney(order.orderAmount) }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">裁定后差额</p>
            <p class="kv-value">{{ formatMoney(disputeBalancePreview) }}</p>
          </div>
        </div>
        <label class="login-field">
          <span>最终服务时长（小时）</span>
          <input v-model.trim="disputeForm.finalDuration" class="field" type="number" min="0" step="0.5" />
        </label>
        <label class="login-field">
          <span>最终订单金额</span>
          <input v-model.trim="disputeForm.finalOrderAmount" class="field" type="number" min="0" step="0.01" />
        </label>
        <label class="login-field">
          <span>后台备注</span>
          <textarea v-model.trim="disputeForm.adminRemark" class="filter-textarea" placeholder="请输入处理备注"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="disputeDialogOpen = false">取消</button>
        <button class="button button-primary" type="button" :disabled="actionLoading" @click="submitDispute">
          {{ actionLoading ? '提交中...' : '确认处理' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useUiStore } from '../stores/ui'
import { cancelOrder, fetchOrderDetail, resolveDispute } from '../utils/admin-api'
import { getOrderStatusBadge, getOrderStatusLabel, getPaymentStatusBadge, getPaymentStatusLabel } from '../utils/admin-view'
import { formatDateTime, formatMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const loading = ref(true)
const detail = ref(null)

const cancelDialogOpen = ref(false)
const disputeDialogOpen = ref(false)
const actionLoading = ref(false)

const cancelForm = reactive({
  reason: '',
  refundAmount: '',
  adminRemark: ''
})

const disputeForm = reactive({
  finalDuration: '',
  finalOrderAmount: '',
  adminRemark: ''
})

const order = computed(() => detail.value?.order || {})
const orderStatusLabel = computed(() => getOrderStatusLabel(order.value?.orderStatus, '--'))
const paymentStatusLabel = computed(() => getPaymentStatusLabel(order.value?.paymentStatus, '--'))
const disputeBalancePreview = computed(() => {
  const finalAmount = Number(disputeForm.finalOrderAmount)
  const currentAmount = Number(order.value?.orderAmount || 0)
  if (!Number.isFinite(finalAmount)) return ''
  return finalAmount - currentAmount
})

const firstValidValue = (payload, keys) => {
  for (const key of keys) {
    const value = payload?.[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

const formatSex = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return '未知'
  const lower = raw.toLowerCase()
  if (['1', 'male', '男', 'man'].includes(lower)) return '男'
  if (['2', 'female', '女', 'woman'].includes(lower)) return '女'
  return raw
}

const getPatientName = (orderItem) => firstValidValue(orderItem, ['patientName', 'userName', 'name']) || '-'
const getPatientSex = (orderItem) => firstValidValue(orderItem, ['patientSex', 'sex', 'gender'])
const getPatientAge = (orderItem) => firstValidValue(orderItem, ['patientAge', 'age', 'userAge']) || '-'
const getContactName = (orderItem) => firstValidValue(orderItem, ['contactPerson', 'contactName', 'emergencyContact', 'userName']) || '-'
const getContactPhone = (orderItem) => firstValidValue(orderItem, ['contactPhone', 'contactMobile', 'userPhone', 'phone']) || '-'
const getServiceContent = (orderItem) => firstValidValue(orderItem, ['serviceContent', 'serviceTypeName', 'serviceType', 'serviceProject']) || '-'
const getSpecialRequirement = (orderItem) => firstValidValue(orderItem, ['specialRequirements', 'customRequirement', 'remark', 'note']) || '-'
const getPaymentTime = (orderItem) => firstValidValue(orderItem, ['paymentTime', 'payTime', 'paidTime'])
const getAcceptTime = (orderItem) => firstValidValue(orderItem, ['acceptTime', 'takeOrderTime', 'receiveOrderTime'])
const getServiceStartTime = (orderItem) => firstValidValue(orderItem, ['serviceStartTime', 'startTime'])
const getServiceEndTime = (orderItem) => firstValidValue(orderItem, ['serviceEndTime', 'endTime'])
const getActualDuration = (orderItem) => firstValidValue(orderItem, ['actualDuration', 'serviceDuration', 'finalDuration', 'timeDisputeFinalDuration', 'timeDisputeUserDuration'])
const getFinalOrderAmount = (orderItem) => firstValidValue(orderItem, ['finalOrderAmount', 'settlementAmount', 'actualPayAmount', 'orderAmount'])
const getBalanceAmount = (orderItem) => firstValidValue(orderItem, ['balanceAmount', 'differenceAmount'])
const getRefundAmount = (orderItem) => firstValidValue(orderItem, ['refundAmount', 'refundFee'])
const getAdminRemark = (orderItem) => firstValidValue(orderItem, ['adminRemark', 'remark', 'backendRemark']) || '-'
const getDisputeReason = (orderItem) => firstValidValue(orderItem, ['timeDisputeReason', 'disputeReason', 'disputeRemark']) || '-'

const formatDuration = (value) => {
  if (value === '' || value === null || value === undefined) return '-'
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) return `${value}`
  return `${numericValue} 小时`
}

const canCancel = (status) => status !== 6 && status !== 7

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await fetchOrderDetail(route.params.id)
  } catch (error) {
    uiStore.toast(error.message || '订单详情加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const openCancelDialog = () => {
  cancelForm.reason = ''
  cancelForm.refundAmount = detail.value?.order?.orderAmount ? String(detail.value.order.orderAmount) : ''
  cancelForm.adminRemark = ''
  cancelDialogOpen.value = true
}

const openDisputeDialog = () => {
  disputeForm.finalDuration = detail.value?.order?.timeDisputeUserDuration ? String(detail.value.order.timeDisputeUserDuration) : (getActualDuration(detail.value?.order) ? String(getActualDuration(detail.value.order)) : '')
  disputeForm.finalOrderAmount = getSuggestedFinalAmount(detail.value?.order)
  disputeForm.adminRemark = ''
  disputeDialogOpen.value = true
}

const openDisputeWorkbench = () => {
  const orderId = detail.value?.order?.orderId
  if (!orderId) return
  router.push({ name: 'workbench', query: { type: 'ORDER_DISPUTE', targetId: String(orderId) } })
}

const submitCancel = async () => {
  if (!detail.value) return
  if (!cancelForm.reason) {
    uiStore.toast('请填写取消原因', 'error')
    return
  }
  actionLoading.value = true
  try {
    await cancelOrder(detail.value.order.orderId, {
      reason: cancelForm.reason,
      refundAmount: cancelForm.refundAmount ? Number(cancelForm.refundAmount) : undefined,
      adminRemark: cancelForm.adminRemark
    })
    uiStore.toast('订单已取消', 'success')
    cancelDialogOpen.value = false
    await loadDetail()
  } catch (error) {
    uiStore.toast(error.message || '取消订单失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const submitDispute = async () => {
  if (!detail.value) return
  if (!disputeForm.finalDuration || !disputeForm.finalOrderAmount) {
    uiStore.toast('请填写最终时长和金额', 'error')
    return
  }
  if (!disputeForm.adminRemark) {
    uiStore.toast('请填写争议处理备注', 'error')
    return
  }
  actionLoading.value = true
  try {
    await resolveDispute(detail.value.order.orderId, {
      finalDuration: Number(disputeForm.finalDuration),
      finalOrderAmount: Number(disputeForm.finalOrderAmount),
      adminRemark: disputeForm.adminRemark
    })
    uiStore.toast('争议处理成功', 'success')
    disputeDialogOpen.value = false
    await loadDetail()
  } catch (error) {
    uiStore.toast(error.message || '争议处理失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadDetail)

const getSuggestedFinalAmount = (orderItem = {}) => {
  const currentAmount = Number(orderItem.orderAmount || 0)
  const balance = Number(orderItem.balanceAmount || 0)
  const suggested = currentAmount + (Number.isFinite(balance) ? balance : 0)
  return Number.isFinite(suggested) ? String(suggested.toFixed(2)) : ''
}

const mapAdminRole = (role) => (role === 'SUPER_ADMIN' ? '超级管理员' : role === 'ADMIN' ? '管理员' : '-')
</script>

<style scoped>
.no-wrap {
  white-space: nowrap;
}

.detail-row-label {
  flex: 0 0 140px;
}

.detail-row-value {
  text-align: left;
}

@media (max-width: 900px) {
  .section-heading {
    align-items: flex-start;
  }

  .detail-row {
    flex-direction: column;
    gap: 8px;
  }

  .detail-row-label {
    min-width: 0;
    flex: 0 0 auto;
  }
}
</style>
