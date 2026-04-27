import assert from 'node:assert/strict'
import test from 'node:test'

import {
  createQualificationPromptTracker,
  resolveQualificationGate,
  shouldPromptQualificationGate,
} from '../utils/escort-qualification-gate.mjs'

test('pending qualification blocks hall without popup', () => {
  const gate = resolveQualificationGate({
    qualificationStatusCode: 0,
    qualificationCompleteness: 100,
    canAcceptOrders: false,
  })

  assert.equal(gate.allowed, false)
  assert.equal(gate.state, 'pending')
  assert.equal(gate.popupRequired, false)
})

test('unsubmitted qualification prompts upload even when status is explicit incomplete', () => {
  const gate = resolveQualificationGate({
    qualificationStatusCode: 3,
    qualificationCompleteness: 0,
    canAcceptOrders: false,
  })

  assert.equal(gate.allowed, false)
  assert.equal(gate.state, 'incomplete')
  assert.equal(gate.title, '资质待补充')
  assert.equal(gate.popupRequired, true)
})

test('rejected qualification prompts only once per foreground session', () => {
  const tracker = createQualificationPromptTracker()
  const gate = resolveQualificationGate({
    qualificationStatusCode: 2,
    qualificationFailReason: '证件照片不清晰',
    canAcceptOrders: false,
  })

  assert.equal(gate.state, 'rejected')
  assert.equal(gate.popupRequired, true)
  assert.equal(shouldPromptQualificationGate({ userId: 8, gate, tracker }), true)
  assert.equal(shouldPromptQualificationGate({ userId: 8, gate, tracker }), false)
})

test('expired qualification prompts with expired state before generic rejection', () => {
  const gate = resolveQualificationGate({
    qualificationStatusCode: 1,
    practiceCertExpired: true,
    canAcceptOrders: false,
  })

  assert.equal(gate.allowed, false)
  assert.equal(gate.state, 'expired')
  assert.equal(gate.popupRequired, true)
})
