import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatBadgeText,
  normalizeBadgeCount,
  resolveRealtimeUnreadTarget,
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

test('resolveRealtimeUnreadTarget routes incoming chat messages to contact unread', () => {
  assert.deepEqual(resolveRealtimeUnreadTarget({
    senderId: 21,
    receiverId: 12,
    msgType: 1
  }, { currentUserId: 12, token: 'token', isLoggedIn: true }), {
    type: 'contact',
    contactId: 21
  })
})

test('resolveRealtimeUnreadTarget routes system messages to system unread', () => {
  assert.deepEqual(resolveRealtimeUnreadTarget({
    senderId: 0,
    receiverId: 12,
    msgType: 1
  }, { currentUserId: 12, token: 'token', isLoggedIn: true }), {
    type: 'system'
  })
})

test('resolveRealtimeUnreadTarget ignores read receipts, own messages and invalid sessions', () => {
  assert.equal(resolveRealtimeUnreadTarget({ type: 'READ_RECEIPT', msgType: 99 }, { currentUserId: 12, token: 'token', isLoggedIn: true }), null)
  assert.equal(resolveRealtimeUnreadTarget({ senderId: 12, receiverId: 21, msgType: 1 }, { currentUserId: 12, token: 'token', isLoggedIn: true }), null)
  assert.equal(resolveRealtimeUnreadTarget({ senderId: 21, receiverId: 12, msgType: 1 }, { currentUserId: 0, token: 'token', isLoggedIn: true }), null)
  assert.equal(resolveRealtimeUnreadTarget({ senderId: 21, receiverId: 12, msgType: 1 }, { currentUserId: 12, token: '', isLoggedIn: true }), null)
})
