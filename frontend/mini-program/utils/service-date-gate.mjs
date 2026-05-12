const formatDateKey = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const normalizeServiceDateKey = (value = '') => {
  const raw = String(value || '').trim()
  const match = raw.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})$/)
  if (!match) return ''
  const [, year, month, day] = match
  return `${year}-${String(Number(month)).padStart(2, '0')}-${String(Number(day)).padStart(2, '0')}`
}

export const isBeforeServiceDate = (serviceDate, now = new Date()) => {
  const serviceDateKey = normalizeServiceDateKey(serviceDate)
  if (!serviceDateKey) return false
  const todayKey = formatDateKey(now)
  if (!todayKey) return false
  return todayKey < serviceDateKey
}

export const getServiceDateGateText = (serviceDate, now = new Date()) => {
  if (!isBeforeServiceDate(serviceDate, now)) return ''
  return `预约服务日期为${normalizeServiceDateKey(serviceDate)}，未到服务日期暂不能核销开始服务`
}
