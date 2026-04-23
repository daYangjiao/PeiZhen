<template>
  <AppShell title="订单详情" subtitle="订单快照、服务信息与后台处理">
    <div class="page-stack">
      <section v-if="loading" class="skeleton"></section>

      <template v-else-if="detail">
        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">{{ detail.order.orderNo }}</h3>
              <p class="section-copy">{{ detail.order.hospital || '-' }} · {{ detail.order.serviceDate || '-' }} {{ detail.order.serviceTimeSlot || '' }}</p>
            </div>
            <div class="toolbar-group">
              <span class="badge" :class="getOrderStatusBadge(detail.order.orderStatus)">{{ orderStatusLabel }}</span>
              <span class="badge" :class="getPaymentStatusBadge(detail.order.paymentStatus)">{{ paymentStatusLabel }}</span>
              <button class="button button-ghost" type="button" @click="router.push('/orders')">返回列表</button>
            </div>
          </div>

          <div class="kv-grid">
            <div class="kv-item">
              <p class="kv-label">患者信息</p>
              <p class="kv-value">{{ detail.order.patientName || '-' }} / {{ detail.order.patientSex || '未知' }} / {{ detail.order.patientAge || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">联系人</p>
              <p class="kv-value">{{ detail.order.contactPerson || detail.user?.name || '-' }} / {{ detail.order.contactPhone || detail.user?.phone || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">服务内容</p>
              <p class="kv-value">{{ detail.order.serviceContent || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">特殊需求</p>
              <p class="kv-value">{{ detail.order.specialRequirements || detail.order.customRequirement || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">支付时间</p>
              <p class="kv-value">{{ formatDateTime(detail.order.paymentTime) }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">陪诊师</p>
              <p class="kv-value">{{ detail.attendant?.name || detail.order.attendantName || '暂未接单' }} / {{ detail.attendant?.phone || '-' }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">创建时间</p>
              <p class="kv-value">{{ formatDateTime(detail.order.createTime) }}</p>
            </div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">金额拆分</h3>
              <p class="section-copy">展示订单金额、退款与差额明细。</p>
            </div>
          </div>
          <div class="amount-breakdown-grid">
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">订单金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(detail.order.orderAmount) }}</p>
            </div>
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">退款金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(detail.order.refundAmount) }}</p>
            </div>
            <div class="amount-breakdown-item">
              <p class="amount-breakdown-label">差额金额</p>
              <p class="amount-breakdown-value">{{ formatMoney(detail.order.balanceAmount) }}</p>
            </div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">后台备注</div>
            <div class="detail-row-value">{{ detail.order.adminRemark || '-' }}</div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">订单进展</h3>
              <p class="section-copy">优先展示真实字段，不再制造假的步骤时间线。</p>
            </div>
            <div class="toolbar-group">
              <button v-if="canCancel(detail.order.orderStatus)" class="button button-danger" type="button" @click="openCancelDialog">取消订单</button>
              <button v-if="detail.order.orderStatus === 5" class="button button-primary" type="button" @click="openDisputeDialog">处理争议</button>
            </div>
          </div>

          <div class="detail-row">
            <div class="detail-row-label">支付状态</div>
            <div class="detail-row-value">{{ paymentStatusLabel }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">订单状态</div>
            <div class="detail-row-value">{{ orderStatusLabel }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">服务进度步骤</div>
            <div class="detail-row-value">{{ detail.order.serviceProgressStep || '-' }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">接单时间</div>
            <div class="detail-row-value">{{ formatDateTime(detail.order.acceptTime) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">服务开始 / 结束</div>
            <div class="detail-row-value">{{ formatDateTime(detail.order.serviceStartTime) }} / {{ formatDateTime(detail.order.serviceEndTime) }}</div>
          </div>
          <div class="detail-row">
            <div class="detail-row-label">争议说明</div>
            <div class="detail-row-value">{{ detail.order.timeDisputeReason || '-' }}</div>
          </div>
          <div class="detail-row" v-if="detail.order.cancelReason">
            <div class="detail-row-label">取消信息</div>
            <div class="detail-row-value">
              {{ detail.order.cancelReason }}<br />
              {{ formatDateTime(detail.order.cancelTime) }}
            </div>
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

    <BaseDialog v-model="disputeDialogOpen" title="处理争议订单" description="订单详情页可直接确认争议结果。" width="620px">
      <div class="page-stack">
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

const orderStatusLabel = computed(() => getOrderStatusLabel(detail.value?.order?.orderStatus, '--'))
const paymentStatusLabel = computed(() => getPaymentStatusLabel(detail.value?.order?.paymentStatus, '--'))

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
  disputeForm.finalDuration = detail.value?.order?.timeDisputeUserDuration ? String(detail.value.order.timeDisputeUserDuration) : ''
  disputeForm.finalOrderAmount = detail.value?.order?.orderAmount ? String(detail.value.order.orderAmount) : ''
  disputeForm.adminRemark = ''
  disputeDialogOpen.value = true
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
</script>
