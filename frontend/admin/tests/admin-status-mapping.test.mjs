import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getMappedStatusText,
} from '../../shared/admin-status-mapping.js'

test('attendant audit status distinguishes incomplete qualifications from pending review', () => {
  assert.equal(getMappedStatusText('attendantAuditStatus', 3), '待补充')
})
