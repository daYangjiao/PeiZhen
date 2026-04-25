<template>
  <AppShell title="管理员账号" subtitle="管理员账号管理">
    <div class="page-stack">
      <section class="panel-card filter-card">
        <div class="section-heading filter-heading">
          <div>
            <h3 class="section-title">管理员筛选</h3>
          </div>
        </div>

        <div class="filter-board">
          <div class="filter-fields-row">
            <label class="filter-field">
              <span>账号状态</span>
              <BaseSelect v-model="filters.status" :options="userStatusOptions" />
            </label>
          </div>
          <div class="filter-actions-row">
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
            <p class="section-copy">支持启用、禁用管理员账号。</p>
          </div>
        </div>

        <div v-if="loading" class="skeleton"></div>
        <template v-else>
          <div v-if="admins.length" class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th class="id-cell">ID</th>
                  <th>管理员信息</th>
                  <th class="status-cell">状态</th>
                  <th class="status-cell">账号类型</th>
                  <th class="time-cell">创建时间</th>
                  <th class="time-cell">最近登录</th>
                  <th class="actions-cell">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="admin in admins" :key="admin.id || admin.phone || admin.account">
                  <td class="id-cell">{{ admin.id ?? '-' }}</td>
                  <td>
                    <p class="table-cell-title">{{ admin.name || '未命名管理员' }}</p>
                    <p class="table-cell-copy">
                      {{ admin.phone || '-' }}
                      <span v-if="admin.account"> / {{ admin.account }}</span>
                    </p>
                  </td>
                  <td class="status-cell"><span class="badge" :class="getUserStatusBadge(admin.status)">{{ getStatusLabel(admin.status) }}</span></td>
                  <td class="status-cell"><span class="badge badge-blue">{{ getRoleLabel(admin.role) }}</span></td>
                  <td class="time-cell">{{ formatDateTime(admin.createTime) }}</td>
                  <td class="time-cell">{{ formatDateTime(admin.lastLoginTime) }}</td>
                  <td class="actions-cell">
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
                      <button
                        class="button button-danger"
                        type="button"
                        :disabled="actionLoading || isCurrentAdmin(admin)"
                        @click="promptDeleteAdmin(admin)"
                      >
                        删除
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
            <BaseSelect v-model="pageSize" :options="pageSizeOptions" @change="changePageSize" />
            <button class="button button-ghost" type="button" :disabled="page === 0 || loading" @click="changePage(page - 1)">上一页</button>
            <button class="button button-ghost" type="button" :disabled="page + 1 >= totalPages || loading" @click="changePage(page + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <BaseDialog
      v-model="createDialogOpen"
      title="新建管理员"
      description="填写管理员信息。"
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
        <label class="login-field">
          <span>账号类型</span>
          <BaseSelect v-model="createForm.role" :options="adminRoleOptions" />
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
      <p class="section-copy">确认后将更新该管理员状态。</p>
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

    <BaseDialog
      v-model="deleteDialogOpen"
      title="确认删除管理员"
      :description="deleteTarget ? `将永久删除管理员 ${deleteTarget.name || deleteTarget.phone || deleteTarget.id}。` : ''"
      width="520px"
    >
      <p class="section-copy">该操作不可撤销，请谨慎确认。</p>
      <template #footer>
        <button class="button button-ghost" type="button" @click="deleteDialogOpen = false">取消</button>
        <button
          class="button button-danger"
          type="button"
          :disabled="actionLoading"
          @click="submitDeleteAdmin"
        >
          {{ actionLoading ? '删除中...' : '确认删除' }}
        </button>
      </template>
    </BaseDialog>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppShell from '../components/AppShell.vue'
import BaseDialog from '../components/BaseDialog.vue'
import BaseSelect from '../components/BaseSelect.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { createAdminUser, deleteAdminUser, fetchAdminUsers, updateAdminUserStatus } from '../utils/admin-api'
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
const pageSizeOptions = [
  { label: '10 条 / 页', value: 10 },
  { label: '20 条 / 页', value: 20 },
  { label: '50 条 / 页', value: 50 }
]
const adminRoleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '超级管理员', value: 'SUPER_ADMIN' }
]

const createDialogOpen = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  name: '',
  phone: '',
  password: '',
  role: 'ADMIN'
})

const statusDialogOpen = ref(false)
const actionLoading = ref(false)
const statusTarget = ref(null)
const deleteDialogOpen = ref(false)
const deleteTarget = ref(null)

const currentAdminIdentity = computed(() => ({
  id: authStore.user?.id ?? authStore.user?.adminId ?? authStore.user?.userId ?? null,
  phone: authStore.user?.phone || '',
  account: authStore.user?.account || authStore.user?.username || ''
}))

const getStatusLabel = (status) => (Number(status) === 1 ? '正常' : '禁用')
const getRoleLabel = (role) => (role === 'SUPER_ADMIN' ? '超级管理员' : '管理员')

const normalizeAdmin = (item) => ({
  ...item,
  id: item.id ?? item.adminId ?? item.userId ?? null,
  name: item.name || item.nickname || item.realName || item.account || '',
  phone: item.phone || item.mobile || '',
  account: item.account || item.username || '',
  status: Number(item.status ?? 1),
  role: item.role === 'SUPER_ADMIN' ? 'SUPER_ADMIN' : 'ADMIN',
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
  createForm.role = 'ADMIN'
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
      password: createForm.password,
      role: createForm.role
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

const promptDeleteAdmin = (admin) => {
  if (isCurrentAdmin(admin)) {
    uiStore.toast('当前登录账号不可删除', 'error')
    return
  }
  deleteTarget.value = admin
  deleteDialogOpen.value = true
}

const submitDeleteAdmin = async () => {
  if (!deleteTarget.value) return
  if (deleteTarget.value.id === null || deleteTarget.value.id === undefined) {
    uiStore.toast('无法识别管理员 ID，删除失败', 'error')
    deleteDialogOpen.value = false
    return
  }
  if (isCurrentAdmin(deleteTarget.value)) {
    uiStore.toast('当前登录账号不可删除', 'error')
    deleteDialogOpen.value = false
    return
  }

  actionLoading.value = true
  try {
    await deleteAdminUser(deleteTarget.value.id)
    uiStore.toast('管理员删除成功', 'success')
    deleteDialogOpen.value = false
    await loadAdminUsers()
  } catch (error) {
    uiStore.toast(error.message || '管理员删除失败', 'error')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadAdminUsers)
</script>
