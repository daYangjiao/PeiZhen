import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../subpkg/order/submit-time-fee.vue', import.meta.url), 'utf8')

test('submit time fee step buttons reset native button chrome and center both symbols', () => {
  const stepButtonBlock = source.match(/\.step-btn\s*\{[\s\S]*?\n\}/)
  assert.ok(stepButtonBlock, 'expected .step-btn style block to exist')
  assert.match(source, /<button class="step-btn" @click="changeDuration\(-0\.5\)">[\s\S]*<text class="step-symbol minus">-<\/text>/)
  assert.match(source, /<button class="step-btn" @click="changeDuration\(0\.5\)">[\s\S]*<text class="step-symbol plus">\+<\/text>/)
  assert.match(stepButtonBlock[0], /display:\s*flex;/)
  assert.match(stepButtonBlock[0], /align-items:\s*center;/)
  assert.match(stepButtonBlock[0], /justify-content:\s*center;/)
  assert.match(stepButtonBlock[0], /padding:\s*0;/)
  assert.match(stepButtonBlock[0], /box-sizing:\s*border-box;/)
  assert.match(source, /\.step-btn::after\s*\{[\s\S]*border:\s*none;/)
  assert.match(source, /\.step-symbol\s*\{[\s\S]*text-align:\s*center;/)
  assert.match(source, /\.step-symbol\.minus\s*\{[\s\S]*font-size:\s*34rpx;/)
  assert.match(source, /\.step-symbol\.plus\s*\{[\s\S]*font-size:\s*30rpx;/)
})
