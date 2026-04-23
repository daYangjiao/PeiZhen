<template>
  <AppShell title="陪诊师管理" subtitle="资质审核、账号状态与服务能力">
    <div class="page-stack">
      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">筛选条件</h3>
            <p class="section-copy">按关键词和审核状态查看陪诊师资料。</p>
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
            <button class="button button-primary" type="button" @click="submitFilters" :disabled="loading">查询</button>
            <button class="button button-ghost" type="button" @click="resetFilters" :disabled="loading">重置</button>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">陪诊师列表</h3>
            <p class="section-copy">支持详情查看、审核通过 / 驳回、封禁与恢复。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="attendants.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th class="id-cell">ID</th>
                  <th>陪诊师</th>
                  <th>常驻医院</th>
                  <th>擅长领域</th>
                  <th>评分 / 年限</th>
                  <th class="status-cell">审核状态</th>
                  <th class="status-cell">账号状态</th>
                  <th>服务单量</th>
                  <th class="time-cell">更新时间</th>
                  <th class="actions-cell">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in attendants" :key="item.id">
                  <td class="id-cell">{{ item.id }}</td>
                  <td>
                    <p class="table-cell-title">{{ item.name || '未命名陪诊师' }}</p>
                    <p class="table-cell-copy">{{ item.phone || '-' }}</p>
                  </td>
                  <td>{{ item.hospitalName || '-' }}</td>
                  <td>{{ item.professionalField || '-' }}</td>
                  <td>{{ item.score ?? '-' }} / {{ item.experienceYears || 0 }} 年</td>
                  <td class="status-cell"><span class="badge" :class="getAttendantStatusBadge(item.status)">{{ getAttendantStatusLabel(item.status, item.statusLabel || '--') }}</span></td>
                  <td class="status-cell"><span class="badge" :class="getUserStatusBadge(item.userStatus)">{{ getUserStatusLabel(item.userStatus, item.userStatusLabel || '--') }}</span></td>
                  <td>{{ item.serviceCount || 0 }}</td>
                  <td class="time-cell">{{ formatDateTime(item.updateTime || item.createTime) }}</td>
                  <td class="actions-cell">
                    <div class="table-actions">
                      <button class="button button-secondary" type="button" @click="goToDetail(item.id)">查看详情</button>
                      <button
                        v-if="item.status === 0"
                        class="button button-primary"
                        type="button"
                        @click="openActionDialog(item, 'approve')"
                      >
                        通过
                      </button>
                      <button
                        v-if="item.status === 0"
                        class="button button-danger"
                        type="button"
                        @click="openActionDialog(item, 'reject')"
                      >
                        驳回
                      </button>
                      <button
                        v-if="item.status === 1"
                        class="button button-danger"
                        type="button"
                        @click="openActionDialog(item, 'ban')"
                      >
                        封禁
                      </button>
                      <button
                        v-if="item.status === 2"
                        class="button button-primary"
                        type="button"
                        @click="openActionDialog(item, 'restore-status')"
                      >
                        恢复
                      </button>
                      <button
                        v-if="item.status === 3"
                        class="button button-primary"
                        type="button"
                        @click="openActionDialog(item, 'restore-review')"
                      >
                        重新通过
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">当前没有符合条件的陪诊师。</div>
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

    <BaseDialog
      v-model="actionDialogOpen"
      :title="actionMeta.title"
      :description="actionMeta.description"
      width="560px"
    >
      <div class="page-stack">
        <div class="kv-grid" v-if="selectedAttendant">
          <div class="kv-item">
            <p class="kv-label">陪诊师</p>
            <p class="kv-value">{{ selectedAttendant.name || selectedAttendant.phone || selectedAttendant.id }}</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前状态</p>
            <p class="kv-value">{{ getAttendantStatusLabel(selectedAttendant.status, selectedAttendant.statusLabel || '--') }}</p>
          </div>
        </div>

        <label v-if="actionMeta.needReason" class="login-field">
          <span>{{ actionMeta.reasonLabel }}</span>
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
  </AppShell>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useUiStore } from '../stores/ui'
import { fetchAttendants, reviewAttendant, updateAttendantStatus } from '../utils/admin-api'
import { attendantStatusOptions, getAttendantStatusBadge, getAttendantStatusLabel, getUserStatusBadge, getUserStatusLabel, toQueryValue } from '../utils/admin-view'
import { formatDateTime } from '../utils/format'

const router = useRouter()
const route = useRoute()
const uiStore = useUiStore()

const filters = reactive({
  keyword: '',
  auditStatus: ''
})

const attendants = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)

const actionDialogOpen = ref(false)
const actionLoading = ref(false)
const selectedAttendant = ref(null)
const actionType = ref('approve')
const actionReason = ref('')

const readQueryValue = (queryValue) => (Array.isArray(queryValue) ? queryValue[0] : queryValue)

const toFilterValue = (queryValue) => {
  const value = readQueryValue(queryValue)
  if (value === '' || value === undefined || value === null) return ''
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : ''
}

const applyQueryFilters = (query) => {
  filters.keyword = typeof readQueryValue(query.keyword) === 'string' ? readQueryValue(query.keyword) : ''
  filters.auditStatus = toFilterValue(query.auditStatus)

  const quick = readQueryValue(query.quick)
  if (quick === 'pending-review' && filters.auditStatus === '') {
    filters.auditStatus = 0
  }
}

const actionMeta = computed(() => {
  const map = {
    approve: {
      title: '通过审核',
      description: '将该陪诊师审核通过并恢复为正常状态。',
      confirmText: '确认通过',
      confirmTone: 'button-primary',
      needReason: false
    },
    reject: {
      title: '驳回审核',
      description: '驳回会把陪诊师状态改为审核失败，并记录原因。',
      confirmText: '确认驳回',
      confirmTone: 'button-danger',
      needReason: true,
      reasonLabel: '驳回原因',
      reasonPlaceholder: '请输入审核驳回原因'
    },
    ban: {
      title: '封禁陪诊师',
      description: '封禁后该陪诊师无法以正常状态参与服务。',
      confirmText: '确认封禁',
      confirmTone: 'button-danger',
      needReason: true,
      reasonLabel: '封禁原因',
      reasonPlaceholder: '请输入封禁原因'
    },
    'restore-status': {
      title: '恢复陪诊师',
      description: '将封禁状态恢复为正常。',
      confirmText: '确认恢复',
      confirmTone: 'button-primary',
      needReason: false
    },
    'restore-review': {
      title: '重新通过审核',
      description: '将审核失败的陪诊师重新恢复为通过状态。',
      confirmText: '确认通过',
      confirmTone: 'button-primary',
      needReason: false
    }
  }
  return map[actionType.value]
})

const loadAttendants = async () => {
  loading.value = true
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
  } catch (error) {
    uiStore.toast(error.message || '陪诊师列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = () => {
  page.value = 0
  loadAttendants()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.auditStatus = ''
  submitFilters()
}

const changePage = (nextPage) => {
  page.value = nextPage
  loadAttendants()
}

const changePageSize = () => {
  page.value = 0
  loadAttendants()
}

const goToDetail = (id) => {
  router.push(`/attendants/${id}`)
}

const openActionDialog = (item, type) => {
  selectedAttendant.value = item
  actionType.value = type
  actionReason.value = ''
  actionDialogOpen.value = true
}

const closeActionDialog = () => {
  actionDialogOpen.value = false
  actionReason.value = ''
}

const submitAction = async () => {
  if (!selectedAttendant.value) return
  if (actionMeta.value.needReason && !actionReason.value) {
    uiStore.toast('请填写处理原因', 'error')
    return
  }

  actionLoading.value = true
  try {
    if (actionType.value === 'approve') {
      await reviewAttendant(selectedAttendant.value.id, { action: 'approve' })
    } else if (actionType.value === 'reject') {
      await reviewAttendant(selectedAttendant.value.id, { action: 'reject', reason: actionReason.value })
    } else if (actionType.value === 'ban') {
      await updateAttendantStatus(selectedAttendant.value.id, { status: 2, reason: actionReason.value })
    } else if (actionType.value === 'restore-status') {
      await updateAttendantStatus(selectedAttendant.value.id, { status: 1, reason: '' })
    } else if (actionType.value === 'restore-review') {
      await reviewAttendant(selectedAttendant.value.id, { action: 'restore' })
    }

    uiStore.toast('陪诊师状态更新成功', 'success')
    closeActionDialog()
    await loadAttendants()
  } catch (error) {
    uiStore.toast(error.message || '陪诊师状态更新失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => route.query,
  (query) => {
    applyQueryFilters(query)
    page.value = 0
    loadAttendants()
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

@media (max-width: 720px) {
  .filter-fields-row {
    grid-template-columns: 1fr;
  }
}
</style>
