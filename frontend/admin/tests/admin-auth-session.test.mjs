import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ADMIN_AUTH_EXPIRED_CODE,
  buildAdminLoginUrl,
  createAuthExpiredError,
  decodeJwtPayload,
  isAuthExpiredError,
  isJwtExpired,
  toAdminRoutePath,
} from '../src/utils/admin-auth-session.js'

const makeJwt = (payload) => {
  const encodedPayload = Buffer.from(JSON.stringify(payload)).toString('base64url')
  return `header.${encodedPayload}.signature`
}

test('isJwtExpired only treats tokens with expired exp as expired', () => {
  assert.equal(isJwtExpired(makeJwt({ exp: 99 }), 100), true)
  assert.equal(isJwtExpired(makeJwt({ exp: 101 }), 100), false)
  assert.equal(isJwtExpired(makeJwt({ role: 'SUPER_ADMIN' }), 100), false)
  assert.equal(isJwtExpired('not-a-jwt', 100), false)
})

test('decodeJwtPayload returns parsed payload when token is decodable', () => {
  assert.deepEqual(decodeJwtPayload(makeJwt({ role: 'ADMIN', exp: 101 })), {
    role: 'ADMIN',
    exp: 101,
  })
})

test('buildAdminLoginUrl preserves current admin route as redirect', () => {
  assert.equal(
    buildAdminLoginUrl('/admin/orders', '?selectedId=12'),
    '/admin/login?redirect=%2Forders%3FselectedId%3D12',
  )
  assert.equal(buildAdminLoginUrl('/admin/login', ''), '/admin/login')
  assert.equal(toAdminRoutePath('/admin/dashboard', ''), '/dashboard')
})

test('createAuthExpiredError marks errors as silent auth expiration errors', () => {
  const error = createAuthExpiredError()
  assert.equal(error.code, ADMIN_AUTH_EXPIRED_CODE)
  assert.equal(error.silent, true)
  assert.equal(isAuthExpiredError(error), true)
  assert.equal(isAuthExpiredError(new Error('普通错误')), false)
})
