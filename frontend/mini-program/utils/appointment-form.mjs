export const normalizeContactName = (value = '') => String(value ?? '').trimStart()

export const normalizeContactPhone = (value = '') => String(value ?? '')
  .replace(/\D/g, '')
  .slice(0, 11)

export const parseTimeToMinutes = (timeValue) => {
  const normalizedTime = String(timeValue || '').trim()
  if (!normalizedTime) return NaN
  const startSegment = normalizedTime.includes('-') ? normalizedTime.split('-')[0].trim() : normalizedTime
  const match = startSegment.match(/^(\d{1,2}):(\d{2})$/)
  if (!match) return NaN
  const hour = Number(match[1])
  const minute = Number(match[2])
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return NaN
  return hour * 60 + minute
}

export const calculateSlotDurationMinutes = (startValue, endValue) => {
  const startMinutes = parseTimeToMinutes(startValue)
  const endMinutes = parseTimeToMinutes(endValue)
  if (Number.isNaN(startMinutes) || Number.isNaN(endMinutes)) {
    return NaN
  }
  if (endMinutes === startMinutes) {
    return 0
  }
  return endMinutes > startMinutes
    ? endMinutes - startMinutes
    : 24 * 60 - startMinutes + endMinutes
}

export const isCrossDayEndTime = (startValue, endValue) => {
  const durationMinutes = calculateSlotDurationMinutes(startValue, endValue)
  if (Number.isNaN(durationMinutes) || durationMinutes <= 0) return false
  return parseTimeToMinutes(endValue) < parseTimeToMinutes(startValue)
}

export const formatSelectedEndTimeDisplay = (startValue, endValue) => {
  if (!endValue) return ''
  if (!startValue) return endValue
  const durationMinutes = calculateSlotDurationMinutes(startValue, endValue)
  if (Number.isNaN(durationMinutes) || durationMinutes <= 0) return endValue
  return isCrossDayEndTime(startValue, endValue) ? `次日${endValue}` : endValue
}
