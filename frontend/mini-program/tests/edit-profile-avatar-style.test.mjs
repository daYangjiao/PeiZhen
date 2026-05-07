import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../subpkg/profile/edit-profile.vue', import.meta.url), 'utf8')
const cropSource = readFileSync(new URL('../subpkg/profile/avatar-crop.vue', import.meta.url), 'utf8')

test('edit profile avatar uses a dedicated rounded shell for shadow instead of shadowing the image directly', () => {
  assert.match(source, /<view class="avatar-shell"[\s\S]*<image[\s\S]*class="avatar-img"/)
  assert.match(source, /\.avatar-shell\s*\{[\s\S]*box-shadow:/)
  const avatarImageBlock = source.match(/\.avatar-img\s*\{[\s\S]*?\n\}/)
  assert.ok(avatarImageBlock, 'expected .avatar-img style block to exist')
  assert.doesNotMatch(avatarImageBlock[0], /box-shadow:/)
})

test('avatar crop page does not draw a circular crop shadow over the square avatar frame', () => {
  const cropMaskBlock = cropSource.match(/\.crop-mask\s*\{[\s\S]*?\n\}/)
  assert.ok(cropMaskBlock, 'expected .crop-mask style block to exist')
  assert.doesNotMatch(cropMaskBlock[0], /radial-gradient\s*\(\s*circle/i)
  assert.doesNotMatch(cropMaskBlock[0], /border-radius:\s*50%/)
  assert.match(cropSource, /\.crop-stage\s*\{[\s\S]*border-radius:\s*32rpx/)
  assert.match(cropSource, /\.crop-frame\s*\{[\s\S]*border-radius:\s*32rpx/)
})
