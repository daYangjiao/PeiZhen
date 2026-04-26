import test from 'node:test'
import assert from 'node:assert/strict'

import {
  calculateServiceAmountByDuration,
  formatDisputeAmount,
  suggestDisputeFinalAmount,
} from '../src/utils/dispute-settlement.js'

test('calculateServiceAmountByDuration follows backend service fee policy', () => {
  assert.equal(calculateServiceAmountByDuration(1.5, 1), 50)
  assert.equal(calculateServiceAmountByDuration(4.5, 1), 140)
  assert.equal(calculateServiceAmountByDuration(2.1, 3), 180)
  assert.equal(calculateServiceAmountByDuration(2.1, 4), 110)
  assert.equal(calculateServiceAmountByDuration(2.1, 2), 135)
})

test('suggestDisputeFinalAmount recalculates amount from edited duration and service type', () => {
  assert.equal(suggestDisputeFinalAmount({ clinicType: 1, orderAmount: 170 }, 3.5), '110.00')
  assert.equal(suggestDisputeFinalAmount({ clinicType: 2, orderAmount: 170 }, 3.5), '180.00')
})

test('suggestDisputeFinalAmount falls back to existing paid amount plus balance when duration is invalid', () => {
  assert.equal(suggestDisputeFinalAmount({ orderAmount: 170, balanceAmount: 20 }, ''), '190.00')
  assert.equal(formatDisputeAmount('90'), '90.00')
})
