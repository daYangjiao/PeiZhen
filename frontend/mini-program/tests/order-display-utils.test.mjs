import test from 'node:test'
import assert from 'node:assert/strict'

import { getOrderDurationLabel } from '../utils/order-display.js'

test('getOrderDurationLabel prefers estimated duration when requested explicitly', () => {
  const order = {
    estimatedDuration: 7,
    actualDuration: 6.5,
    consultationDuration: 7,
    serviceTimeSlot: '09:00-16:00',
  }

  assert.equal(getOrderDurationLabel(order, '—', { prefer: 'estimated' }), '7小时')
})
