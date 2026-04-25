import test from 'node:test'
import assert from 'node:assert/strict'

import { buildChatHeaderMeta } from '../utils/chat-ui.mjs'

test('chat header subtitle does not claim static online status', () => {
  const userMeta = buildChatHeaderMeta({ role: 'user' })
  const escortMeta = buildChatHeaderMeta({ role: 'escort' })

  assert.equal(userMeta.subtitle.includes('在线'), false)
  assert.equal(escortMeta.subtitle.includes('在线'), false)
})

test('chat header uses role-specific business subtitle', () => {
  assert.deepEqual(buildChatHeaderMeta({ role: 'user' }), {
    peerLabel: '陪诊师',
    subtitle: '陪诊沟通',
  })
  assert.deepEqual(buildChatHeaderMeta({ role: 'escort' }), {
    peerLabel: '用户',
    subtitle: '服务沟通',
  })
})
