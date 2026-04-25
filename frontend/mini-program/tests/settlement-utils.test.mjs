import test from 'node:test'
import assert from 'node:assert/strict'

import {
  calculateAttendantIncome,
  calculatePlatformFee,
  normalizeFinalOrderAmount,
} from '../utils/settlement.mjs'

test('normalizeFinalOrderAmount uses final orderAmount without adding balance again', () => {
  assert.equal(
    normalizeFinalOrderAmount({ orderAmount: '230.00', balanceAmount: '60.00', orderStatus: 6 }),
    230,
  )
})

test('calculateAttendantIncome returns post-commission income', () => {
  assert.equal(
    calculateAttendantIncome({ orderAmount: '230.00', balanceAmount: '60.00', orderStatus: 6 }),
    207,
  )
})

test('calculatePlatformFee returns platform commission', () => {
  assert.equal(calculatePlatformFee({ orderAmount: '230.00', orderStatus: 6 }), 23)
})
