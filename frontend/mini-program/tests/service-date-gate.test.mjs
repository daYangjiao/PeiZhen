import assert from 'node:assert/strict'
import test from 'node:test'
import { readFileSync } from 'node:fs'

import {
  getServiceDateGateText,
  isBeforeServiceDate,
} from '../utils/service-date-gate.mjs'

test('service date gate blocks verification before the scheduled date', () => {
  const now = new Date('2026-05-07T10:00:00+08:00')

  assert.equal(isBeforeServiceDate('2026-05-12', now), true)
  assert.equal(getServiceDateGateText('2026-05-12', now), '预约服务日期为2026-05-12，未到服务日期暂不能核销开始服务')
})

test('service date gate allows today, past dates, and unparseable legacy values', () => {
  const now = new Date('2026-05-12T10:00:00+08:00')

  assert.equal(isBeforeServiceDate('2026-05-12', now), false)
  assert.equal(isBeforeServiceDate('2026-05-11', now), false)
  assert.equal(isBeforeServiceDate('2026年05月12日', now), false)
})

test('escort detail page checks service date before scan verification actions', () => {
  const source = readFileSync(new URL('../subpkg/order/escort-detail.vue', import.meta.url), 'utf8')

  assert.match(source, /import \{ getServiceDateGateText, isBeforeServiceDate \} from '@\/utils\/service-date-gate\.mjs'/)
  assert.match(source, /isBeforeServiceDate\(\)\s*\{[\s\S]*return isBeforeServiceDate\(this\.orderInfo\?\.serviceDate\)/)
  assert.match(source, /onScanCodeClick\(\)\s*\{[\s\S]*if \(this\.isBeforeServiceDate\) \{[\s\S]*this\.showServiceDateGateToast\(\)/)
  assert.match(source, /onSimulateScanClick\(\)\s*\{[\s\S]*if \(this\.isBeforeServiceDate\) \{[\s\S]*this\.showServiceDateGateToast\(\)/)
})
