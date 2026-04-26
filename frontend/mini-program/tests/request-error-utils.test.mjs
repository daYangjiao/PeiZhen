import test from 'node:test'
import assert from 'node:assert/strict'

import { isUnauthorizedResponse } from '../utils/request-error.mjs'

test('isUnauthorizedResponse detects direct and wrapped 401 responses', () => {
  assert.equal(isUnauthorizedResponse({ statusCode: 401 }), true)
  assert.equal(isUnauthorizedResponse({ code: 401 }), true)
  assert.equal(isUnauthorizedResponse({ data: { code: 401 } }), true)
  assert.equal(isUnauthorizedResponse({ statusCode: 400, code: 400 }), false)
})
