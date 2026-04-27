import assert from 'node:assert/strict'
import test from 'node:test'

import {
  ACCEPT_CONFIRM_ACTIONS,
  buildAcceptConfirmSummaryRows,
} from '../utils/escort-accept-confirm.mjs'

test('accept confirm summary uses order details needed before accepting', () => {
  const rows = buildAcceptConfirmSummaryRows({
    patientName: '王女士',
    serviceContent: '普通陪诊',
    hospital: '华西医院',
    serviceDate: '2026-04-27',
    serviceTimeSlot: '09:00-11:30',
    orderAmount: 200,
  })

  assert.deepEqual(rows, [
    ['患者信息', '王女士'],
    ['服务项目', '普通陪诊'],
    ['服务医院', '华西医院'],
    ['服务时间', '2026-04-27 09:00-11:30'],
    ['预计收入', '180.00元'],
  ])
})

test('accept confirm actions use equal button weight', () => {
  assert.deepEqual(ACCEPT_CONFIRM_ACTIONS.map((action) => action.weight), [1, 1])
})
