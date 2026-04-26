import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatPraiseRate,
  formatRatingScore,
  getRatingStarStates,
} from '../utils/rating.js'

test('formatRatingScore hides score when there are no evaluations', () => {
  assert.equal(formatRatingScore(null, 0), '暂无评分')
  assert.equal(formatPraiseRate(0, 0), '暂无评价')
})

test('getRatingStarStates renders half stars for decimal average ratings', () => {
  assert.deepEqual(getRatingStarStates(4.5, 3), ['full', 'full', 'full', 'full', 'half'])
  assert.deepEqual(getRatingStarStates(4.4, 3), ['full', 'full', 'full', 'full', 'half'])
  assert.deepEqual(getRatingStarStates(4.2, 3), ['full', 'full', 'full', 'full', 'empty'])
})

test('formatPraiseRate shows percentage only when evaluation count exists', () => {
  assert.equal(formatPraiseRate(67, 3), '67%')
  assert.equal(formatPraiseRate(null, 1), '0%')
  assert.equal(formatPraiseRate(100, undefined), '暂无评价')
})
