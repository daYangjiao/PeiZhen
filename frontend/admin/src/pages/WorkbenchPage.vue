<template>
  <AppShell title="处理工作台" subtitle="争议与审核流水线">
    <div class="workbench-page">
      <section class="panel-card workbench-header">
        <div class="workbench-tabs">
          <button
            v-for="option in typeOptions"
            :key="option.value"
            class="workbench-tab"
            :class="{ active: currentType === option.value }"
            type="button"
            @click="switchType(option.value)"
          >
            <span>{{ option.label }}</span>
            <strong>{{ option.count }}</strong>
          </button>
        </div>
        <div class="toolbar-group">
          <span class="summary-pill">我的处理中 <strong>{{ summary.myClaimCount || 0 }}</strong></span>
          <button class="button button-secondary" type="button" :disabled="loading" @click="reloadAll">刷新</button>
        </div>
      </section>

      <section class="panel-card filter-card">
        <div class="filter-board">
          <label class="filter-field">
            <span>关键词</span>
            <input v-model.trim="keyword" class="field-inline" type="text" :placeholder="currentType === ORDER_DISPUTE ? '订单号、患者、医院' : '姓名、手机号或医院'" @keyup.enter="submitSearch" />
          </label>
          <div class="filter-actions-row">
            <button class="button button-primary" type="button" :disabled="loading" @click="submitSearch">查询</button>
            <button class="button button-ghost" type="button" :disabled="loading" @click="resetSearch">重置</button>
          </div>
        </div>
      </section>

      <section class="workbench-layout">
        <aside class="panel-card workbench-queue">
          <div class="section-heading">
            <div>
              <h3 class="section-title">待办队列</h3>
              <p class="section-copy">共 {{ total }} 条，当前第 {{ page + 1 }} / {{ totalPages }} 页</p>
            </div>
          </div>

          <div v-if="loading" class="skeleton"></div>
          <div v-else-if="tasks.length" class="task-list">
            <button
              v-for="task in tasks"
              :key="`${task.taskType}-${task.targetId}`"
              class="task-item"
              :class="{ active: selectedTargetId === task.targetId, locked: task.claimed && !task.claimMine }"
              type="button"
              @click="selectTask(task)"
            >
              <div>
                <p class="task-title">{{ task.title || `#${task.targetId}` }}</p>
                <p class="task-copy">{{ task.subtitle || '-' }}</p>
                <p v-if="task.claimed" class="task-copy">
                  {{ task.claimMine ? '系统已分配给我' : (authStore.isSuperAdmin && task.operatorName ? `${task.operatorName}处理中` : '处理中') }}
                </p>
              </div>
              <span class="badge" :class="task.claimed && !task.claimMine ? 'badge-gray' : 'badge-orange'">{{ task.statusLabel || '待处理' }}</span>
            </button>
          </div>
          <div v-else class="empty-card">当前没有待处理任务。</div>

          <div class="pagination">
            <div class="pagination-controls">
              <button class="button button-ghost" type="button" :disabled="page === 0 || loading" @click="changePage(page - 1)">上一页</button>
              <button class="button button-ghost" type="button" :disabled="page + 1 >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
            </div>
          </div>
        </aside>

        <main class="panel-card workbench-detail">
          <template v-if="detailLoading">
            <div class="skeleton"></div>
          </template>
          <template v-else-if="currentType === ORDER_DISPUTE && order">
            <div class="section-heading">
              <div>
                <h3 class="section-title">{{ order.orderNo || `订单 ${selectedTargetId}` }}</h3>
                <p class="section-copy">{{ order.hospital || '-' }} · {{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</p>
              </div>
              <span class="badge badge-orange">争议处理中</span>
            </div>

            <div class="evidence-grid">
              <article class="evidence-card">
                <p class="evidence-label">陪诊师提交</p>
                <strong>{{ formatDurationHour(getActualDuration(order)) }}</strong>
                <span>建议金额 {{ formatMoney(attendantSuggestedAmount) }}</span>
              </article>
              <article class="evidence-card">
                <p class="evidence-label">用户申诉</p>
                <strong>{{ formatDurationHour(order.timeDisputeUserDuration) }}</strong>
                <span>{{ order.timeDisputeReason || '未填写申诉说明' }}</span>
              </article>
              <article class="evidence-card">
                <p class="evidence-label">平台裁定预览</p>
                <strong>{{ disputePreviewLabel }}</strong>
                <span>{{ disputePreviewAmount }}</span>
              </article>
            </div>

            <div class="kv-grid">
              <div class="kv-item"><p class="kv-label">患者 / 联系人</p><p class="kv-value">{{ order.patientName || '-' }} / {{ order.contactPerson || order.userName || '-' }}</p></div>
              <div class="kv-item"><p class="kv-label">联系电话</p><p class="kv-value">{{ order.contactPhone || order.userPhone || '-' }}</p></div>
              <div class="kv-item"><p class="kv-label">陪诊师</p><p class="kv-value">{{ orderDetail?.attendant?.name || order.attendantName || '-' }}</p></div>
              <div class="kv-item"><p class="kv-label">服务开始 / 结束</p><p class="kv-value">{{ formatDateTime(order.serviceStartTime) }} / {{ formatDateTime(order.serviceEndTime) }}</p></div>
              <div class="kv-item"><p class="kv-label">当前订单金额</p><p class="kv-value">{{ formatMoney(order.orderAmount) }}</p></div>
              <div class="kv-item"><p class="kv-label">当前差额</p><p class="kv-value">{{ formatMoney(order.balanceAmount) }}</p></div>
            </div>
          </template>

          <template v-else-if="currentType === ATTENDANT_REVIEW && attendantDetail">
            <div class="section-heading">
              <div>
                <h3 class="section-title">{{ attendantDetail.user?.name || attendantDetail.user?.phone || `陪诊师 ${selectedTargetId}` }}</h3>
                <p class="section-copy">{{ attendantDetail.user?.phone || '-' }} · {{ attendantDetail.attendant?.hospitalName || '未填写常驻医院' }}</p>
              </div>
              <span class="badge badge-orange">待审核</span>
            </div>

            <div class="qualification-summary">
              <div class="summary-pill"><span>材料完整度</span><strong>{{ qualificationCompleteness }}%</strong></div>
              <div class="summary-pill" :class="{ danger: qualificationBlocked }"><span>审核结果</span><strong>{{ qualificationBlocked ? approveDisabledReason : '可通过' }}</strong></div>
            </div>

            <div class="kv-grid">
              <div class="kv-item"><p class="kv-label">擅长领域</p><p class="kv-value">{{ attendantDetail.attendant?.professionalField || '-' }}</p></div>
              <div class="kv-item"><p class="kv-label">从业年限</p><p class="kv-value">{{ attendantDetail.attendant?.experienceYears || 0 }} 年</p></div>
              <div class="kv-item"><p class="kv-label">服务单量</p><p class="kv-value">{{ attendantDetail.attendant?.serviceCount || 0 }} 单</p></div>
              <div class="kv-item"><p class="kv-label">历史完成</p><p class="kv-value">{{ attendantDetail.completedOrderCount || 0 }} / {{ attendantDetail.totalOrderCount || 0 }}</p></div>
            </div>

            <div class="qualification-grid">
              <article v-for="card in qualificationCards" :key="card.key" class="qualification-card">
                <p class="qualification-title">{{ card.title }}</p>
                <button v-if="card.url" class="qualification-thumb-button" type="button" @click="openPreview(card)">
                  <img :src="card.url" :alt="card.title" class="qualification-thumb-image" />
                </button>
                <div v-else class="qualification-thumb-placeholder">未上传</div>
                <p class="qualification-status" :class="card.url ? 'is-uploaded' : 'is-missing'">{{ card.url ? '已上传' : '缺失' }}</p>
                <p v-if="card.expireDate" class="qualification-expire" :class="{ 'is-expired': card.expired }">有效期 {{ card.expireDate }}{{ card.expired ? '（已过期）' : '' }}</p>
              </article>
            </div>
          </template>

          <template v-else>
            <div class="empty-card">请选择左侧任务开始处理。</div>
          </template>
        </main>

        <aside class="panel-card action-panel">
          <div class="section-heading">
            <div>
              <h3 class="section-title">处理区</h3>
              <p class="section-copy">{{ lockToken ? `系统保护剩余 ${lockRemainingText}` : '选择任务后系统自动分配' }}</p>
            </div>
          </div>

          <template v-if="selectedTargetId && lockToken">
            <div v-if="currentType === ORDER_DISPUTE" class="page-stack">
              <div class="mode-grid">
                <button class="button" :class="disputeMode === 'attendant' ? 'button-primary' : 'button-secondary'" type="button" @click="applyDisputeMode('attendant')">按陪诊师</button>
                <button class="button" :class="disputeMode === 'user' ? 'button-primary' : 'button-secondary'" type="button" @click="applyDisputeMode('user')">按用户</button>
                <button class="button" :class="disputeMode === 'custom' ? 'button-primary' : 'button-secondary'" type="button" @click="disputeMode = 'custom'">自定义</button>
              </div>
              <label class="login-field"><span>最终时长</span><input v-model.trim="disputeForm.finalDuration" class="field" type="number" min="0" step="0.5" /></label>
              <label class="login-field"><span>最终金额</span><input v-model.trim="disputeForm.finalOrderAmount" class="field" type="number" min="0" step="0.01" /></label>
              <label class="login-field"><span>处理备注</span><textarea v-model.trim="disputeForm.adminRemark" class="filter-textarea" placeholder="请输入处理依据和结果"></textarea></label>
              <button class="button button-primary" type="button" :disabled="actionLoading" @click="confirmComplete">提交处理</button>
            </div>

            <div v-else class="page-stack">
              <button class="button button-primary" type="button" :disabled="qualificationBlocked || actionLoading" @click="completeAttendant('approve')">通过审核</button>
              <div class="reason-chip-row">
                <button v-for="chip in rejectReasons" :key="chip" class="button button-secondary reason-chip" type="button" @click="reviewForm.reason = chip">{{ chip }}</button>
              </div>
              <label class="login-field"><span>驳回原因</span><textarea v-model.trim="reviewForm.reason" class="filter-textarea" placeholder="请输入驳回原因"></textarea></label>
              <button class="button button-danger" type="button" :disabled="actionLoading" @click="completeAttendant('reject')">驳回审核</button>
            </div>

            <p class="section-copy auto-renew-copy">系统会自动续期当前任务，离开页面或处理完成后自动释放。</p>
          </template>
          <div v-else class="empty-card">当前没有分配中的任务。</div>
        </aside>
      </section>
    </div>

    <BaseDialog v-model="confirmDialogOpen" title="确认提交处理" :description="confirmDescription" width="520px">
      <p class="section-copy">提交后任务会从队列移除，并自动进入下一条。</p>
      <template #footer>
        <button class="button button-ghost" type="button" @click="confirmDialogOpen = false">取消</button>
        <button class="button button-primary" type="button" :disabled="actionLoading" @click="submitOrderDispute">确认提交</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="previewDialogOpen" :title="previewTitle || '资质预览'" width="760px">
      <div class="qualification-preview">
        <img v-if="previewImageUrl" :src="previewImageUrl" :alt="previewTitle || '资质预览'" class="qualification-preview-image" />
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="previewDialogOpen = false">关闭</button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { claimWorkbenchTask, completeWorkbenchTask, fetchAttendantDetail, fetchOrderDetail, fetchWorkbenchSummary, fetchWorkbenchTasks, releaseWorkbenchTask } from '../utils/admin-api'
import { formatDateTime, formatMoney } from '../utils/format'

const ORDER_DISPUTE = 'ORDER_DISPUTE'
const ATTENDANT_REVIEW = 'ATTENDANT_REVIEW'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const authStore = useAuthStore()
authStore.restore()

const currentType = ref(route.query.type === ATTENDANT_REVIEW ? ATTENDANT_REVIEW : ORDER_DISPUTE)
const keyword = ref('')
const tasks = ref([])
const summary = reactive({ disputeOrderCount: 0, attendantReviewCount: 0, myClaimCount: 0 })
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const actionLoading = ref(false)
const selectedTargetId = ref(route.query.targetId ? Number(route.query.targetId) : null)
const lockToken = ref('')
const lockExpiresAt = ref(null)
const now = ref(Date.now())
const orderDetail = ref(null)
const attendantDetail = ref(null)
const disputeMode = ref('attendant')
const disputeForm = reactive({ finalDuration: '', finalOrderAmount: '', adminRemark: '' })
const reviewForm = reactive({ reason: '' })
const confirmDialogOpen = ref(false)
const previewDialogOpen = ref(false)
const previewImageUrl = ref('')
const previewTitle = ref('')
let timer = null
let refreshTimer = null
let autoRenewing = false

const typeOptions = computed(() => [
  { label: '订单争议', value: ORDER_DISPUTE, count: summary.disputeOrderCount || 0 },
  { label: '陪诊师审核', value: ATTENDANT_REVIEW, count: summary.attendantReviewCount || 0 }
])
const order = computed(() => orderDetail.value?.order || null)
const qualification = computed(() => attendantDetail.value?.qualification || {})
const lockRemainingText = computed(() => {
  if (!lockExpiresAt.value) return '--'
  const remaining = Math.max(0, new Date(lockExpiresAt.value).getTime() - now.value)
  const minutes = Math.floor(remaining / 60000)
  const seconds = Math.floor((remaining % 60000) / 1000)
  return `${minutes}:${String(seconds).padStart(2, '0')}`
})
const attendantSuggestedAmount = computed(() => Number(order.value?.orderAmount || 0) + Number(order.value?.balanceAmount || 0))
const disputeBalancePreview = computed(() => Number(disputeForm.finalOrderAmount || 0) - Number(order.value?.orderAmount || 0))
const disputePreviewLabel = computed(() => disputeBalancePreview.value > 0 ? '待用户补差额' : '直接完成')
const disputePreviewAmount = computed(() => disputeBalancePreview.value > 0 ? `补差额 ${formatMoney(disputeBalancePreview.value)}` : `退款 ${formatMoney(Math.abs(Math.min(disputeBalancePreview.value, 0)))}`)
const confirmDescription = computed(() => `最终时长 ${disputeForm.finalDuration || '-'} 小时，最终金额 ${formatMoney(disputeForm.finalOrderAmount || 0)}。`)
const qualificationCompleteness = computed(() => {
  const cards = qualificationCards.value
  if (!cards.length) return 0
  return Math.round((cards.filter((card) => card.url && !card.expired).length / cards.length) * 100)
})
const qualificationBlocked = computed(() => Boolean(approveDisabledReason.value))
const approveDisabledReason = computed(() => {
  const missing = qualificationCards.value.find((card) => !card.url)
  if (missing) return `${missing.title}缺失`
  const expired = qualificationCards.value.find((card) => card.expired)
  if (expired) return `${expired.title}过期`
  return ''
})
const qualificationCards = computed(() => {
  const item = qualification.value || {}
  return [
    { key: 'id-front', title: '身份证正面', url: item.idCardFrontFileUrl || item.idCardFileUrl },
    { key: 'id-back', title: '身份证背面', url: item.idCardBackFileUrl },
    { key: 'practice', title: '执业证', url: item.practiceCertFileUrl, expireDate: item.practiceCertExpireDate, expired: isExpired(item.practiceCertExpireDate) },
    { key: 'health', title: '健康证', url: item.healthCertFileUrl, expireDate: item.healthCertExpireDate, expired: isExpired(item.healthCertExpireDate) }
  ]
})
const rejectReasons = ['证件照片不清晰', '证件信息不完整', '证件已过期', '个人资料不完整']

const getActualDuration = (item) => item?.actualDuration || item?.timeDisputeUserDuration || ''
const formatDurationHour = (value) => value ? `${value} 小时` : '-'
const isExpired = (dateText) => Boolean(dateText && dateText < new Date().toISOString().slice(0, 10))

const syncQuery = () => {
  router.replace({
    query: {
      type: currentType.value,
      ...(selectedTargetId.value ? { targetId: String(selectedTargetId.value) } : {}),
      ...(keyword.value ? { keyword: keyword.value } : {})
    }
  })
}

const loadSummary = async () => {
  Object.assign(summary, await fetchWorkbenchSummary())
}

const loadTasks = async () => {
  loading.value = true
  try {
    const response = await fetchWorkbenchTasks({ type: currentType.value, keyword: keyword.value, page: page.value, pageSize: 10 })
    tasks.value = response.content || []
    total.value = response.totalElements || 0
    totalPages.value = Math.max(response.totalPages || 1, 1)
    if (selectedTargetId.value && !tasks.value.some((task) => task.targetId === selectedTargetId.value)) {
      lockToken.value = ''
      lockExpiresAt.value = null
      selectedTargetId.value = null
      orderDetail.value = null
      attendantDetail.value = null
    }
    if (!selectedTargetId.value && tasks.value.length) {
      const next = tasks.value.find((task) => !task.claimed || task.claimMine)
      if (next) {
        await selectTask(next)
      }
    }
  } catch (error) {
    uiStore.toast(error.message || '待办队列加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const reloadAll = async () => {
  await loadSummary()
  await loadTasks()
}

const selectTask = async (task) => {
  if (!task?.targetId) return
  if (task.claimed && !task.claimMine) {
    uiStore.toast('该任务正在处理中，系统会自动分配下一条', 'info')
    const next = tasks.value.find((item) => item.targetId !== task.targetId && (!item.claimed || item.claimMine))
    if (next) await selectTask(next)
    return
  }
  selectedTargetId.value = task.targetId
  syncQuery()
  await claimCurrentTask()
  await loadDetail()
}

const claimCurrentTask = async () => {
  if (!selectedTargetId.value) return
  try {
    const response = await claimWorkbenchTask(currentType.value, selectedTargetId.value)
    lockToken.value = response.lockToken
    lockExpiresAt.value = response.expiresAt
  } catch (error) {
    lockToken.value = ''
    lockExpiresAt.value = null
    uiStore.toast(error.message || '任务领取失败', 'error')
    throw error
  }
}

const renewClaim = async (silent = false) => {
  await claimCurrentTask()
  if (!silent) {
    uiStore.toast('任务保护已续期', 'success')
  }
}

const autoRenewCurrentClaim = async () => {
  if (!selectedTargetId.value || !lockToken.value || !lockExpiresAt.value || autoRenewing) return
  const remaining = new Date(lockExpiresAt.value).getTime() - Date.now()
  if (remaining <= 0) {
    lockToken.value = ''
    lockExpiresAt.value = null
    await reloadAll()
    return
  }
  if (remaining > 120000) return
  autoRenewing = true
  try {
    await renewClaim(true)
  } catch (error) {
    lockToken.value = ''
    lockExpiresAt.value = null
    uiStore.toast(error.message || '任务保护已失效，系统将重新分配', 'error')
    await reloadAll()
  } finally {
    autoRenewing = false
  }
}

const releaseCurrentClaim = () => {
  if (!selectedTargetId.value || !lockToken.value) return
  releaseWorkbenchTask(currentType.value, selectedTargetId.value).catch(() => {})
}

const loadDetail = async () => {
  if (!selectedTargetId.value || !lockToken.value) return
  detailLoading.value = true
  try {
    if (currentType.value === ORDER_DISPUTE) {
      orderDetail.value = await fetchOrderDetail(selectedTargetId.value)
      attendantDetail.value = null
      applyDisputeMode('attendant')
    } else {
      attendantDetail.value = await fetchAttendantDetail(selectedTargetId.value)
      orderDetail.value = null
    }
  } catch (error) {
    uiStore.toast(error.message || '详情加载失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

const applyDisputeMode = (mode) => {
  disputeMode.value = mode
  if (!order.value) return
  if (mode === 'attendant') {
    disputeForm.finalDuration = String(order.value.actualDuration || '')
    disputeForm.finalOrderAmount = attendantSuggestedAmount.value ? attendantSuggestedAmount.value.toFixed(2) : String(order.value.orderAmount || '')
  } else if (mode === 'user') {
    disputeForm.finalDuration = String(order.value.timeDisputeUserDuration || order.value.actualDuration || '')
    disputeForm.finalOrderAmount = String(order.value.orderAmount || '')
  }
}

const confirmComplete = () => {
  if (!disputeForm.finalDuration || !disputeForm.finalOrderAmount || !disputeForm.adminRemark) {
    uiStore.toast('请填写最终时长、最终金额和处理备注', 'error')
    return
  }
  confirmDialogOpen.value = true
}

const submitOrderDispute = async () => {
  actionLoading.value = true
  try {
    await completeWorkbenchTask(currentType.value, selectedTargetId.value, {
      lockToken: lockToken.value,
      finalDuration: Number(disputeForm.finalDuration),
      finalOrderAmount: Number(disputeForm.finalOrderAmount),
      adminRemark: disputeForm.adminRemark
    })
    confirmDialogOpen.value = false
    await afterComplete()
  } catch (error) {
    uiStore.toast(error.message || '争议处理失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const completeAttendant = async (action) => {
  if (action === 'reject' && !reviewForm.reason) {
    uiStore.toast('请填写驳回原因', 'error')
    return
  }
  actionLoading.value = true
  try {
    await completeWorkbenchTask(currentType.value, selectedTargetId.value, {
      lockToken: lockToken.value,
      action,
      reason: reviewForm.reason
    })
    await afterComplete()
  } catch (error) {
    uiStore.toast(error.message || '审核处理失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

const afterComplete = async () => {
  uiStore.toast('处理完成', 'success')
  lockToken.value = ''
  lockExpiresAt.value = null
  selectedTargetId.value = null
  orderDetail.value = null
  attendantDetail.value = null
  reviewForm.reason = ''
  disputeForm.adminRemark = ''
  await reloadAll()
  syncQuery()
}

const submitSearch = async () => {
  releaseCurrentClaim()
  page.value = 0
  selectedTargetId.value = null
  lockToken.value = ''
  syncQuery()
  await reloadAll()
}

const resetSearch = async () => {
  keyword.value = ''
  await submitSearch()
}

const changePage = async (nextPage) => {
  releaseCurrentClaim()
  page.value = nextPage
  selectedTargetId.value = null
  lockToken.value = ''
  await loadTasks()
}

const switchType = async (type) => {
  if (currentType.value === type) return
  releaseCurrentClaim()
  currentType.value = type
  selectedTargetId.value = null
  lockToken.value = ''
  orderDetail.value = null
  attendantDetail.value = null
  page.value = 0
  syncQuery()
  await reloadAll()
}

const openPreview = (card) => {
  previewTitle.value = card.title
  previewImageUrl.value = card.url
  previewDialogOpen.value = true
}

watch(
  () => route.query,
  async (query) => {
    const nextType = query.type === ATTENDANT_REVIEW ? ATTENDANT_REVIEW : ORDER_DISPUTE
    const nextTarget = query.targetId ? Number(query.targetId) : null
    if (nextType !== currentType.value) currentType.value = nextType
    if (nextTarget && nextTarget !== selectedTargetId.value) {
      selectedTargetId.value = nextTarget
      await claimCurrentTask()
      await loadDetail()
    }
  }
)

onMounted(async () => {
  timer = window.setInterval(() => {
    now.value = Date.now()
    autoRenewCurrentClaim()
  }, 1000)
  refreshTimer = window.setInterval(() => {
    if (!loading.value && !actionLoading.value) {
      reloadAll()
    }
  }, 15000)
  await reloadAll()
  if (selectedTargetId.value) {
    await claimCurrentTask()
    await loadDetail()
  }
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  if (refreshTimer) window.clearInterval(refreshTimer)
  releaseCurrentClaim()
})
</script>

<style scoped>
.workbench-page {
  display: grid;
  gap: 14px;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.workbench-tabs,
.mode-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.workbench-tab {
  min-height: 44px;
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 0 14px;
  background: #fff;
  color: var(--text);
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.workbench-tab.active {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 12px 24px rgba(42, 120, 255, 0.18);
}

.workbench-layout {
  display: grid;
  grid-template-columns: minmax(260px, 0.78fr) minmax(0, 1.45fr) minmax(280px, 0.82fr);
  gap: 14px;
  align-items: start;
}

.workbench-queue,
.workbench-detail,
.action-panel {
  min-height: 640px;
}

.task-list {
  display: grid;
  gap: 10px;
}

.task-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: #fff;
  text-align: left;
}

.task-item.active {
  border-color: rgba(42, 120, 255, 0.42);
  background: rgba(237, 244, 255, 0.92);
}

.task-item.locked {
  opacity: 0.62;
}

.task-title {
  margin: 0;
  color: var(--text);
  font-weight: 800;
  line-height: 1.4;
}

.task-copy {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.45;
}

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.evidence-card {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface-soft);
}

.evidence-card strong,
.evidence-card span {
  display: block;
  margin-top: 8px;
}

.evidence-label {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.summary-pill.danger {
  color: var(--danger);
  background: rgba(228, 85, 85, 0.08);
}

.full-width {
  width: 100%;
}

.auto-renew-copy {
  margin: 2px 0 0;
}

@media (max-width: 1280px) {
  .workbench-layout {
    grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.2fr);
  }

  .action-panel {
    grid-column: 1 / -1;
    min-height: 0;
  }
}

@media (max-width: 860px) {
  .workbench-header,
  .workbench-layout {
    grid-template-columns: 1fr;
  }

  .workbench-header {
    display: grid;
  }

  .workbench-queue,
  .workbench-detail,
  .action-panel {
    min-height: 0;
  }

  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
