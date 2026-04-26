export const PLATFORM_COMMISSION_RATE = 0.10
export const ATTENDANT_INCOME_RATE = 1 - PLATFORM_COMMISSION_RATE

const toMoneyNumber = (value) => {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount : 0
}

const roundMoney = (value) => Math.round(toMoneyNumber(value) * 100) / 100

const hasBackendAmount = (value) => value !== undefined && value !== null && value !== ''

export const normalizeFinalOrderAmount = (order = {}) => {
  if (hasBackendAmount(order.settlementAmount)) {
    return roundMoney(order.settlementAmount)
  }
  if (Number(order.orderStatus) !== 6) {
    return 0
  }
  let amount = toMoneyNumber(order.orderAmount)
  const refund = toMoneyNumber(order.refundAmount)
  const balance = hasBackendAmount(order.balanceAmount) ? toMoneyNumber(order.balanceAmount) : null
  if (refund > 0 && (balance === null || balance >= 0)) {
    amount -= refund
  }
  return roundMoney(Math.max(amount, 0))
}

export const calculatePlatformFee = (order = {}) => {
  if (hasBackendAmount(order.platformFeeAmount)) {
    return roundMoney(order.platformFeeAmount)
  }
  return roundMoney(normalizeFinalOrderAmount(order) * PLATFORM_COMMISSION_RATE)
}

export const calculateAttendantIncome = (order = {}) => {
  if (hasBackendAmount(order.attendantIncomeAmount)) {
    return roundMoney(order.attendantIncomeAmount)
  }
  return roundMoney(normalizeFinalOrderAmount(order) * ATTENDANT_INCOME_RATE)
}

export const shouldDisplayIncomeRecord = (order = {}) => {
  return calculateAttendantIncome(order) > 0
}

export const calculateEstimatedAttendantIncome = (amountOrOrder = 0) => {
  const amount = typeof amountOrOrder === 'object'
    ? toMoneyNumber(amountOrOrder.orderAmount)
    : toMoneyNumber(amountOrOrder)
  return roundMoney(Math.max(amount, 0) * ATTENDANT_INCOME_RATE)
}

export const calculateDisplayAttendantIncome = (order = {}) => {
  const status = Number(order.orderStatus)
  if (status === 6) {
    return calculateAttendantIncome(order)
  }
  if (status === 7 || status === 10) {
    return 0
  }
  return calculateEstimatedAttendantIncome(order)
}
