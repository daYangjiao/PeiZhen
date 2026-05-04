import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../subpkg/profile/qualification-upload.vue', import.meta.url), 'utf8')

test('qualification upload compresses certificate images before uploading and surfaces backend error messages', () => {
  assert.match(source, /const compressSelectedImage = \(filePath\) =>/)
  assert.match(source, /const preparedFilePath = key === 'idCardFront'[\s\S]*await compressSelectedImage\(filePath\)/)
  assert.match(source, /uni\.showToast\(\{ title: error\?\.message \|\| error\?\.errMsg \|\| error\?\.data\?\.message \|\| '上传失败'/)
})
