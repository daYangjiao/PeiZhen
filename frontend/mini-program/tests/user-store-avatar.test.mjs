import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../stores/user.js', import.meta.url), 'utf8')
const profileSource = readFileSync(new URL('../pages/role-user/profile.vue', import.meta.url), 'utf8')

test('user store avatar getter prefers avatar before avatarUrl to avoid stale session urls', () => {
  const getterBlock = source.match(/avatar:\s*\(state\)\s*=>\s*\{([\s\S]*?)\n    \},/)
  assert.ok(getterBlock, 'expected avatar getter block to exist')
  assert.match(getterBlock[1], /return\s+state\.userInfo\.avatar\s*\|\|\s*state\.userInfo\.avatarUrl\s*\|\|\s*''/)
})

test('uploadAvatar keeps avatar and avatarUrl in sync after uploading a user avatar', () => {
  const apiSource = readFileSync(new URL('../api/user.js', import.meta.url), 'utf8')
  const uploadAvatarBlock = apiSource.match(/export const uploadAvatar = async[\s\S]*?\n\}/)
  assert.ok(uploadAvatarBlock, 'expected uploadAvatar implementation to exist')
  assert.match(uploadAvatarBlock[0], /store\.setUserInfo\(\{\s*\.\.\.userInfo,\s*avatar:\s*avatarPath,\s*avatarUrl:\s*avatarPath\s*\}\)/)
})

test('role-user profile refresh syncs both avatar and avatarUrl from latest user detail', () => {
  assert.match(profileSource, /const nextAvatar = userDetailResponse\.data\.avatar \|\| userDetailResponse\.data\.avatarUrl \|\| ''/)
  assert.match(profileSource, /avatar:\s*nextAvatar,\s*[\r\n]+\s*avatarUrl:\s*nextAvatar/)
})
