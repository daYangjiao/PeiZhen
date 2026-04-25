const ACTIONABLE_TAB_VALUES = new Set([0, 4, 9])

const toNumber = (value) => {
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? null : numberValue
}

export const getPatientOrderActionTabValue = (order) => {
  if (!order) return null

  const paymentStatus = toNumber(order.paymentStatus)
  const orderStatus = toNumber(order.orderStatus)

  if (paymentStatus === 0 && orderStatus !== 7) return 0
  if (orderStatus === 4) return 4
  if (orderStatus === 9) return 9

  return null
}

export const hasPatientActionableOrder = (orders = []) => {
  return Array.isArray(orders) && orders.some((order) => getPatientOrderActionTabValue(order) !== null)
}

export const shouldShowOrderTabBadge = (tabValue, orders = []) => {
  if (tabValue == null) return hasPatientActionableOrder(orders)

  const normalizedTabValue = toNumber(tabValue)
  if (!ACTIONABLE_TAB_VALUES.has(normalizedTabValue)) return false

  return Array.isArray(orders) && orders.some((order) => getPatientOrderActionTabValue(order) === normalizedTabValue)
}
