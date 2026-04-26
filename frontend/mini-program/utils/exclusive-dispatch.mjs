const EXCLUSIVE_DISPATCH_STATUS = 8
const EXCLUSIVE_DISPATCH_TIMEOUT_MS = 15 * 60 * 1000

const toNumberId = (value) => {
  const next = Number(value || 0)
  return Number.isFinite(next) && next > 0 ? next : 0
}

const parseTimeMs = (value) => {
  if (!value) return 0
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (value instanceof Date) return Number.isFinite(value.getTime()) ? value.getTime() : 0
  if (typeof value !== 'string') return 0
  const normalized = value.includes('T') ? value : value.replace(/-/g, '/')
  const parsed = Date.parse(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

export const buildExclusiveDispatchKey = (userId, orderId) => {
  const nextUserId = toNumberId(userId)
  const nextOrderId = toNumberId(orderId)
  if (!nextUserId || !nextOrderId) return ''
  return `${nextUserId}:${nextOrderId}`
}

export const getExclusiveDispatchDeadline = (order = {}) => {
  const baseTime = parseTimeMs(order.paymentTime || order.createTime || order.updateTime)
  if (!baseTime) return 0
  return baseTime + EXCLUSIVE_DISPATCH_TIMEOUT_MS
}

export const getExclusiveDispatchRemainingMs = (order = {}, now = Date.now()) => {
  const deadline = getExclusiveDispatchDeadline(order)
  if (!deadline) return 0
  return Math.max(0, deadline - now)
}

export const isExclusiveDispatchExpired = (order = {}, now = Date.now()) =>
  getExclusiveDispatchRemainingMs(order, now) <= 0

export const getExclusiveDispatchDisplayState = (order = {}, now = Date.now()) => {
  const ended = Number(order.orderStatus) === EXCLUSIVE_DISPATCH_STATUS
    && isExclusiveDispatchExpired(order, now)
  return ended
    ? {
        active: false,
        ended: true,
        statusText: '专属派单已流转',
        detailTitle: '专属接单时段已结束',
        detailHint: '你曾收到过这笔专属派单，目前已不再单独保留，系统会自动转入公共接单大厅。',
      }
    : {
        active: Number(order.orderStatus) === EXCLUSIVE_DISPATCH_STATUS,
        ended: false,
        statusText: '专属派单待确认',
        detailTitle: '专属派单待确认',
        detailHint: '请尽快确认接单，超时后订单将自动释放到公共接单大厅。',
      }
}

export const formatExclusiveDispatchCountdown = (remainingMs = 0) => {
  const safeMs = Math.max(0, Number(remainingMs) || 0)
  const totalSeconds = Math.floor(safeMs / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export const shouldOpenExclusiveDispatchPopup = ({
  userId,
  orderId,
  orderStatus,
  remainingMs,
  ignoredKeys,
  currentRoute,
  currentOrderId,
} = {}) => {
  const key = buildExclusiveDispatchKey(userId, orderId)
  if (!key) return false
  if (Number(orderStatus) !== EXCLUSIVE_DISPATCH_STATUS) return false
  if ((Number(remainingMs) || 0) <= 0) return false
  if (ignoredKeys instanceof Set && ignoredKeys.has(key)) return false

  const normalizedRoute = String(currentRoute || '')
  const activeOrderId = toNumberId(currentOrderId)
  if (normalizedRoute.includes('subpkg/order/escort-detail') && activeOrderId === toNumberId(orderId)) {
    return false
  }

  return true
}

export const EXCLUSIVE_DISPATCH_TIMEOUT = EXCLUSIVE_DISPATCH_TIMEOUT_MS
