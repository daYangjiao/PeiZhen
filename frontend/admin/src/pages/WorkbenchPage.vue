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

        <main class="panel-card workbench-workspace">
          <template v-if="detailLoading">
            <div class="skeleton"></div>
          </template>
          <template v-else-if="currentType === ORDER_DISPUTE && order">
            <div class="workspace-status">
              <div>
                <p class="workspace-kicker">当前任务</p>
                <h3 class="section-title">{{ order.orderNo || `订单 ${selectedTargetId}` }}</h3>
                <p class="section-copy">{{ order.hospital || '-' }} · {{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</p>
              </div>
              <div class="workspace-lock">
                <span class="badge badge-orange">争议处理中</span>
                <span class="lock-pill">{{ lockToken ? `系统保护 ${lockRemainingText}` : '系统自动分配' }}</span>
              </div>
            </div>

            <section class="workspace-section">
              <div class="dispute-focus-grid">
                <article class="dispute-focus-card attendant-side">
                  <p class="dispute-focus-label">陪诊师提交</p>
                  <strong>{{ formatDurationHour(getActualDuration(order)) }}</strong>
                  <span>提交金额 {{ formatMoney(attendantSuggestedAmount) }}</span>
                  <p class="dispute-focus-copy">{{ order.adminRemark || '陪诊师提交实际服务时长后，用户未认可并发起申诉。' }}</p>
                </article>
                <article class="dispute-focus-card user-side">
                  <p class="dispute-focus-label">用户申诉重点</p>
                  <strong>{{ formatDurationHour(order.timeDisputeUserDuration) }}</strong>
                  <span>{{ order.timeDisputeReason || '未填写申诉说明' }}</span>
                  <p class="dispute-focus-copy">请优先核对用户申诉原因、服务时间和双方金额差异。</p>
                </article>
                <article class="dispute-focus-card result-side">
                  <p class="dispute-focus-label">裁定结果预览</p>
                  <strong>{{ disputePreviewLabel }}</strong>
                  <span>{{ disputePreviewAmount }}</span>
                  <p class="dispute-focus-copy">最终金额高于已付金额时，订单进入待用户补差额。</p>
                </article>
              </div>
            </section>

            <section class="action-panel">
              <div>
                <h3 class="section-title">裁定处理</h3>
                <p class="section-copy auto-renew-copy">系统自动续期，离开页面或处理完成后释放。</p>
              </div>
              <div v-if="selectedTargetId && lockToken" class="action-form dispute-action-form">
                <div class="mode-grid">
                  <button class="button" :class="disputeMode === 'attendant' ? 'button-primary' : 'button-secondary'" type="button" @click="applyDisputeMode('attendant')">按陪诊师</button>
                  <button class="button" :class="disputeMode === 'user' ? 'button-primary' : 'button-secondary'" type="button" @click="applyDisputeMode('user')">按用户</button>
                  <button class="button" :class="disputeMode === 'custom' ? 'button-primary' : 'button-secondary'" type="button" @click="disputeMode = 'custom'">自定义</button>
                </div>
                <label class="login-field"><span>最终时长</span><input v-model.trim="disputeForm.finalDuration" class="field" type="number" min="0" step="0.5" /></label>
                <label class="login-field"><span>最终金额</span><input v-model.trim="disputeForm.finalOrderAmount" class="field" type="number" min="0" step="0.01" /></label>
                <label class="login-field action-remark"><span>处理备注</span><textarea v-model.trim="disputeForm.adminRemark" class="filter-textarea" placeholder="请输入处理依据和结果"></textarea></label>
                <button class="button button-primary submit-action" type="button" :disabled="actionLoading" @click="confirmComplete">提交处理</button>
              </div>
              <div v-else class="empty-card inline-empty">当前任务正在分配，请稍候。</div>
            </section>

            <section class="workspace-section">
              <div class="section-heading compact-heading">
                <div>
                  <h3 class="section-title">订单完整信息</h3>
                </div>
              </div>
              <div class="order-full-grid">
                <div v-for="item in orderInfoItems" :key="item.label" class="kv-item">
                  <p class="kv-label">{{ item.label }}</p>
                  <p class="kv-value">{{ item.value }}</p>
                </div>
                <div class="kv-item"><p class="kv-label">当前订单金额</p><p class="kv-value">{{ formatMoney(order.orderAmount) }}</p></div>
                <div class="kv-item"><p class="kv-label">当前差额</p><p class="kv-value">{{ formatMoney(order.balanceAmount) }}</p></div>
                <div class="kv-item"><p class="kv-label">退款金额</p><p class="kv-value">{{ formatMoney(order.refundAmount) }}</p></div>
              </div>
            </section>
          </template>

          <template v-else-if="currentType === ATTENDANT_REVIEW && attendantDetail">
            <div class="workspace-status">
              <div>
                <p class="workspace-kicker">当前任务</p>
                <h3 class="section-title">{{ attendantDetail.user?.name || attendantDetail.user?.phone || `陪诊师 ${selectedTargetId}` }}</h3>
                <p class="section-copy">{{ attendantDetail.user?.phone || '-' }} · {{ attendantDetail.attendant?.hospitalName || '未填写常驻医院' }}</p>
              </div>
              <div class="workspace-lock">
                <span class="badge badge-orange">待审核</span>
                <span class="lock-pill">{{ lockToken ? `系统保护 ${lockRemainingText}` : '系统自动分配' }}</span>
              </div>
            </div>

            <section class="workspace-section qualification-review-stage">
              <div class="qualification-main-viewer" :class="{ missing: !activeQualificationCard?.url, expired: activeQualificationCard?.expired }">
                <div class="qualification-main-head">
                  <div>
                    <p class="workspace-kicker">证件核验</p>
                    <h3 class="section-title">{{ activeQualificationCard?.title || '资质图片' }}</h3>
                    <p v-if="activeQualificationCard?.expireDate" class="section-copy">有效期 {{ activeQualificationCard.expireDate }}</p>
                  </div>
                  <button v-if="activeQualificationCard?.url" class="button button-secondary" type="button" @click="openPreview(activeQualificationCard)">放大查看</button>
                </div>
                <button v-if="activeQualificationCard?.url" class="qualification-main-image-button" type="button" @click="openPreview(activeQualificationCard)">
                  <img :src="activeQualificationCard.url" :alt="activeQualificationCard.title" class="qualification-main-image" />
                </button>
                <div v-else class="qualification-main-placeholder">请选择已上传的证件图片</div>
              </div>

              <div class="review-snapshot-grid">
                <article
                  v-for="card in qualificationCards"
                  :key="card.key"
                  class="review-snapshot-card"
                  :class="{ active: activeQualificationCard?.key === card.key, missing: !card.url, expired: card.expired }"
                >
                  <div class="review-snapshot-head">
                    <p>{{ card.title }}</p>
                    <span>{{ card.url ? (card.expired ? '已过期' : '已上传') : '缺失' }}</span>
                  </div>
                  <button v-if="card.url" class="review-snapshot-image-button" type="button" @click="activeQualificationKey = card.key">
                    <img :src="card.url" :alt="card.title" class="review-snapshot-image" />
                  </button>
                  <div v-else class="review-snapshot-placeholder">未上传</div>
                  <p v-if="card.expireDate" class="review-snapshot-date">有效期 {{ card.expireDate }}</p>
                </article>
              </div>
            </section>

            <section class="action-panel">
              <div>
                <h3 class="section-title">人工审核</h3>
                <p class="section-copy auto-renew-copy">先核对证件图片与个人资料，再选择通过或驳回。</p>
              </div>
              <div v-if="selectedTargetId && lockToken" class="review-decision-grid">
                <article class="decision-card decision-card-approve" :class="{ disabled: qualificationBlocked }">
                  <div>
                    <p class="decision-label">通过入驻</p>
                    <h4 class="decision-title">人工确认资料真实有效</h4>
                    <p class="decision-copy">{{ qualificationBlocked ? `系统检测：${approveDisabledReason}` : '请人工核对姓名、医院、证件图片和有效期后再通过。' }}</p>
                  </div>
                  <button class="button button-primary approve-action" type="button" :disabled="qualificationBlocked || actionLoading" @click="completeAttendant('approve')">通过审核</button>
                </article>

                <article class="decision-card decision-card-reject">
                  <div>
                    <p class="decision-label">驳回补充</p>
                    <h4 class="decision-title">选择原因或填写说明</h4>
                    <p class="decision-copy">驳回后陪诊师端会看到原因，并按要求重新提交材料。</p>
                  </div>
                  <div class="reason-chip-row">
                    <button v-for="chip in rejectReasons" :key="chip" class="button button-secondary reason-chip" type="button" @click="reviewForm.reason = chip">{{ chip }}</button>
                  </div>
                  <label class="login-field action-remark"><span>驳回原因</span><textarea v-model.trim="reviewForm.reason" class="filter-textarea" placeholder="请输入驳回原因"></textarea></label>
                  <button class="button button-danger submit-action" type="button" :disabled="actionLoading" @click="completeAttendant('reject')">驳回审核</button>
                </article>
              </div>
              <div v-else class="empty-card inline-empty">当前任务正在分配，请稍候。</div>
            </section>

            <section class="workspace-section">
              <div class="section-heading compact-heading">
                <div>
                  <h3 class="section-title">陪诊师资料</h3>
                </div>
              </div>
              <div class="order-full-grid">
                <div v-for="item in attendantInfoItems" :key="item.label" class="kv-item">
                  <p class="kv-label">{{ item.label }}</p>
                  <p class="kv-value">{{ item.value }}</p>
                </div>
              </div>
            </section>
          </template>

          <template v-else>
            <div class="empty-card">请选择左侧任务开始处理。</div>
          </template>
        </main>
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
const activeQualificationKey = ref('id-front')
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
const activeQualificationCard = computed(() => {
  const cards = qualificationCards.value || []
  return cards.find((card) => card.key === activeQualificationKey.value && card.url) || cards.find((card) => card.url) || cards[0] || null
})
const orderInfoItems = computed(() => {
  const item = order.value || {}
  return [
    { label: '订单编号', value: item.orderNo || '-' },
    { label: '下单用户', value: `${orderDetail.value?.user?.name || item.userName || '-'} / ${orderDetail.value?.user?.phone || item.userPhone || '-'}` },
    { label: '患者信息', value: `${item.patientName || '-'} / ${item.patientSex || '-'} / ${item.patientAge || '-'}` },
    { label: '联系人', value: `${item.contactPerson || '-'} / ${item.contactPhone || '-'}` },
    { label: '陪诊师', value: `${orderDetail.value?.attendant?.name || item.attendantName || '暂未接单'} / ${orderDetail.value?.attendant?.phone || item.attendantPhone || '-'}` },
    { label: '医院', value: item.hospital || '-' },
    { label: '服务内容', value: item.serviceContent || '-' },
    { label: '服务日期', value: `${item.serviceDate || '-'} ${item.serviceTimeSlot || ''}` },
    { label: '接单时间', value: formatDateTime(item.acceptTime) },
    { label: '服务开始', value: formatDateTime(item.serviceStartTime) },
    { label: '服务结束', value: formatDateTime(item.serviceEndTime) },
    { label: '预估时长', value: formatDurationHour(item.estimatedDuration) },
    { label: '实际时长', value: formatDurationHour(item.actualDuration) },
    { label: '用户认可时长', value: formatDurationHour(item.timeDisputeUserDuration) },
    { label: '支付时间', value: formatDateTime(item.paymentTime) },
    { label: '特殊需求', value: item.specialRequirements || item.customRequirement || '-' },
    { label: '后台备注', value: item.adminRemark || '-' },
    { label: '争议处理时间', value: formatDateTime(item.disputeResolvedTime) }
  ]
})
const attendantInfoItems = computed(() => {
  const user = attendantDetail.value?.user || {}
  const item = attendantDetail.value?.attendant || {}
  return [
    { label: '姓名 / 手机号', value: `${user.name || '-'} / ${user.phone || '-'}` },
    { label: '常驻医院', value: item.hospitalName || '-' },
    { label: '擅长领域', value: item.professionalField || '-' },
    { label: '从业年限', value: `${item.experienceYears || 0} 年` },
    { label: '服务单量', value: `${item.serviceCount || 0} 单` },
    { label: '历史完成', value: `${attendantDetail.value?.completedOrderCount || 0} / ${attendantDetail.value?.totalOrderCount || 0}` },
    { label: '评分', value: item.score || '-' },
    { label: '简介', value: item.introduction || '-' }
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
      const firstUploaded = qualificationCards.value.find((card) => card.url)
      activeQualificationKey.value = firstUploaded?.key || 'id-front'
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
  min-width: 0;
}

.workbench-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  overflow: visible;
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
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.workbench-tab.active {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
  box-shadow: 0 12px 24px rgba(42, 120, 255, 0.18);
}

.workbench-layout {
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  min-width: 0;
}

.workbench-queue,
.workbench-workspace {
  min-height: 680px;
}

.workbench-queue {
  position: sticky;
  top: 18px;
  align-self: start;
  max-height: calc(100vh - 42px);
  overflow: auto;
}

.workbench-workspace {
  display: grid;
  gap: 14px;
  align-content: start;
  min-width: 0;
  overflow: hidden;
}

.task-list {
  display: grid;
  gap: 10px;
}

.workbench-queue .pagination {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(220, 232, 251, 0.72);
}

.workbench-queue .pagination-controls {
  gap: 10px;
}

.task-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: #fff;
  text-align: left;
  transition: border-color 180ms ease, background-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.task-item > .badge {
  justify-self: start;
}

.task-item.active {
  border-color: rgba(42, 120, 255, 0.42);
  background: rgba(237, 244, 255, 0.92);
  box-shadow: 0 12px 24px rgba(42, 120, 255, 0.12);
  transform: translateY(-1px);
}

.task-item.locked {
  opacity: 0.62;
}

.task-title {
  margin: 0;
  color: var(--text);
  font-weight: 800;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.task-copy {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.workspace-status {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(173, 199, 232, 0.74);
  border-radius: 22px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.workspace-kicker {
  margin: 0 0 6px;
  color: var(--primary);
  font-size: 12px;
  font-weight: 800;
}

.workspace-lock {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 180px;
}

.lock-pill {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 12px;
  color: var(--primary-deep);
  background: var(--primary-soft);
  border: 1px solid rgba(42, 120, 255, 0.18);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.workspace-section {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.compact-heading {
  margin-top: 2px;
}

.evidence-grid,
.dispute-focus-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.evidence-card,
.dispute-focus-card {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface-soft);
  min-width: 0;
}

.dispute-focus-card {
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 148px;
}

.dispute-focus-card.attendant-side {
  border-color: rgba(42, 120, 255, 0.18);
  background: linear-gradient(180deg, #fff 0%, #edf4ff 100%);
}

.dispute-focus-card.user-side {
  border-color: rgba(228, 85, 85, 0.18);
  background: linear-gradient(180deg, #fff 0%, #fff4f4 100%);
}

.dispute-focus-card.result-side {
  border-color: rgba(26, 167, 114, 0.2);
  background: linear-gradient(180deg, #fff 0%, rgba(26, 167, 114, 0.08) 100%);
}

.evidence-card strong,
.evidence-card span,
.dispute-focus-card strong,
.dispute-focus-card span {
  display: block;
  overflow-wrap: anywhere;
}

.dispute-focus-card strong {
  font-size: 24px;
  line-height: 1.2;
}

.dispute-focus-card span {
  color: var(--text);
  font-weight: 700;
  line-height: 1.5;
}

.dispute-focus-copy {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.55;
}

.evidence-label,
.dispute-focus-label {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.summary-pill.danger {
  color: var(--danger);
  background: rgba(228, 85, 85, 0.08);
}

.action-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(173, 199, 232, 0.8);
  background: #f8fbff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.action-form {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.dispute-action-form {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  align-items: end;
}

.review-action-form {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  align-items: end;
}

.review-decision-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(320px, 1.2fr);
  gap: 14px;
  align-items: stretch;
  min-width: 0;
}

.decision-card {
  display: grid;
  gap: 14px;
  align-content: space-between;
  min-width: 0;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(184, 208, 246, 0.84);
  background: #fff;
}

.decision-card-approve {
  background: linear-gradient(180deg, #ffffff 0%, #edf4ff 100%);
  border-color: rgba(42, 120, 255, 0.22);
}

.decision-card-approve.disabled {
  background: linear-gradient(180deg, #fff 0%, rgba(228, 85, 85, 0.06) 100%);
  border-color: rgba(228, 85, 85, 0.18);
}

.decision-card-reject {
  background: linear-gradient(180deg, #fff 0%, #fff7f7 100%);
  border-color: rgba(228, 85, 85, 0.16);
}

.decision-label {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.decision-title {
  margin: 0;
  color: var(--text);
  font-size: 18px;
  line-height: 1.35;
}

.decision-copy {
  margin: 8px 0 0;
  color: var(--text-muted);
  line-height: 1.55;
}

.mode-grid {
  align-items: center;
}

.mode-grid .button {
  min-width: 86px;
  white-space: nowrap;
}

.action-remark .filter-textarea {
  min-height: 46px;
  resize: vertical;
}

.submit-action,
.approve-action {
  min-height: 46px;
  white-space: nowrap;
  width: 100%;
}

.inline-empty {
  padding: 14px;
  box-shadow: none;
}

.qualification-review-stage {
  display: grid;
  gap: 14px;
}

.qualification-main-viewer {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border-radius: 20px;
  border: 1px solid rgba(184, 208, 246, 0.88);
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.qualification-main-viewer.missing,
.qualification-main-viewer.expired {
  border-color: rgba(228, 85, 85, 0.24);
  background: linear-gradient(180deg, #fff8f8 0%, #ffffff 100%);
}

.qualification-main-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.qualification-main-head .button {
  flex: 0 0 auto;
}

.qualification-main-image-button,
.qualification-main-placeholder {
  width: 100%;
  height: clamp(320px, 38vw, 520px);
  border: 1px solid rgba(184, 208, 246, 0.78);
  border-radius: 18px;
  background:
    linear-gradient(45deg, rgba(226, 238, 255, 0.5) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(226, 238, 255, 0.5) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, rgba(226, 238, 255, 0.5) 75%),
    linear-gradient(-45deg, transparent 75%, rgba(226, 238, 255, 0.5) 75%),
    #fff;
  background-size: 20px 20px;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0;
  overflow: hidden;
}

.qualification-main-image-button {
  padding: 0;
  cursor: zoom-in;
}

.qualification-main-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qualification-main-placeholder {
  display: grid;
  place-items: center;
  color: var(--text-muted);
  font-weight: 800;
  border-style: dashed;
}

.review-snapshot-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.review-snapshot-card {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border-radius: 18px;
  border: 1px solid rgba(184, 208, 246, 0.84);
  background: #f8fbff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.review-snapshot-card.active {
  border-color: rgba(37, 99, 235, 0.52);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.review-snapshot-card.missing,
.review-snapshot-card.expired {
  border-color: rgba(228, 85, 85, 0.22);
  background: #fff8f8;
}

.review-snapshot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.review-snapshot-head p,
.review-snapshot-date {
  margin: 0;
}

.review-snapshot-head p {
  color: var(--text);
  font-weight: 800;
}

.review-snapshot-head span {
  flex: 0 0 auto;
  min-height: 24px;
  border-radius: 999px;
  padding: 4px 10px;
  color: var(--success);
  background: rgba(26, 167, 114, 0.1);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.review-snapshot-card.missing .review-snapshot-head span,
.review-snapshot-card.expired .review-snapshot-head span {
  color: var(--danger);
  background: rgba(228, 85, 85, 0.1);
}

.review-snapshot-image-button,
.review-snapshot-placeholder {
  width: 100%;
  height: 118px;
  border: 1px solid rgba(184, 208, 246, 0.8);
  border-radius: 16px;
  background: #fff;
  overflow: hidden;
}

.review-snapshot-image-button {
  padding: 0;
  cursor: pointer;
}

.review-snapshot-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.review-snapshot-placeholder {
  display: grid;
  place-items: center;
  color: var(--text-muted);
  border-style: dashed;
}

.review-snapshot-date {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.order-full-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}

.full-width {
  width: 100%;
}

.auto-renew-copy {
  margin: 2px 0 0;
}

@media (max-width: 1280px) {
  .workbench-layout {
    grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  }

  .evidence-grid,
  .dispute-focus-grid,
  .dispute-action-form,
  .review-action-form,
  .review-decision-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-snapshot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
  .workbench-workspace {
    min-height: 0;
    position: static;
    max-height: none;
    overflow: visible;
  }

  .workspace-status,
  .evidence-grid,
  .dispute-focus-grid,
  .dispute-action-form,
  .review-action-form,
  .review-decision-grid,
  .review-snapshot-grid {
    grid-template-columns: 1fr;
  }

  .workspace-status {
    display: grid;
  }

  .qualification-main-head {
    align-items: stretch;
    flex-direction: column;
  }

  .qualification-main-head .button {
    width: 100%;
  }

  .qualification-main-image-button,
  .qualification-main-placeholder {
    height: 260px;
  }

  .workspace-lock {
    justify-content: flex-start;
    min-width: 0;
  }
}
</style>
