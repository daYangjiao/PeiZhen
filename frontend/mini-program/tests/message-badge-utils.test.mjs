import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatBadgeText,
  normalizeBadgeCount,
} from '../utils/message-badge.mjs'

test('normalizeBadgeCount clamps invalid and negative counts to zero', () => {
  assert.equal(normalizeBadgeCount(undefined), 0)
  assert.equal(normalizeBadgeCount(null), 0)
  assert.equal(normalizeBadgeCount(-3), 0)
  assert.equal(normalizeBadgeCount(Number.NaN), 0)
})

test('normalizeBadgeCount keeps positive integer badge counts', () => {
  assert.equal(normalizeBadgeCount(1), 1)
  assert.equal(normalizeBadgeCount('12'), 12)
  assert.equal(normalizeBadgeCount(9.8), 9)
})

test('formatBadgeText hides zero and caps large counts', () => {
  assert.equal(formatBadgeText(0), '')
  assert.equal(formatBadgeText(9), '9')
  assert.equal(formatBadgeText(100), '99+')
})
