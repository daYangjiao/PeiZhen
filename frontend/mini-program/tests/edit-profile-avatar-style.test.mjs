import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../subpkg/profile/edit-profile.vue', import.meta.url), 'utf8')

test('edit profile avatar uses a dedicated rounded shell for shadow instead of shadowing the image directly', () => {
  assert.match(source, /<view class="avatar-shell"[\s\S]*<image[\s\S]*class="avatar-img"/)
  assert.match(source, /\.avatar-shell\s*\{[\s\S]*box-shadow:/)
  const avatarImageBlock = source.match(/\.avatar-img\s*\{[\s\S]*?\n\}/)
  assert.ok(avatarImageBlock, 'expected .avatar-img style block to exist')
  assert.doesNotMatch(avatarImageBlock[0], /box-shadow:/)
})
