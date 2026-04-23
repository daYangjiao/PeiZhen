<template>
  <AppShell title="陪诊师详情" subtitle="资质文件、服务能力与审核动作">
    <div class="page-stack">
      <section v-if="loading" class="skeleton"></section>

      <template v-else-if="detail">
        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">{{ detail.user.name || '未命名陪诊师' }}</h3>
              <p class="section-copy">{{ detail.user.phone || '-' }} · {{ detail.user.sex || '未知' }} · {{ detail.user.age || '-' }} 岁</p>
            </div>
            <div class="toolbar-group">
              <span class="badge" :class="getAttendantStatusBadge(detail.attendant.status)">{{ attendantStatusLabel }}</span>
              <span class="badge" :class="getUserStatusBadge(detail.user.status)">{{ getUserStatusLabel(detail.user.status, '--') }}</span>
              <button class="button button-ghost" type="button" @click="router.push({ path: '/attendants', query: { selectedId: String(detail.user.id) } })">返回列表</button>
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
            <div class="kv-item">
              <p class="kv-label">累计订单数</p>
              <p class="kv-value">{{ detail.totalOrderCount }}</p>
            </div>
            <div class="kv-item">
              <p class="kv-label">已完成订单数</p>
              <p class="kv-value">{{ detail.completedOrderCount }}</p>
            </div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">审核与账号处理</h3>
              <p class="section-copy">根据当前状态执行通过、驳回、封禁或恢复。</p>
            </div>
            <div class="toolbar-group">
              <button v-if="detail.attendant.status === 0" class="button button-primary" type="button" @click="openActionDialog('approve')">通过审核</button>
              <button v-if="detail.attendant.status === 0" class="button button-danger" type="button" @click="openActionDialog('reject')">驳回审核</button>
              <button v-if="detail.attendant.status === 1" class="button button-danger" type="button" @click="openActionDialog('ban')">封禁</button>
              <button v-if="detail.attendant.status === 2" class="button button-primary" type="button" @click="openActionDialog('restore-status')">恢复</button>
              <button v-if="detail.attendant.status === 3" class="button button-primary" type="button" @click="openActionDialog('restore-review')">重新通过</button>
            </div>
          </div>

          <div class="detail-row" v-if="detail.attendant.qualificationFailReason">
            <div class="detail-row-label">当前处理原因</div>
            <div class="detail-row-value">{{ detail.attendant.qualificationFailReason }}</div>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">资质文件</h3>
              <p class="section-copy">查看陪诊师资质材料。</p>
            </div>
          </div>
          <div class="qualification-grid">
            <article v-for="card in qualificationCards" :key="card.key" class="qualification-card">
              <p class="qualification-title">{{ card.title }}</p>
              <button
                v-if="card.url"
                class="qualification-thumb-button"
                type="button"
                @click="openPreview(card)"
              >
                <img :src="card.url" :alt="card.title" class="qualification-thumb-image" />
              </button>
              <div v-else class="qualification-thumb-placeholder">未上传</div>
              <p class="qualification-status" :class="card.url ? 'is-uploaded' : 'is-missing'">
                {{ card.url ? '已上传' : '缺失' }}
              </p>
            </article>
          </div>
        </section>

        <section class="panel-card">
          <div class="section-heading">
            <div>
              <h3 class="section-title">最近订单</h3>
            </div>
          </div>
          <div v-if="detail.recentOrders?.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>用户</th>
                  <th>服务信息</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in detail.recentOrders" :key="order.orderId">
                  <td>{{ order.orderNo }}</td>
                  <td>{{ order.userName || '-' }}</td>
                  <td>{{ order.hospital || '-' }}<br />{{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</td>
                  <td>{{ formatMoney(order.orderAmount) }}</td>
                  <td><span class="badge" :class="getOrderStatusBadge(order.orderStatus)">{{ getOrderStatusLabel(order.orderStatus, order.orderStatusLabel || '--') }}</span></td>
                  <td>
                    <button class="button button-secondary" type="button" @click="router.push(`/orders/${order.orderId}`)">查看订单</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">暂无最近订单。</div>
        </section>
      </template>

      <section v-else class="empty-card">未找到该陪诊师信息。</section>
    </div>

    <BaseDialog v-model="actionDialogOpen" :title="actionMeta.title" :description="actionMeta.description" width="560px">
      <div class="page-stack">
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
        <button class="button button-ghost" type="button" @click="actionDialogOpen = false">取消</button>
        <button class="button" :class="actionMeta.confirmTone" type="button" :disabled="actionLoading" @click="submitAction">
          {{ actionLoading ? '提交中...' : actionMeta.confirmText }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog
      v-model="previewDialogOpen"
      :title="previewTitle || '资质文件预览'"
      description="资质材料预览。"
      width="760px"
    >
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
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useAttendantReviewActions } from '../composables/useAttendantReviewActions'
import { useUiStore } from '../stores/ui'
import { fetchAttendantDetail } from '../utils/admin-api'
import { getAttendantStatusBadge, getAttendantStatusLabel, getOrderStatusBadge, getOrderStatusLabel, getUserStatusBadge, getUserStatusLabel } from '../utils/admin-view'
import { formatMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const loading = ref(true)
const detail = ref(null)

const actionDialogOpen = ref(false)
const actionType = ref('approve')
const actionReason = ref('')
const previewDialogOpen = ref(false)
const previewImageUrl = ref('')
const previewTitle = ref('')

const { actionLoading, getActionMeta, getReasonChips, runAttendantAction } = useAttendantReviewActions(uiStore)

const attendantStatusLabel = computed(() => getAttendantStatusLabel(detail.value?.attendant?.status, '--'))
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

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await fetchAttendantDetail(route.params.id)
  } catch (error) {
    uiStore.toast(error.message || '陪诊师详情加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const openActionDialog = (type) => {
  actionType.value = type
  actionReason.value = ''
  actionDialogOpen.value = true
}

const applyReasonChip = (reason) => {
  actionReason.value = reason
}

const submitAction = async () => {
  if (!detail.value) return
  const result = await runAttendantAction({
    attendantId: detail.value.user.id,
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
  actionDialogOpen.value = false
  await loadDetail()
}

const openPreview = (card) => {
  if (!card.url) return
  previewTitle.value = card.title
  previewImageUrl.value = card.url
  previewDialogOpen.value = true
}

watch(() => route.params.id, loadDetail, { immediate: true })
</script>

<style scoped>
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
</style>
