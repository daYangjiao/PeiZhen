import test from 'node:test'
import assert from 'node:assert/strict'

import {
  isWechatBrowser,
  parseWechatOAuthPayload,
  resolveWechatLoginPlatform,
} from '../utils/wechat-login.mjs'

test('resolveWechatLoginPlatform prefers mini program and app runtime before h5 browser', () => {
  assert.equal(resolveWechatLoginPlatform({ isMiniProgram: true, isApp: true, userAgent: 'MicroMessenger' }), 'MINI_PROGRAM')
  assert.equal(resolveWechatLoginPlatform({ isApp: true, userAgent: 'MicroMessenger' }), 'APP')
  assert.equal(resolveWechatLoginPlatform({ userAgent: 'Mozilla MicroMessenger' }), 'WECHAT_H5')
  assert.equal(resolveWechatLoginPlatform({ userAgent: 'Mozilla Safari' }), '')
})

test('isWechatBrowser detects wechat embedded browser', () => {
  assert.equal(isWechatBrowser('Mozilla/5.0 MicroMessenger/8.0'), true)
  assert.equal(isWechatBrowser('Mozilla/5.0 Chrome/120'), false)
})

test('parseWechatOAuthPayload reads token and bind token from h5 hash route', () => {
  assert.deepEqual(parseWechatOAuthPayload('#/pages/auth/login?role=user&wechatToken=abc'), {
    token: 'abc',
    bindToken: '',
  })
  assert.deepEqual(parseWechatOAuthPayload('#/pages/auth/login?wechatBindToken=bind-1'), {
    token: '',
    bindToken: 'bind-1',
  })
})
