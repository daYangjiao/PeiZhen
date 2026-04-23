import { getMappedStatusText, getStatusMapEntries } from '../../../shared/admin-status-mapping'

const toOptions = (allLabel, group) => [
  { label: allLabel, value: '' },
  ...Object.entries(getStatusMapEntries(group)).map(([value, label]) => ({
    label,
    value: Number(value)
  }))
]

export const getUserTypeLabel = (value, fallback = '-') => getMappedStatusText('userType', value, fallback)
export const getUserStatusLabel = (value, fallback = '-') => getMappedStatusText('userStatus', value, fallback)
export const getAttendantStatusLabel = (value, fallback = '-') => getMappedStatusText('attendantAuditStatus', value, fallback)
export const getOrderStatusLabel = (value, fallback = '-') => getMappedStatusText('orderStatus', value, fallback)
export const getPaymentStatusLabel = (value, fallback = '-') => getMappedStatusText('paymentStatus', value, fallback)

export const userTypeOptions = toOptions('全部角色', 'userType')
export const userStatusOptions = toOptions('全部状态', 'userStatus')
export const attendantStatusOptions = toOptions('全部审核状态', 'attendantAuditStatus')
export const orderStatusOptions = toOptions('全部订单状态', 'orderStatus')
export const paymentStatusOptions = toOptions('全部支付状态', 'paymentStatus')

export const getUserStatusBadge = (status) => (status === 1 ? 'badge-green' : 'badge-red')

export const getAttendantStatusBadge = (status) => {
  if (status === 1) return 'badge-green'
  if (status === 2 || status === 3) return 'badge-red'
  return 'badge-orange'
}

export const getOrderStatusBadge = (status) => {
  if (status === 6) return 'badge-green'
  if (status === 7) return 'badge-red'
  if (status === 5) return 'badge-orange'
  if (status === 0) return 'badge-gray'
  return 'badge-blue'
}

export const getPaymentStatusBadge = (status) => (status === 1 ? 'badge-green' : 'badge-gray')

export const toQueryValue = (value) => (value === '' ? '' : Number(value))
