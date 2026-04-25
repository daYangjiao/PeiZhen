import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getPatientOrderActionTabValue,
  hasPatientActionableOrder,
  shouldShowOrderTabBadge
} from '../utils/order-tab-badges.mjs'

test('pending payment order lights all and pending-payment tabs only', () => {
  const orders = [{ paymentStatus: 0, orderStatus: 1 }]

  assert.equal(getPatientOrderActionTabValue(orders[0]), 0)
  assert.equal(hasPatientActionableOrder(orders), true)
  assert.equal(shouldShowOrderTabBadge(null, orders), true)
  assert.equal(shouldShowOrderTabBadge(0, orders), true)
  assert.equal(shouldShowOrderTabBadge(4, orders), false)
  assert.equal(shouldShowOrderTabBadge(9, orders), false)
})

test('time confirmation order lights all and confirm-time tabs only', () => {
  const orders = [{ paymentStatus: 1, orderStatus: 4 }]

  assert.equal(getPatientOrderActionTabValue(orders[0]), 4)
  assert.equal(shouldShowOrderTabBadge(null, orders), true)
  assert.equal(shouldShowOrderTabBadge(0, orders), false)
  assert.equal(shouldShowOrderTabBadge(4, orders), true)
  assert.equal(shouldShowOrderTabBadge(9, orders), false)
})

test('balance payment order lights all and balance tabs only', () => {
  const orders = [{ paymentStatus: 1, orderStatus: 9 }]

  assert.equal(getPatientOrderActionTabValue(orders[0]), 9)
  assert.equal(shouldShowOrderTabBadge(null, orders), true)
  assert.equal(shouldShowOrderTabBadge(0, orders), false)
  assert.equal(shouldShowOrderTabBadge(4, orders), false)
  assert.equal(shouldShowOrderTabBadge(9, orders), true)
})

test('non-actionable statuses and canceled unpaid orders do not light tabs', () => {
  const orders = [
    { paymentStatus: 1, orderStatus: 1 },
    { paymentStatus: 1, orderStatus: 2 },
    { paymentStatus: 1, orderStatus: 3 },
    { paymentStatus: 1, orderStatus: 5 },
    { paymentStatus: 1, orderStatus: 6 },
    { paymentStatus: 1, orderStatus: 7 },
    { paymentStatus: 1, orderStatus: 8 },
    { paymentStatus: 0, orderStatus: 7 }
  ]

  assert.equal(hasPatientActionableOrder(orders), false)
  assert.equal(shouldShowOrderTabBadge(null, orders), false)
  assert.equal(shouldShowOrderTabBadge(0, orders), false)
  assert.equal(shouldShowOrderTabBadge(4, orders), false)
  assert.equal(shouldShowOrderTabBadge(5, orders), false)
  assert.equal(shouldShowOrderTabBadge(9, orders), false)
})
