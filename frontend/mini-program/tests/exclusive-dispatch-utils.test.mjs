import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildExclusiveDispatchKey,
  formatExclusiveDispatchCountdown,
  getExclusiveDispatchRemainingMs,
  isExclusiveDispatchExpired,
  shouldOpenExclusiveDispatchPopup,
} from '../utils/exclusive-dispatch.mjs'

test('buildExclusiveDispatchKey returns a stable user-order key', () => {
  assert.equal(buildExclusiveDispatchKey(12, 88), '12:88')
  assert.equal(buildExclusiveDispatchKey('12', '88'), '12:88')
  assert.equal(buildExclusiveDispatchKey(null, 88), '')
})

test('getExclusiveDispatchRemainingMs uses paymentTime and clamps at zero', () => {
  const now = Date.parse('2026-04-19T10:10:00+08:00')
  const remaining = getExclusiveDispatchRemainingMs({
    paymentTime: '2026-04-19 10:00:00',
  }, now)

  assert.equal(remaining, 5 * 60 * 1000)
  assert.equal(
    getExclusiveDispatchRemainingMs({ paymentTime: '2026-04-19 09:30:00' }, now),
    0,
  )
})

test('formatExclusiveDispatchCountdown formats minutes and seconds', () => {
  assert.equal(formatExclusiveDispatchCountdown(5 * 60 * 1000), '05:00')
  assert.equal(formatExclusiveDispatchCountdown(61 * 1000), '01:01')
  assert.equal(formatExclusiveDispatchCountdown(0), '00:00')
})

test('isExclusiveDispatchExpired is true once the confirmation window ends', () => {
  const now = Date.parse('2026-04-19T10:16:00+08:00')
  assert.equal(isExclusiveDispatchExpired({ paymentTime: '2026-04-19 10:00:00' }, now), true)
  assert.equal(isExclusiveDispatchExpired({ paymentTime: '2026-04-19 10:05:00' }, now), false)
})

test('shouldOpenExclusiveDispatchPopup blocks ignored, expired, and current-detail cases', () => {
  const key = buildExclusiveDispatchKey(9, 1001)
  const baseInput = {
    userId: 9,
    orderId: 1001,
    orderStatus: 8,
    remainingMs: 60 * 1000,
    ignoredKeys: new Set(),
    currentRoute: 'pages/role-escort/order',
    currentOrderId: 0,
  }

  assert.equal(shouldOpenExclusiveDispatchPopup(baseInput), true)
  assert.equal(
    shouldOpenExclusiveDispatchPopup({
      ...baseInput,
      ignoredKeys: new Set([key]),
    }),
    false,
  )
  assert.equal(
    shouldOpenExclusiveDispatchPopup({
      ...baseInput,
      remainingMs: 0,
    }),
    false,
  )
  assert.equal(
    shouldOpenExclusiveDispatchPopup({
      ...baseInput,
      currentRoute: 'subpkg/order/escort-detail',
      currentOrderId: 1001,
    }),
    false,
  )
  assert.equal(
    shouldOpenExclusiveDispatchPopup({
      ...baseInput,
      orderStatus: 2,
    }),
    false,
  )
})
