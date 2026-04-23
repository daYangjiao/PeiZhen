<template>
  <AppShell title="陪诊师审核" subtitle="入驻资料审核">
    <div class="page-stack">
      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">筛选条件</h3>
            <p class="section-copy">按姓名、手机号、医院和审核状态筛选。</p>
          </div>
        </div>
        <div class="toolbar filter-toolbar">
          <div class="toolbar-group filter-fields-row">
            <input v-model.trim="filters.keyword" class="field-inline" type="text" placeholder="姓名、手机号或 ID" @keyup.enter="submitFilters" />
            <select v-model="filters.auditStatus" class="filter-select">
              <option v-for="option in attendantStatusOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
          <div class="toolbar-group filter-actions-row">
            <button class="button button-primary" type="button" :disabled="queueLoading" @click="submitFilters">查询</button>
            <button class="button button-ghost" type="button" :disabled="queueLoading" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>

      <section class="workbench-grid">
        <article class="panel-card queue-panel">
          <div class="section-heading queue-heading">
            <div>
              <h3 class="section-title">审核队列</h3>
              <p class="section-copy">共 {{ total }} 条，当前第 {{ page + 1 }} / {{ totalPages }} 页</p>
            </div>
          </div>

          <div v-if="queueLoading" class="skeleton"></div>
          <div v-else-if="attendants.length" class="queue-list">
            <button
              v-for="item in attendants"
              :key="item.id"
              class="queue-item"
              :class="{ 'is-selected': selectedId === item.id }"
              type="button"
              @click="selectQueueItem(item.id)"
            >
              <div class="queue-item-main">
                <p class="queue-title">{{ item.name || item.phone || `陪诊师 ${item.id}` }}</p>
                <p class="queue-copy">{{ item.phone || '-' }} · {{ item.hospitalName || '未填写常驻医院' }}</p>
                <p class="queue-copy">ID {{ item.id }} · 更新时间 {{ formatDateTime(item.updateTime || item.createTime) }}</p>
              </div>
              <div class="queue-item-statuses">
                <span class="badge" :class="getAttendantStatusBadge(item.status)">{{ getAttendantStatusLabel(item.status, item.statusLabel || '--') }}</span>
                <span class="badge" :class="getUserStatusBadge(item.userStatus)">{{ getUserStatusLabel(item.userStatus, item.userStatusLabel || '--') }}</span>
              </div>
            </button>
          </div>
          <div v-else class="empty-card">当前筛选条件下没有待查看的陪诊师。</div>

          <div class="pagination">
            <div class="pagination-controls">
              <select v-model.number="pageSize" class="filter-select" @change="changePageSize">
                <option :value="10">10 条 / 页</option>
                <option :value="20">20 条 / 页</option>
                <option :value="50">50 条 / 页</option>
              </select>
              <button class="button button-ghost" type="button" :disabled="page === 0 || queueLoading" @click="changePage(page - 1)">上一页</button>
              <button class="button button-ghost" type="button" :disabled="page + 1 >= totalPages || queueLoading" @click="changePage(page + 1)">下一页</button>
            </div>
          </div>
        </article>

        <article class="panel-card detail-panel">
          <template v-if="detailLoading">
            <div class="skeleton"></div>
          </template>
          <template v-else-if="detail">
            <div class="section-heading">
              <div>
                <h3 class="section-title">{{ detail.user.name || '未命名陪诊师' }}</h3>
                <p class="section-copy">{{ detail.user.phone || '-' }} · {{ detail.user.sex || '未知' }} · {{ detail.user.age || '-' }} 岁</p>
              </div>
              <div class="toolbar-group">
                <span class="badge" :class="getAttendantStatusBadge(detail.attendant.status)">{{ getAttendantStatusLabel(detail.attendant.status, '--') }}</span>
                <span class="badge" :class="getUserStatusBadge(detail.user.status)">{{ getUserStatusLabel(detail.user.status, '--') }}</span>
                <button class="button button-secondary" type="button" @click="openFullDetail(detail.user.id)">完整详情</button>
              </div>
            </div>

            <div class="kv-grid">
              <div class="kv-item">
                <p class="kv-label">常驻医院</p>
                <p class="kv-value">{{ detail.attendant.hospitalName || '-' }}</p>
              </div>
              <div class="kv-item">
                <p class="kv-label">擅长领域</p>
                <p class="kv-value">{{ detail.attendant.professionalField || '-' }}</p>
              </div>
              <div class="kv-item">
                <p class="kv-label">从业年限</p>
                <p class="kv-value">{{ detail.attendant.experienceYears || 0 }} 年</p>
              </div>
              <div class="kv-item">
                <p class="kv-label">评分 / 服务单量</p>
                <p class="kv-value">{{ detail.attendant.score || '-' }} / {{ detail.attendant.serviceCount || 0 }} 单</p>
              </div>
            </div>

            <div class="section-heading detail-actions-heading">
              <div>
                <h4 class="section-title">审核处理</h4>
                <p class="section-copy">审核入驻资料并处理账号状态。</p>
              </div>
              <div class="toolbar-group">
                <button v-if="detail.attendant.status === 0" class="button button-primary" type="button" @click="openActionDialog('approve')">通过审核</button>
                <button v-if="detail.attendant.status === 0" class="button button-danger" type="button" @click="openActionDialog('reject')">驳回审核</button>
                <button v-if="detail.attendant.status === 1" class="button button-danger" type="button" @click="openActionDialog('ban')">封禁</button>
                <button v-if="detail.attendant.status === 2" class="button button-primary" type="button" @click="openActionDialog('restore-status')">恢复</button>
                <button v-if="detail.attendant.status === 3" class="button button-primary" type="button" @click="openActionDialog('restore-review')">重新通过</button>
              </div>
            </div>

            <div class="qualification-grid">
              <article v-for="card in qualificationCards" :key="card.key" class="qualification-card">
                <p class="qualification-title">{{ card.title }}</p>
                <button v-if="card.url" class="qualification-thumb-button" type="button" @click="openPreview(card)">
                  <img :src="card.url" :alt="card.title" class="qualification-thumb-image" />
                </button>
                <div v-else class="qualification-thumb-placeholder">未上传</div>
                <p class="qualification-status" :class="card.url ? 'is-uploaded' : 'is-missing'">
                  {{ card.url ? '已上传' : '缺失' }}
                </p>
              </article>
            </div>
          </template>
          <template v-else>
            <div class="empty-card">请先从左侧队列选择一位陪诊师。</div>
          </template>
        </article>
      </section>
    </div>

    <BaseDialog v-model="actionDialogOpen" :title="actionMeta.title" :description="actionMeta.description" width="560px">
      <div class="page-stack">
        <div class="kv-grid" v-if="detail">
          <div class="kv-item">
            <p class="kv-label">陪诊师</p>
            <p class="kv-value">{{ detail.user.name || detail.user.phone || detail.user.id }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前状态</p>
            <p class="kv-value">{{ getAttendantStatusLabel(detail.attendant.status, '--') }}</p>
          </div>
        </div>

        <label v-if="actionMeta.needReason" class="login-field">
          <span>{{ actionMeta.reasonLabel }}</span>
          <div class="reason-chip-row">
            <button
              v-for="chip in reasonChips"
              :key="chip"
              class="button button-secondary reason-chip"
              type="button"
              @click="applyReasonChip(chip)"
            >
              {{ chip }}
            </button>
          </div>
          <textarea v-model.trim="actionReason" class="filter-textarea" :placeholder="actionMeta.reasonPlaceholder"></textarea>
        </label>
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="closeActionDialog">取消</button>
        <button class="button" :class="actionMeta.confirmTone" type="button" :disabled="actionLoading" @click="submitAction">
          {{ actionLoading ? '提交中...' : actionMeta.confirmText }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="previewDialogOpen" :title="previewTitle || '资质文件预览'" description="资质材料预览。" width="760px">
      <div class="qualification-preview">
        <img v-if="previewImageUrl" :src="previewImageUrl" :alt="previewTitle || '资质文件'" class="qualification-preview-image" />
      </div>
      <template #footer>
        <button class="button button-ghost" type="button" @click="previewDialogOpen = false">关闭</button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useAttendantReviewActions } from '../composables/useAttendantReviewActions'
import { useUiStore } from '../stores/ui'
import { fetchAttendantDetail, fetchAttendants } from '../utils/admin-api'
import { attendantStatusOptions, getAttendantStatusBadge, getAttendantStatusLabel, getUserStatusBadge, getUserStatusLabel, toQueryValue } from '../utils/admin-view'
import { formatDateTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const filters = reactive({
  keyword: '',
  auditStatus: ''
})

const attendants = ref([])
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)
const selectedId = ref(null)
const queueLoading = ref(true)
const detailLoading = ref(false)
const detail = ref(null)

const actionDialogOpen = ref(false)
const actionType = ref('approve')
const actionReason = ref('')
const previewDialogOpen = ref(false)
const previewImageUrl = ref('')
const previewTitle = ref('')

const ignoreNextQueryWatch = ref(false)

const { actionLoading, getActionMeta, getReasonChips, runAttendantAction } = useAttendantReviewActions(uiStore)

const actionMeta = computed(() => getActionMeta(actionType.value))
const reasonChips = computed(() => getReasonChips(actionType.value))

const qualificationCards = computed(() => [
  {
    key: 'id-front',
    title: '身份证正面',
    url: detail.value?.qualification?.idCardFrontFileUrl || ''
  },
  {
    key: 'id-back',
    title: '身份证反面',
    url: detail.value?.qualification?.idCardBackFileUrl || ''
  },
  {
    key: 'practice-cert',
    title: '执业证书',
    url: detail.value?.qualification?.practiceCertFileUrl || ''
  },
  {
    key: 'health-cert',
    title: '健康证',
    url: detail.value?.qualification?.healthCertFileUrl || ''
  }
])

const readQueryValue = (value) => (Array.isArray(value) ? value[0] : value)

const parseNumberQuery = (value, fallback) => {
  const raw = readQueryValue(value)
  const numeric = Number(raw)
  return Number.isFinite(numeric) ? numeric : fallback
}

const parseAuditStatusQuery = (value) => {
  const raw = readQueryValue(value)
  if (raw === undefined || raw === null || raw === '') return ''
  const numeric = Number(raw)
  return Number.isFinite(numeric) ? numeric : ''
}

const parseSelectedIdQuery = (value) => {
  const raw = readQueryValue(value)
  if (raw === undefined || raw === null || raw === '') return null
  const numeric = Number(raw)
  return Number.isFinite(numeric) ? numeric : null
}

const normalizeQuery = (query) => {
  const keyword = typeof readQueryValue(query.keyword) === 'string' ? readQueryValue(query.keyword).trim() : ''
  const auditStatus = parseAuditStatusQuery(query.auditStatus)
  const parsedPage = parseNumberQuery(query.page, 0)
  const parsedPageSize = parseNumberQuery(query.pageSize, 10)
  const selected = parseSelectedIdQuery(query.selectedId)
  const quick = readQueryValue(query.quick)

  filters.keyword = keyword
  filters.auditStatus = auditStatus
  if (quick === 'pending-review' && filters.auditStatus === '') {
    filters.auditStatus = 0
  }
  page.value = Math.max(parsedPage, 0)
  pageSize.value = [10, 20, 50].includes(parsedPageSize) ? parsedPageSize : 10
  selectedId.value = selected
}

const isSameQuery = (nextQuery) => {
  const current = route.query
  const currentKeys = Object.keys(current)
  const nextKeys = Object.keys(nextQuery)
  if (currentKeys.length !== nextKeys.length) return false
  return nextKeys.every((key) => String(readQueryValue(current[key]) ?? '') === String(nextQuery[key] ?? ''))
}

const buildQuery = (overrides = {}) => {
  const query = {
    page: String(page.value),
    pageSize: String(pageSize.value)
  }
  if (filters.keyword) query.keyword = filters.keyword
  if (filters.auditStatus !== '') query.auditStatus = String(filters.auditStatus)
  if (selectedId.value !== null && selectedId.value !== undefined && selectedId.value !== '') query.selectedId = String(selectedId.value)

  if (overrides.keyword !== undefined) {
    if (overrides.keyword) query.keyword = String(overrides.keyword)
    else delete query.keyword
  }
  if (overrides.auditStatus !== undefined) {
    if (overrides.auditStatus === '') delete query.auditStatus
    else query.auditStatus = String(overrides.auditStatus)
  }
  if (overrides.page !== undefined) query.page = String(overrides.page)
  if (overrides.pageSize !== undefined) query.pageSize = String(overrides.pageSize)
  if (overrides.selectedId !== undefined) {
    if (overrides.selectedId === null || overrides.selectedId === '') delete query.selectedId
    else query.selectedId = String(overrides.selectedId)
  }
  return query
}

const syncQuery = async (overrides = {}) => {
  const nextQuery = buildQuery(overrides)
  if (isSameQuery(nextQuery)) return false
  ignoreNextQueryWatch.value = true
  await router.replace({ query: nextQuery })
  return true
}

const resolveSelectedId = (candidateId, list) => {
  if (!list.length) return null
  if (candidateId !== null && list.some((item) => item.id === candidateId)) return candidateId
  return list[0].id
}

const loadDetail = async (id) => {
  if (!id) {
    detail.value = null
    return
  }
  detailLoading.value = true
  try {
    detail.value = await fetchAttendantDetail(id)
  } catch (error) {
    detail.value = null
    uiStore.toast(error.message || '陪诊师详情加载失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

const loadWorkbench = async ({ preferredSelectedId } = {}) => {
  queueLoading.value = true
  try {
    const response = await fetchAttendants({
      keyword: filters.keyword,
      auditStatus: toQueryValue(filters.auditStatus),
      page: page.value,
      pageSize: pageSize.value
    })
    attendants.value = response.content || []
    total.value = response.totalElements || 0
    totalPages.value = Math.max(response.totalPages || 1, 1)

    const resolved = resolveSelectedId(preferredSelectedId ?? selectedId.value, attendants.value)
    const changedSelection = resolved !== selectedId.value
    selectedId.value = resolved

    if (changedSelection) {
      await syncQuery({ selectedId: resolved })
    }
    await loadDetail(resolved)
  } catch (error) {
    attendants.value = []
    total.value = 0
    totalPages.value = 1
    selectedId.value = null
    detail.value = null
    uiStore.toast(error.message || '陪诊师列表加载失败', 'error')
  } finally {
    queueLoading.value = false
  }
}

const selectQueueItem = async (id) => {
  selectedId.value = id
  const changed = await syncQuery({ selectedId: id })
  if (!changed) {
    await loadDetail(id)
  }
}

const submitFilters = async () => {
  page.value = 0
  selectedId.value = null
  const changed = await syncQuery({ page: 0, selectedId: null })
  if (!changed) {
    await loadWorkbench({ preferredSelectedId: null })
  }
}

const resetFilters = async () => {
  filters.keyword = ''
  filters.auditStatus = ''
  page.value = 0
  selectedId.value = null
  const changed = await syncQuery({
    keyword: '',
    auditStatus: '',
    page: 0,
    selectedId: null
  })
  if (!changed) {
    await loadWorkbench({ preferredSelectedId: null })
  }
}

const changePage = async (nextPage) => {
  page.value = nextPage
  selectedId.value = null
  const changed = await syncQuery({ page: nextPage, selectedId: null })
  if (!changed) {
    await loadWorkbench({ preferredSelectedId: null })
  }
}

const changePageSize = async () => {
  page.value = 0
  selectedId.value = null
  const changed = await syncQuery({ page: 0, pageSize: pageSize.value, selectedId: null })
  if (!changed) {
    await loadWorkbench({ preferredSelectedId: null })
  }
}

const openFullDetail = (id) => {
  router.push(`/attendants/${id}`)
}

const openActionDialog = (type) => {
  if (!detail.value?.user?.id) return
  actionType.value = type
  actionReason.value = ''
  actionDialogOpen.value = true
}

const closeActionDialog = () => {
  actionDialogOpen.value = false
  actionReason.value = ''
}

const applyReasonChip = (reason) => {
  actionReason.value = reason
}

const pickNextQueueId = (currentId) => {
  const index = attendants.value.findIndex((item) => item.id === currentId)
  if (index < 0) return null
  return attendants.value[index + 1]?.id || attendants.value[index - 1]?.id || null
}

const submitAction = async () => {
  const attendantId = detail.value?.user?.id
  if (!attendantId) return

  const nextQueueId = pickNextQueueId(attendantId)
  const result = await runAttendantAction({
    attendantId,
    actionType: actionType.value,
    reason: actionReason.value
  })
  if (!result.ok) {
    if (result.errorMessage === '请填写处理原因') {
      uiStore.toast(result.errorMessage, 'error')
    }
    return
  }

  uiStore.toast('处理成功', 'success')
  closeActionDialog()
  await loadWorkbench({ preferredSelectedId: nextQueueId })
}

const openPreview = (card) => {
  if (!card.url) return
  previewTitle.value = card.title
  previewImageUrl.value = card.url
  previewDialogOpen.value = true
}

watch(
  () => route.query,
  async (query) => {
    if (ignoreNextQueryWatch.value) {
      ignoreNextQueryWatch.value = false
      return
    }
    normalizeQuery(query)
    await loadWorkbench({ preferredSelectedId: selectedId.value })
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
  grid-template-columns: minmax(240px, 1.6fr) minmax(200px, 1fr);
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

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.45fr);
  gap: 16px;
}

.queue-panel,
.detail-panel {
  min-height: 680px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.queue-list {
  display: grid;
  gap: 10px;
  align-content: start;
}

.queue-item {
  width: 100%;
  text-align: left;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.queue-item:hover {
  border-color: #cbd5f5;
}

.queue-item.is-selected {
  border-color: #4463ff;
  box-shadow: 0 0 0 1px #4463ff inset;
  background: #f7f9ff;
}

.queue-item-main {
  display: grid;
  gap: 4px;
}

.queue-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.queue-copy {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}

.queue-item-statuses {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.queue-heading {
  margin-bottom: 4px;
}

.detail-actions-heading {
  margin-top: 8px;
}

.reason-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 6px 0 8px;
}

.reason-chip {
  font-size: 12px;
  padding: 6px 10px;
}

@media (max-width: 1080px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }

  .queue-panel,
  .detail-panel {
    min-height: 0;
  }
}

@media (max-width: 720px) {
  .filter-fields-row {
    grid-template-columns: 1fr;
  }
}
</style>
