export const TIMEOUT_CLOSE_STATUS_TEXT = '超时关闭'
const TIMEOUT_REASON_KEYWORD = '超时未匹配到陪诊师'
const DEFAULT_TIMEOUT_CLOSE_MESSAGE = '订单已超时未匹配到陪诊师，系统已自动关闭并发起退款。'

export const isTimeoutClosedOrder = (order) => {
  if (!order || Number(order.orderStatus) !== 7) {
    return false
  }
  const statusText = String(order.orderStatusDesc || '').trim()
  const cancelReason = String(order.cancelReason || '').trim()
  return statusText === TIMEOUT_CLOSE_STATUS_TEXT || cancelReason.includes(TIMEOUT_REASON_KEYWORD)
}

export const getTimeoutClosedMessage = (order) => {
  const cancelReason = String(order?.cancelReason || '').trim()
  if (cancelReason) {
    return cancelReason
  }
  return DEFAULT_TIMEOUT_CLOSE_MESSAGE
}
