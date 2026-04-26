const STATUS_TEXT_MAP = {
  userType: {
    0: '普通用户',
    1: '陪诊师',
    2: '管理员'
  },
  userStatus: {
    0: '禁用',
    1: '正常'
  },
  attendantAuditStatus: {
    0: '待审核',
    1: '已通过',
    2: '未通过'
  },
  orderStatus: {
    0: '待支付',
    1: '待接单',
    2: '待服务',
    3: '服务中',
    4: '待确认时长',
    5: '争议处理中',
    6: '已完成',
    7: '已取消',
    8: '专属派单待确认',
    9: '待补差额',
    10: '待平台退款'
  },
  paymentStatus: {
    0: '待支付',
    1: '已支付'
  }
}

const normalizeStatusValue = (value) => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}

export const getMappedStatusText = (group, value, fallback = '-') => {
  const numericValue = normalizeStatusValue(value)
  if (numericValue === null) return fallback
  return STATUS_TEXT_MAP[group]?.[numericValue] ?? fallback
}

export const getStatusMapEntries = (group) => STATUS_TEXT_MAP[group] || {}
