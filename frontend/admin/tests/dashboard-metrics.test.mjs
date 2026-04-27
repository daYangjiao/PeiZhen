import assert from 'node:assert/strict'
import test from 'node:test'

import { buildDashboardMetricCards } from '../src/utils/dashboard-metrics.js'

test('dashboard metrics split total users, patient users, and attendants', () => {
  const cards = buildDashboardMetricCards({
    totalUsers: 7,
    totalPatientUsers: 4,
    totalAttendants: 3,
    pendingAttendantReviews: 2,
    totalOrders: 9,
    todayOrders: 1,
    disputeOrders: 0,
    todayOperationCount: 5,
    recentOperationLogs: [{ id: 1 }],
  })

  assert.deepEqual(cards.slice(0, 3).map((card) => [card.label, card.value, card.badge]), [
    ['总用户', 7, '全部'],
    ['患者用户', 4, '患者'],
    ['陪诊师', 3, '陪诊师'],
  ])
  assert.equal(cards.some((card) => card.label === '用户总量'), false)
  assert.equal(cards.some((card) => card.label === '订单总量'), false)
})
