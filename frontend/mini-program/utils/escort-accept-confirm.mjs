import { formatServiceTimeSlot } from './order-display.js'
import { calculateEstimatedAttendantIncome } from './settlement.mjs'

const textOrFallback = (value, fallback = '-') => {
  const text = value == null ? '' : String(value).trim()
  return text || fallback
}

export const ACCEPT_CONFIRM_ACTIONS = [
  { key: 'cancel', label: '取消', tone: 'secondary', weight: 1 },
  { key: 'confirm', label: '确认接单', tone: 'primary', weight: 1 },
]

export const resolveAcceptConfirmIncome = (order = {}) => {
  if (order.price !== undefined && order.price !== null && order.price !== '') {
    return Number(order.price).toFixed(2)
  }
  return calculateEstimatedAttendantIncome(order).toFixed(2)
}

export const resolveAcceptConfirmTime = (order = {}) => {
  const formattedTime = textOrFallback(order.displayServiceTime || order.appointmentTime, '')
  if (formattedTime) return formattedTime
  return [order.serviceDate, formatServiceTimeSlot(order.serviceTimeSlot || '')]
    .filter(Boolean)
    .join(' ')
    .trim() || '-'
}

export const buildAcceptConfirmSummaryRows = (order = {}) => ([
  ['患者信息', textOrFallback(order.patientName || order.userName || order.contactPerson || order.userName)],
  ['服务项目', textOrFallback(order.serviceContent || order.serviceTypeName || order.serviceType)],
  ['服务医院', textOrFallback(order.hospital || order.hospitalName)],
  ['服务时间', resolveAcceptConfirmTime(order)],
  ['预计收入', `${resolveAcceptConfirmIncome(order)}元`],
])
