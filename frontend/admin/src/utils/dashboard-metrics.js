export const buildDashboardMetricCards = (dashboard = {}) => {
  const totalUsers = dashboard.totalUsers || 0
  const totalAttendants = dashboard.totalAttendants || 0
  const totalPatientUsers = dashboard.totalPatientUsers ?? Math.max(totalUsers - totalAttendants, 0)

  const cards = [
    {
      label: '总用户',
      value: totalUsers,
      hint: '患者+陪诊师',
      badge: '全部',
      tone: 'neutral',
    },
    {
      label: '患者用户',
      value: totalPatientUsers,
      hint: '普通用户',
      badge: '患者',
      tone: 'neutral',
    },
    {
      label: '陪诊师',
      value: totalAttendants,
      hint: '陪诊师账号',
      badge: '陪诊师',
      tone: 'success',
    },
    {
      label: '待审陪诊师',
      value: dashboard.pendingAttendantReviews || 0,
      hint: '待审核',
      badge: '审核',
      tone: 'warning',
    },
    {
      label: '今日订单',
      value: dashboard.todayOrders || 0,
      hint: '今日',
      badge: '今日',
      tone: 'success',
    },
    {
      label: '争议订单',
      value: dashboard.disputeOrders || 0,
      hint: '争议',
      badge: '争议',
      tone: 'danger',
    },
  ]

  if (dashboard.recentOperationLogs?.length) {
    cards.push({
      label: '今日处理',
      value: dashboard.todayOperationCount || 0,
      hint: '后台操作',
      badge: '日志',
      tone: 'neutral',
    })
  }

  return cards
}
