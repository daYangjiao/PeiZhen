import test from 'node:test'
import assert from 'node:assert/strict'

import {
  calculateAttendantIncome,
  calculateDisplayAttendantIncome,
  calculateEstimatedAttendantIncome,
  calculatePlatformFee,
  normalizeFinalOrderAmount,
  shouldDisplayIncomeRecord,
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

test('completed refund with negative balance uses final order amount without double subtracting', () => {
  const order = { orderAmount: '140.00', balanceAmount: '-30.00', refundAmount: '30.00', orderStatus: 6 }
  assert.equal(normalizeFinalOrderAmount(order), 140)
  assert.equal(calculateAttendantIncome(order), 126)
})

test('legacy completed refund subtracts refund once when balance is missing', () => {
  const order = { orderAmount: '170.00', refundAmount: '30.00', orderStatus: 6 }
  assert.equal(normalizeFinalOrderAmount(order), 140)
  assert.equal(calculateAttendantIncome(order), 126)
})

test('backend settlement fields take precedence in wallet calculations', () => {
  const order = {
    orderAmount: '170.00',
    refundAmount: '30.00',
    orderStatus: 6,
    settlementAmount: '140.00',
    platformFeeAmount: '14.00',
    attendantIncomeAmount: '126.00',
  }
  assert.equal(normalizeFinalOrderAmount(order), 140)
  assert.equal(calculatePlatformFee(order), 14)
  assert.equal(calculateAttendantIncome(order), 126)
})

test('canceled refund order does not generate wallet income', () => {
  const order = { orderAmount: '170.00', refundAmount: '170.00', penaltyAmount: '0.00', orderStatus: 7 }
  assert.equal(normalizeFinalOrderAmount(order), 0)
  assert.equal(calculateAttendantIncome(order), 0)
  assert.equal(calculateDisplayAttendantIncome(order), 0)
})

test('wallet income list hides completed orders with zero real income', () => {
  assert.equal(shouldDisplayIncomeRecord({
    orderStatus: 6,
    settlementAmount: '0.00',
    attendantIncomeAmount: '0.00',
  }), false)
  assert.equal(shouldDisplayIncomeRecord({
    orderStatus: 6,
    settlementAmount: '170.00',
    attendantIncomeAmount: '153.00',
  }), true)
})

test('estimated income is only used before final settlement', () => {
  assert.equal(calculateEstimatedAttendantIncome('170.00'), 153)
  assert.equal(calculateDisplayAttendantIncome({ orderAmount: '170.00', orderStatus: 2 }), 153)
  assert.equal(calculateDisplayAttendantIncome({ orderAmount: '170.00', orderStatus: 8 }), 153)
})
