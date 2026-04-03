export const formatOrderDateTime = (value) => {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    if (typeof value === 'string') {
      return value
        .replace('T', ' ')
        .replace(/\s*GMT[^\s)]+(\s*\([^)]+\))?/i, '')
        .trim()
    }
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const parseSlotDuration = (slot = '') => {
  if (!slot || typeof slot !== 'string' || !slot.includes('-')) return null
  const [start, end] = slot.split('-').map(item => item.trim())
  if (!start || !end) return null
  const [startHour, startMinute] = start.split(':').map(Number)
  const [endHour, endMinute] = end.split(':').map(Number)
  if ([startHour, startMinute, endHour, endMinute].some(Number.isNaN)) return null
  const startTotal = startHour * 60 + startMinute
  const endTotal = endHour * 60 + endMinute
  if (endTotal === startTotal) return null
  const durationMinutes = endTotal > startTotal
    ? endTotal - startTotal
    : 24 * 60 - startTotal + endTotal
  return Math.round((durationMinutes / 60) * 10) / 10
}

export const formatServiceTimeSlot = (slot = '') => {
  if (!slot || typeof slot !== 'string' || !slot.includes('-')) return slot || ''
  const [start, end] = slot.split('-').map(item => item.trim())
  if (!start || !end) return slot
  const [startHour, startMinute] = start.split(':').map(Number)
  const [endHour, endMinute] = end.split(':').map(Number)
  if ([startHour, startMinute, endHour, endMinute].some(Number.isNaN)) return slot
  const startTotal = startHour * 60 + startMinute
  const endTotal = endHour * 60 + endMinute
  return endTotal < startTotal ? `${start}-次日${end}` : `${start}-${end}`
}

export const getOrderDurationHours = (order = {}) => {
  const candidates = [
    order.actualDuration,
    order.consultationDuration,
    order.estimatedDuration,
  ]

  for (const candidate of candidates) {
    const value = Number(candidate)
    if (!Number.isNaN(value) && value > 0) return value
  }

  const slotDuration = parseSlotDuration(order.serviceTimeSlot)
  if (slotDuration) return slotDuration
  return null
}

export const getOrderDurationLabel = (order = {}, fallback = '—') => {
  const duration = getOrderDurationHours(order)
  if (duration == null) return fallback
  const normalized = Number(duration)
  if (Number.isInteger(normalized)) return `${normalized}小时`
  return `${normalized.toFixed(1).replace(/\.0$/, '')}小时`
}
