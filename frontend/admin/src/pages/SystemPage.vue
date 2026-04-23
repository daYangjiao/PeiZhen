<template>
  <AppShell title="系统设置" subtitle="管理员账号管理与系统说明">
    <div class="page-stack">
      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">管理员筛选</h3>
            <p class="section-copy">支持按状态筛选，并可创建新管理员账号。</p>
          </div>
        </div>

        <div class="toolbar">
          <div class="toolbar-group">
            <select v-model="filters.status" class="filter-select">
              <option v-for="option in userStatusOptions" :key="option.label" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
          <div class="toolbar-group">
            <button class="button button-primary" type="button" :disabled="loading" @click="submitFilters">查询</button>
            <button class="button button-ghost" type="button" :disabled="loading" @click="resetFilters">重置</button>
            <button class="button button-secondary" type="button" @click="openCreateDialog">新建管理员</button>
          </div>
        </div>
      </section>

      <section class="panel-card">
        <div class="section-heading">
          <div>
            <h3 class="section-title">管理员列表</h3>
            <p class="section-copy">数据来自 `/api/admin/admin-users`，可直接启用/禁用管理员账号。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="admins.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>管理员信息</th>
                  <th>状态</th>
                  <th>创建时间</th>
                  <th>最近登录</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="admin in admins" :key="admin.id || admin.phone || admin.account">
                  <td>{{ admin.id ?? '-' }}</td>
                  <td>
                    <p class="table-cell-title">{{ admin.name || '未命名管理员' }}</p>
                    <p class="table-cell-copy">
                      {{ admin.phone || '-' }}
                      <span v-if="admin.account"> / {{ admin.account }}</span>
                    </p>
                  </td>
                  <td><span class="badge" :class="getUserStatusBadge(admin.status)">{{ getStatusLabel(admin.status) }}</span></td>
                  <td>{{ formatDateTime(admin.createTime) }}</td>
                  <td>{{ formatDateTime(admin.lastLoginTime) }}</td>
                  <td>
                    <div class="table-actions">
                      <button
                        class="button"
                        :class="admin.status === 1 ? 'button-danger' : 'button-primary'"
                        type="button"
                        :disabled="actionLoading || (admin.status === 1 && isCurrentAdmin(admin))"
                        @click="promptStatusChange(admin)"
                      >
                        {{ admin.status === 1 ? '禁用' : '恢复' }}
                      </button>
                    </div>
                    <p v-if="admin.status === 1 && isCurrentAdmin(admin)" class="helper-text">当前登录账号不可禁用</p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-card">当前没有符合条件的管理员。</div>
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

      <section class="readonly-card">
        <div class="section-heading">
          <div>
            <span class="inline-tag">只读说明</span>
            <h3 class="section-title">系统与联调信息</h3>
            <p class="section-copy">以下区域仍保持只读，用于汇总后台形态和运维说明。</p>
          </div>
        </div>

        <div class="kv-grid">
          <div class="kv-item">
            <p class="kv-label">当前后台形态</p>
            <p class="kv-value">桌面端 Vue 3 + Vite 管理台，入口路径为 `/admin/`。</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">本地联调目标</p>
            <p class="kv-value">默认代理到 `http://127.0.0.1:8081`，统一走 `/api`、`/uploads`、`/ws`。</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">登录态说明</p>
            <p class="kv-value">通过 `/api/admin/auth/login` 获取 token，并在 401 时自动清理本地会话。</p>
          </div>
          <div class="kv-item">
            <p class="kv-label">当前管理员</p>
            <p class="kv-value">{{ adminName }}</p>
          </div>
        </div>

        <div class="readonly-list">
          <article class="readonly-item">
            <h4 class="readonly-title">管理策略提醒</h4>
            <p class="readonly-copy">管理员禁用会立即生效，建议至少保留一个可登录账号，避免后台无人可维护。</p>
          </article>
          <article class="readonly-item">
            <h4 class="readonly-title">后续扩展建议</h4>
            <p class="readonly-copy">系统公告、通知偏好、审计日志等能力可继续放在本页，待对应后端接口稳定后再逐步开放编辑。</p>
          </article>
        </div>
      </section>
    </div>

    <BaseDialog
      v-model="createDialogOpen"
      title="新建管理员"
      description="创建后可立即使用账号密码登录后台。"
      width="560px"
    >
      <div class="page-stack">
        <label class="login-field">
          <span>姓名</span>
          <input v-model.trim="createForm.name" class="field" type="text" placeholder="请输入管理员姓名" />
        </label>
        <label class="login-field">
          <span>手机号</span>
          <input v-model.trim="createForm.phone" class="field" type="text" placeholder="请输入手机号" />
        </label>
        <label class="login-field">
          <span>初始密码</span>
          <input v-model.trim="createForm.password" class="field" type="password" placeholder="请输入初始密码（至少 6 位）" />
        </label>
      </div>

      <template #footer>
        <button class="button button-ghost" type="button" @click="createDialogOpen = false">取消</button>
        <button class="button button-primary" type="button" :disabled="createLoading" @click="submitCreateAdmin">
          {{ createLoading ? '创建中...' : '确认创建' }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog
      v-model="statusDialogOpen"
      title="确认状态变更"
      :description="statusTarget ? `将管理员 ${statusTarget.name || statusTarget.phone || statusTarget.id} ${statusTarget.status === 1 ? '禁用' : '恢复'}。` : ''"
      width="520px"
    >
      <p class="section-copy">状态变更会直接调用后台接口，并刷新当前列表。</p>
      <template #footer>
        <button class="button button-ghost" type="button" @click="statusDialogOpen = false">取消</button>
        <button
          class="button"
          :class="statusTarget?.status === 1 ? 'button-danger' : 'button-primary'"
          type="button"
          :disabled="actionLoading"
          @click="submitStatusChange"
        >
          {{ actionLoading ? '提交中...' : statusTarget?.status === 1 ? '确认禁用' : '确认恢复' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { createAdminUser, fetchAdminUsers, updateAdminUserStatus } from '../utils/admin-api'
import { getUserStatusBadge, toQueryValue, userStatusOptions } from '../utils/admin-view'
import { formatDateTime } from '../utils/format'

const authStore = useAuthStore()
const uiStore = useUiStore()
authStore.restore()

const filters = reactive({
  status: ''
})

const admins = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(1)
const page = ref(0)
const pageSize = ref(10)

const createDialogOpen = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  name: '',
  phone: '',
  password: ''
})

const statusDialogOpen = ref(false)
const actionLoading = ref(false)
const statusTarget = ref(null)

const adminName = computed(() => authStore.user?.name || authStore.user?.account || '管理员')
const currentAdminIdentity = computed(() => ({
  id: authStore.user?.id ?? authStore.user?.adminId ?? authStore.user?.userId ?? null,
  phone: authStore.user?.phone || '',
  account: authStore.user?.account || authStore.user?.username || ''
}))

const getStatusLabel = (status) => (Number(status) === 1 ? '正常' : '禁用')

const normalizeAdmin = (item) => ({
  ...item,
  id: item.id ?? item.adminId ?? item.userId ?? null,
  name: item.name || item.nickname || item.realName || item.account || '',
  phone: item.phone || item.mobile || '',
  account: item.account || item.username || '',
  status: Number(item.status ?? 1),
  createTime: item.createTime || item.createdAt || item.gmtCreate || '',
  lastLoginTime: item.lastLoginTime || item.lastLoginAt || item.loginTime || ''
})

const isCurrentAdmin = (admin) => {
  const currentId = currentAdminIdentity.value.id
  const currentPhone = currentAdminIdentity.value.phone
  const currentAccount = currentAdminIdentity.value.account

  if (currentId !== null && currentId !== undefined && admin.id !== null && admin.id !== undefined) {
    return Number(currentId) === Number(admin.id)
  }
  if (currentPhone && admin.phone) {
    return currentPhone === admin.phone
  }
  if (currentAccount && admin.account) {
    return currentAccount === admin.account
  }
  return false
}

const loadAdminUsers = async () => {
  loading.value = true
  try {
    const response = await fetchAdminUsers({
      status: toQueryValue(filters.status),
      page: page.value,
      pageSize: pageSize.value
    })
    const content = Array.isArray(response)
      ? response
      : (Array.isArray(response.content) ? response.content : (Array.isArray(response.records) ? response.records : []))
    admins.value = content.map(normalizeAdmin)
    total.value = response.totalElements ?? response.total ?? admins.value.length
    totalPages.value = Math.max(response.totalPages ?? response.pages ?? 1, 1)
  } catch (error) {
    uiStore.toast(error.message || '管理员列表加载失败', 'error')
  } finally {
    loading.value = false
  }
}

const submitFilters = () => {
  page.value = 0
  loadAdminUsers()
}

const resetFilters = () => {
  filters.status = ''
  submitFilters()
}

const changePage = (nextPage) => {
  page.value = nextPage
  loadAdminUsers()
}

const changePageSize = () => {
  page.value = 0
  loadAdminUsers()
}

const openCreateDialog = () => {
  createForm.name = ''
  createForm.phone = ''
  createForm.password = ''
  createDialogOpen.value = true
}

const submitCreateAdmin = async () => {
  if (!createForm.name || !createForm.phone || !createForm.password) {
    uiStore.toast('请完整填写姓名、手机号和密码', 'error')
    return
  }
  if (createForm.password.length < 6) {
    uiStore.toast('密码至少需要 6 位', 'error')
    return
  }

  createLoading.value = true
  try {
    await createAdminUser({
      name: createForm.name,
      phone: createForm.phone,
      password: createForm.password
    })
    uiStore.toast('管理员创建成功', 'success')
    createDialogOpen.value = false
    page.value = 0
    await loadAdminUsers()
  } catch (error) {
    uiStore.toast(error.message || '管理员创建失败', 'error')
  } finally {
    createLoading.value = false
  }
}

const promptStatusChange = (admin) => {
  if (admin.status === 1 && isCurrentAdmin(admin)) {
    uiStore.toast('当前登录账号不可禁用', 'error')
    return
  }
  statusTarget.value = admin
  statusDialogOpen.value = true
}

const submitStatusChange = async () => {
  if (!statusTarget.value) return
  if (statusTarget.value.id === null || statusTarget.value.id === undefined) {
    uiStore.toast('无法识别管理员 ID，状态更新失败', 'error')
    statusDialogOpen.value = false
    return
  }
  if (statusTarget.value.status === 1 && isCurrentAdmin(statusTarget.value)) {
    uiStore.toast('当前登录账号不可禁用', 'error')
    statusDialogOpen.value = false
    return
  }

  actionLoading.value = true
  try {
    const nextStatus = statusTarget.value.status === 1 ? 0 : 1
    await updateAdminUserStatus(statusTarget.value.id, nextStatus)
    uiStore.toast('管理员状态更新成功', 'success')
    statusDialogOpen.value = false
    await loadAdminUsers()
  } catch (error) {
    uiStore.toast(error.message || '管理员状态更新失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadAdminUsers)
</script>
