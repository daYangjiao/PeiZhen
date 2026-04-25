export const PLATFORM_COMMISSION_RATE = 0.10
export const ATTENDANT_INCOME_RATE = 1 - PLATFORM_COMMISSION_RATE

const toMoneyNumber = (value) => {
  const amount = Number(value || 0)
  return Number.isFinite(amount) ? amount : 0
}

const roundMoney = (value) => Math.round(toMoneyNumber(value) * 100) / 100

export const normalizeFinalOrderAmount = (order = {}) => {
  return roundMoney(order.orderAmount)
}

export const calculatePlatformFee = (order = {}) => {
  return roundMoney(normalizeFinalOrderAmount(order) * PLATFORM_COMMISSION_RATE)
}

export const calculateAttendantIncome = (order = {}) => {
  return roundMoney(normalizeFinalOrderAmount(order) * ATTENDANT_INCOME_RATE)
}
