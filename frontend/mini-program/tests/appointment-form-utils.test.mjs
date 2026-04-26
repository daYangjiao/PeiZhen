import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatSelectedEndTimeDisplay,
  isCrossDayEndTime,
  normalizeContactName,
  normalizeContactPhone,
} from '../utils/appointment-form.mjs'

test('contact inputs keep user text while trimming leading whitespace', () => {
  assert.equal(normalizeContactName('  张三'), '张三')
  assert.equal(normalizeContactName('张 三'), '张 三')
})

test('phone input keeps only digits and limits to eleven numbers', () => {
  assert.equal(normalizeContactPhone(' 1a8 65068003799'), '18650680037')
})

test('end time display marks only cross-day ranges', () => {
  assert.equal(formatSelectedEndTimeDisplay('23:30', '00:30'), '次日00:30')
  assert.equal(formatSelectedEndTimeDisplay('08:30', '10:00'), '10:00')
  assert.equal(isCrossDayEndTime('23:30', '00:30'), true)
  assert.equal(isCrossDayEndTime('08:30', '10:00'), false)
})
