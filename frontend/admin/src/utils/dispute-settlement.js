const toNumber = (value) => {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
}

const normalizeServiceType = (order) => {
  const serviceType = toNumber(order?.clinicType ?? order?.serviceTypeNumber ?? order?.serviceType)
  return [1, 2, 3, 4].includes(serviceType) ? serviceType : 1
}

export const calculateServiceAmountByDuration = (duration, serviceType = 1) => {
  const hours = toNumber(duration)
  if (!hours || hours <= 0) return null

  switch (serviceType) {
    case 2:
      return Math.ceil(hours) * 45
    case 3:
      return calculateServiceAmountByDuration(hours, 1) + 100
    case 4:
      return calculateServiceAmountByDuration(hours, 1) + 30
    case 1:
    default: {
      const extraHours = Math.max(0, Math.ceil(hours - 2))
      return 50 + extraHours * 30
    }
  }
}

export const formatDisputeAmount = (amount) => {
  const numberValue = toNumber(amount)
  return numberValue === null ? '' : numberValue.toFixed(2)
}

export const suggestDisputeFinalAmount = (order, duration) => {
  const calculated = calculateServiceAmountByDuration(duration, normalizeServiceType(order))
  if (calculated !== null) return formatDisputeAmount(calculated)

  const paidAmount = toNumber(order?.orderAmount) || 0
  const balanceAmount = toNumber(order?.balanceAmount) || 0
  return formatDisputeAmount(paidAmount + balanceAmount)
}
