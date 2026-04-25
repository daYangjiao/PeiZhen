<template>
  <AppShell title="用户管理" subtitle="用户档案与账号状态">
    <div class="page-stack">
      <section class="panel-card filter-card">
        <div class="section-heading filter-heading">
          <div>
            <h3 class="section-title">用户筛选</h3>
          </div>
        </div>

        <div class="filter-board">
          <div class="filter-fields-row">
            <label class="filter-field">
              <span>关键词</span>
              <input v-model.trim="filters.keyword" class="field-inline" type="text" placeholder="姓名、手机号或 ID" @keyup.enter="submitFilters" />
            </label>
            <label class="filter-field">
              <span>账号类型</span>
              <select v-model="filters.userType" class="filter-select">
                <option v-for="option in userTypeOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span>账号状态</span>
              <select v-model="filters.status" class="filter-select">
                <option v-for="option in userStatusOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
              </select>
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
            <h3 class="section-title">用户列表</h3>
            <p class="section-copy">点击列表行或查看资料打开档案抽屉。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="users.length" class="record-list user-record-list">
            <article
              v-for="user in users"
              :key="user.id"
              class="record-item user-record-item"
              :class="{ 'selected-row': selectedUserId === user.id }"
              @click="openUserDrawer(user.id)"
            >
              <div class="record-main">
                <div class="record-summary">
                  <div class="user-info">
                    <img class="user-avatar" :src="getUserAvatar(user)" :alt="user.name || '用户头像'" />
                    <div class="user-info-copy">
                      <p class="record-title">{{ user.name || '未命名用户' }}</p>
                      <p class="record-copy">ID {{ user.id }} · {{ user.phone || '-' }}</p>
                      <p class="record-copy">{{ formatUserSex(user) }} / {{ formatUserAge(user) }}</p>
                    </div>
                  </div>
                  <div class="record-chip-row">
                    <span class="badge" :class="getUserStatusBadge(user.status)">{{ getUserStatusLabel(user.status, user.statusLabel || '--') }}</span>
                  </div>
                </div>

                <div class="record-meta-grid">
                  <div class="record-stat">
                    <p class="record-label">账号类型</p>
                    <p class="record-value">{{ getUserTypeLabel(user.userType, user.userTypeLabel || '--') }}</p>
                    <p v-if="isAttendantUser(user)" class="record-note">{{ getAttendantAuditStatus(user) }}</p>
                  </div>
                  <div class="record-stat">
                    <p class="record-label">订单摘要</p>
                    <p class="record-value">{{ getOrderCountValue(user) }} 单</p>
                    <p class="record-note">完成 {{ getCompletedOrderCountValue(user) }} 单</p>
                  </div>
                  <div class="record-stat">
                    <p class="record-label">注册时间</p>
                    <p class="record-value">{{ formatDateTime(getUserRegisterTime(user)) }}</p>
                    <p class="record-note">点击打开完整档案</p>
                  </div>
                </div>
              </div>

              <div class="record-side" @click.stop>
                <button class="button button-secondary" type="button" @click="openUserDrawer(user.id)">查看资料</button>
                <button class="button" :class="user.status === 1 ? 'button-danger' : 'button-primary'" type="button" @click="promptStatusChange(user)">
                  {{ user.status === 1 ? '禁用' : '恢复' }}
                </button>
              </div>
            </article>
          </div>
          <div v-else class="empty-card">未查询到符合条件的用户。</div>
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

      <BaseDrawer
        v-model="detailDrawerOpen"
        title="用户档案"
        :description="selectedUserSummary ? `${selectedUserSummary.name || '未命名用户'} · ${selectedUserSummary.phone || '无手机号'}` : '完整资料与账号操作'"
        width="880px"
      >
        <div class="section-heading">
          <div>
            <h3 class="section-title">完整资料</h3>
          </div>
          <div v-if="selectedUserSummary" class="toolbar-group">
            <span class="badge" :class="getUserStatusBadge(selectedUserSummary.status)">{{ getUserStatusLabel(selectedUserSummary.status, selectedUserSummary.statusLabel || '--') }}</span>
            <button
              class="button"
              :class="selectedUserSummary.status === 1 ? 'button-danger' : 'button-primary'"
              type="button"
              :disabled="actionLoading"
              @click="promptStatusChange(selectedUserSummary)"
            >
              {{ selectedUserSummary.status === 1 ? '禁用账号' : '恢复账号' }}
            </button>
          </div>
        </div>

        <div v-if="detailLoading" class="skeleton"></div>
        <template v-else-if="selectedUserDetail">
          <div class="page-stack">
            <section class="profile-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">基础信息</h4>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <p class="kv-label">姓名</p>
                  <p class="kv-value">{{ selectedUserDetail.user.name || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">手机号</p>
                  <p class="kv-value">{{ selectedUserDetail.user.phone || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">性别 / 年龄</p>
                  <p class="kv-value">{{ selectedUserDetail.user.sex || '未知' }} / {{ selectedUserDetail.user.age || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">角色</p>
                  <p class="kv-value">{{ getUserTypeLabel(selectedUserDetail.user.userType, selectedUserDetail.user.userTypeLabel || '--') }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">账号状态</p>
                  <p class="kv-value">{{ getUserStatusLabel(selectedUserDetail.user.status, selectedUserDetail.user.statusLabel || '--') }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">注册时间</p>
                  <p class="kv-value">{{ formatDateTime(getUserRegisterTime(selectedUserDetail.user)) }}</p>
                </div>
              </div>
            </section>

            <section class="profile-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">订单统计</h4>
              </div>
              <div class="amount-breakdown-grid">
                <div class="amount-breakdown-item">
                  <p class="amount-breakdown-label">总订单数</p>
                  <p class="amount-breakdown-value">{{ selectedUserDetail.orderCount ?? 0 }}</p>
                </div>
                <div class="amount-breakdown-item">
                  <p class="amount-breakdown-label">已完成订单</p>
                  <p class="amount-breakdown-value">{{ selectedUserDetail.completedOrderCount ?? 0 }}</p>
                </div>
                <div v-if="selectedUserDetail.attendantProfile" class="amount-breakdown-item">
                  <p class="amount-breakdown-label">资质状态</p>
                  <p class="amount-breakdown-value small-value">{{ getAttendantStatusLabel(selectedUserDetail.attendantProfile.status, selectedUserDetail.attendantProfile.statusLabel || '--') }}</p>
                </div>
              </div>
            </section>

            <section v-if="selectedUserDetail.attendantProfile" class="profile-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">陪诊师资料</h4>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <p class="kv-label">常驻医院</p>
                  <p class="kv-value">{{ selectedUserDetail.attendantProfile.hospitalName || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">擅长领域</p>
                  <p class="kv-value">{{ selectedUserDetail.attendantProfile.professionalField || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">从业年限</p>
                  <p class="kv-value">{{ selectedUserDetail.attendantProfile.experienceYears || '-' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">资质状态</p>
                  <p class="kv-value">{{ getAttendantStatusLabel(selectedUserDetail.attendantProfile.status, selectedUserDetail.attendantProfile.statusLabel || '--') }}</p>
                </div>
              </div>
            </section>

            <section v-if="selectedUserDetail.qualification" class="profile-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">资质资料</h4>
              </div>
              <div class="kv-grid">
                <div class="kv-item">
                  <p class="kv-label">身份证正面</p>
                  <p class="kv-value">{{ selectedUserDetail.qualification.idCardFrontFileUrl ? '已上传' : '未上传' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">身份证反面</p>
                  <p class="kv-value">{{ selectedUserDetail.qualification.idCardBackFileUrl ? '已上传' : '未上传' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">执业证书</p>
                  <p class="kv-value">{{ selectedUserDetail.qualification.practiceCertFileUrl ? '已上传' : '未上传' }}</p>
                </div>
                <div class="kv-item">
                  <p class="kv-label">健康证</p>
                  <p class="kv-value">{{ selectedUserDetail.qualification.healthCertFileUrl ? '已上传' : '未上传' }}</p>
                </div>
              </div>
            </section>

            <section class="profile-section">
              <div class="section-heading compact-heading">
                <h4 class="section-title">最近订单</h4>
              </div>
              <div v-if="selectedUserDetail.recentOrders?.length" class="table-wrap inner-table-wrap">
                <table class="table inner-table">
                  <thead>
                    <tr>
                      <th>订单号</th>
                      <th>医院</th>
                      <th>服务时间</th>
                      <th>金额</th>
                      <th>状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="order in selectedUserDetail.recentOrders" :key="order.orderId">
                      <td>{{ order.orderNo }}</td>
                      <td>{{ order.hospital || '-' }}</td>
                      <td>{{ order.serviceDate || '-' }} {{ order.serviceTimeSlot || '' }}</td>
                      <td>{{ formatMoney(order.orderAmount) }}</td>
                      <td><span class="badge" :class="getOrderStatusBadge(order.orderStatus)">{{ getOrderStatusLabel(order.orderStatus, order.orderStatusLabel || '--') }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div v-else class="empty-card inner-empty">暂无最近订单。</div>
            </section>
          </div>
        </template>
        <div v-else class="empty-card">当前页没有可展示的用户资料。</div>
      </BaseDrawer>
    </div>

    <BaseDialog
      v-model="confirmOpen"
      title="确认状态变更"
      :description="confirmUser ? `将用户 ${confirmUser.name || confirmUser.phone || confirmUser.id} ${confirmUser.status === 1 ? '禁用' : '恢复'}。` : ''"
      width="520px"
    >
      <p class="section-copy">确认后将更新用户状态。</p>
      <template #footer>
        <button class="button button-ghost" type="button" @click="confirmOpen = false">取消</button>
        <button class="button" :class="confirmUser?.status === 1 ? 'button-danger' : 'button-primary'" type="button" :disabled="actionLoading" @click="confirmStatusChange">
          {{ actionLoading ? '提交中...' : confirmUser?.status === 1 ? '确认禁用' : '确认恢复' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import BaseDrawer from '../components/BaseDrawer.vue'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useUiStore } from '../stores/ui'
import { fetchUserDetail, fetchUsers, updateUserStatus } from '../utils/admin-api'
import { getAttendantStatusLabel, getOrderStatusBadge, getOrderStatusLabel, getUserStatusBadge, getUserStatusLabel, getUserTypeLabel, toQueryValue, userStatusOptions, userTypeOptions } from '../utils/admin-view'
import { formatDateTime, formatMoney } from '../utils/format'

const uiStore = useUiStore()

const filters = reactive({
  keyword: '',
  userType: '',
  status: ''
})

const users = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)

const selectedUserId = ref(null)
const detailDrawerOpen = ref(false)
const detailLoading = ref(false)
const selectedUserDetail = ref(null)
const confirmOpen = ref(false)
const confirmUser = ref(null)
const actionLoading = ref(false)
const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%2284%22 height=%2284%22 viewBox=%220 0 84 84%22 fill=%22none%22%3E%3Crect width=%2284%22 height=%2284%22 rx=%2242%22 fill=%22%23E7EEF9%22/%3E%3Cpath d=%22M42 42C49.1797 42 55 36.1797 55 29C55 21.8203 49.1797 16 42 16C34.8203 16 29 21.8203 29 29C29 36.1797 34.8203 42 42 42ZM42 48.5C33.3242 48.5 26.25 55.5742 26.25 64.25V67H57.75V64.25C57.75 55.5742 50.6758 48.5 42 48.5Z%22 fill=%22%2392A3BF%22/%3E%3C/svg%3E'

const firstValidValue = (payload, keys) => {
  for (const key of keys) {
    const value = payload?.[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

const formatUserSex = (user) => {
  const value = String(firstValidValue(user, ['sex', 'gender', 'patientSex'])).trim()
  if (!value) return '未知'
  if (['1', 'male', '男', 'man'].includes(value.toLowerCase())) return '男'
  if (['2', 'female', '女', 'woman'].includes(value.toLowerCase())) return '女'
  return value
}

const formatUserAge = (user) => {
  const value = firstValidValue(user, ['age', 'userAge', 'patientAge'])
  return value === '' ? '-' : `${value}`
}

const getUserAvatar = (user) => firstValidValue(user, ['avatar', 'avatarUrl', 'headImg', 'headImgUrl', 'profilePhoto']) || defaultAvatar
const getUserRegisterTime = (user) => firstValidValue(user, ['registerTime', 'registerAt', 'createTime', 'createdAt'])
const getOrderCountValue = (user) => firstValidValue(user, ['orderCount', 'totalOrderCount', 'totalOrders', 'orderNum']) || 0
const getCompletedOrderCountValue = (user) => firstValidValue(user, ['completedOrderCount', 'finishOrderCount', 'finishedOrderCount', 'completedOrders']) || 0

const isAttendantUser = (user) => {
  if (user?.attendantProfile) return true
  const typeLabel = String(user?.userTypeLabel || '').toLowerCase()
  const typeValue = String(user?.userType || '').toLowerCase()
  return typeLabel.includes('陪诊') || typeValue.includes('attendant') || typeValue === '2' || Number(user?.userType) === 1
}

const getAttendantProfile = (user) => user?.attendantProfile || user || {}
const getAttendantAuditStatus = (user) => getAttendantStatusLabel(firstValidValue(getAttendantProfile(user), ['attendantAuditStatus', 'status', 'auditStatus']), firstValidValue(getAttendantProfile(user), ['attendantAuditStatusLabel', 'statusLabel', 'auditStatusLabel']) || '--')

const selectedUserSummary = computed(() => users.value.find((user) => user.id === selectedUserId.value) || null)

const resolveSelectedUserId = () => {
  if (!users.value.length) return null
  if (selectedUserId.value !== null && users.value.some((user) => user.id === selectedUserId.value)) return selectedUserId.value
  return users.value[0].id
}

const loadSelectedUserDetail = async (userId) => {
  if (!userId) {
    selectedUserDetail.value = null
    return
  }
  detailLoading.value = true
  try {
    selectedUserDetail.value = await fetchUserDetail(userId)
  } catch (error) {
    selectedUserDetail.value = null
    uiStore.toast(error.message || '用户详情加载失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const response = await fetchUsers({
      keyword: filters.keyword,
      userType: toQueryValue(filters.userType),
      status: toQueryValue(filters.status),
      page: page.value,
      pageSize: pageSize.value
    })
    users.value = response.content || []
    total.value = response.totalElements || 0
    totalPages.value = Math.max(response.totalPages || 1, 1)

    selectedUserId.value = resolveSelectedUserId()
    await loadSelectedUserDetail(selectedUserId.value)
  } catch (error) {
    users.value = []
    selectedUserId.value = null
    selectedUserDetail.value = null
    total.value = 0
    totalPages.value = 1
    uiStore.toast(error.message || '用户列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = () => {
  page.value = 0
  selectedUserId.value = null
  loadUsers()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.userType = ''
  filters.status = ''
  submitFilters()
}

const changePage = (nextPage) => {
  page.value = nextPage
  selectedUserId.value = null
  loadUsers()
}

const changePageSize = () => {
  page.value = 0
  selectedUserId.value = null
  loadUsers()
}

const openUserDrawer = async (userId) => {
  if (!userId) return
  detailDrawerOpen.value = true
  if (selectedUserId.value === userId && selectedUserDetail.value) return
  selectedUserId.value = userId
  await loadSelectedUserDetail(userId)
}

const promptStatusChange = (user) => {
  confirmUser.value = user
  confirmOpen.value = true
}

const confirmStatusChange = async () => {
  if (!confirmUser.value) return
  actionLoading.value = true
  try {
    await updateUserStatus(confirmUser.value.id, confirmUser.value.status === 1 ? 0 : 1)
    uiStore.toast('用户状态更新成功', 'success')
    confirmOpen.value = false
    await loadUsers()
  } catch (error) {
    uiStore.toast(error.message || '用户状态更新失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.filter-fields-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) repeat(2, minmax(170px, 1fr));
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
.record-meta-grid,
.record-stat,
.user-info-copy {
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

.record-chip-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
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

.record-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
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
  min-width: 128px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex: 0 0 36px;
  object-fit: cover;
  border: 1px solid rgba(173, 199, 232, 0.75);
  background: #eef4fb;
}

.profile-section {
  display: grid;
  gap: 12px;
}

.compact-heading {
  margin-bottom: 0;
}

.inner-table-wrap {
  border-radius: var(--radius-md);
}

.inner-table {
  min-width: 720px;
}

.inner-empty {
  padding: 18px;
}

.small-value {
  font-size: 16px;
}

@media (max-width: 1080px) {
  .record-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .record-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    min-width: 0;
  }

  .filter-fields-row {
    grid-template-columns: repeat(2, minmax(170px, 1fr));
  }
}

@media (max-width: 640px) {
  .record-summary {
    flex-direction: column;
    align-items: stretch;
  }

  .record-chip-row {
    justify-content: flex-start;
  }

  .record-meta-grid,
  .filter-fields-row {
    grid-template-columns: 1fr;
  }

  .record-side {
    grid-template-columns: 1fr;
  }
}
</style>
