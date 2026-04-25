import { del, get, patch, post } from './http'

export const loginAdmin = (payload) => post('/api/admin/auth/login', payload)
export const fetchDashboard = () => get('/api/admin/dashboard/overview')
export const fetchUsers = (params) => get('/api/admin/users', params)
export const fetchUserDetail = (id) => get(`/api/admin/users/${id}`)
export const updateUserStatus = (id, status) => patch(`/api/admin/users/${id}/status`, { status })

export const fetchAttendants = (params) => get('/api/admin/attendants', params)
export const fetchAttendantDetail = (id) => get(`/api/admin/attendants/${id}`)
export const fetchAttendantQualificationLogs = (id, params) => get(`/api/admin/attendants/${id}/qualification-logs`, params)
export const fetchNextPendingAttendant = (params) => get('/api/admin/attendants/next-pending', params)
export const reviewAttendant = (id, payload) => patch(`/api/admin/attendants/${id}/qualification-review`, payload)
export const updateAttendantStatus = (id, payload) => patch(`/api/admin/attendants/${id}/status`, payload)

export const fetchOrders = (params) => get('/api/admin/orders', params)
export const fetchOrderDetail = (id) => get(`/api/admin/orders/${id}`)
export const cancelOrder = (id, payload) => patch(`/api/admin/orders/${id}/cancel`, payload)
export const resolveDispute = (id, payload) => patch(`/api/admin/orders/${id}/dispute-resolution`, payload)
export const fetchOperationLogs = (params) => get('/api/admin/operation-logs', params)

export const fetchWorkbenchSummary = () => get('/api/admin/workbench/summary')
export const fetchWorkbenchTasks = (params) => get('/api/admin/workbench/tasks', params)
export const claimWorkbenchTask = (type, targetId) => post(`/api/admin/workbench/tasks/${type}/${targetId}/claim`)
export const completeWorkbenchTask = (type, targetId, payload) => post(`/api/admin/workbench/tasks/${type}/${targetId}/complete`, payload)
export const releaseWorkbenchTask = (type, targetId) => del(`/api/admin/workbench/tasks/${type}/${targetId}/claim`)

export const fetchAdminUsers = (params) => get('/api/admin/admin-users', params)
export const createAdminUser = (payload) => post('/api/admin/admin-users', payload)
export const updateAdminUserStatus = (id, status) => patch(`/api/admin/admin-users/${id}/status`, { status })
export const deleteAdminUser = (id) => del(`/api/admin/admin-users/${id}`)
