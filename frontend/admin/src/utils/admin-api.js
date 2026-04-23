import { del, get, patch, post } from './http'

export const loginAdmin = (payload) => post('/api/admin/auth/login', payload)
export const fetchDashboard = () => get('/api/admin/dashboard/overview')
export const fetchUsers = (params) => get('/api/admin/users', params)
export const fetchUserDetail = (id) => get(`/api/admin/users/${id}`)
export const updateUserStatus = (id, status) => patch(`/api/admin/users/${id}/status`, { status })

export const fetchAttendants = (params) => get('/api/admin/attendants', params)
export const fetchAttendantDetail = (id) => get(`/api/admin/attendants/${id}`)
export const fetchNextPendingAttendant = (params) => get('/api/admin/attendants/next-pending', params)
export const reviewAttendant = (id, payload) => patch(`/api/admin/attendants/${id}/qualification-review`, payload)
export const updateAttendantStatus = (id, payload) => patch(`/api/admin/attendants/${id}/status`, payload)

export const fetchOrders = (params) => get('/api/admin/orders', params)
export const fetchOrderDetail = (id) => get(`/api/admin/orders/${id}`)
export const cancelOrder = (id, payload) => patch(`/api/admin/orders/${id}/cancel`, payload)
export const resolveDispute = (id, payload) => patch(`/api/admin/orders/${id}/dispute-resolution`, payload)

export const fetchAdminUsers = (params) => get('/api/admin/admin-users', params)
export const createAdminUser = (payload) => post('/api/admin/admin-users', payload)
export const updateAdminUserStatus = (id, status) => patch(`/api/admin/admin-users/${id}/status`, { status })
export const deleteAdminUser = (id) => del(`/api/admin/admin-users/${id}`)
