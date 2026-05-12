import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const readChatPage = (name) => readFileSync(new URL(`../subpkg/chat/${name}`, import.meta.url), 'utf8')

for (const page of ['chat.vue', 'chat-escort.vue']) {
  test(`${page} supports replying to a specific chat message`, () => {
    const source = readChatPage(page)

    assert.match(source, /const replyTarget = ref\(null\)/)
    assert.match(source, /@click\.stop="selectReplyTarget\(msg\)"/)
    assert.match(source, /class="reply-preview"/)
    assert.match(source, /replyToMessageId: activeReply\.id/)
    assert.match(source, /replyToContent: activeReply\.content/)
    assert.match(source, /replyToSenderName: activeReply\.senderName/)
    assert.match(source, /v-if="msg\.replyToContent"/)
  })
}
