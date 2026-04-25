import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getQualificationPreviewUrls,
  normalizeQualificationStatus,
} from '../utils/qualification.mjs'

test('qualification uploaded status requires a displayable file url', () => {
  const normalized = normalizeQualificationStatus({
    idCardUploaded: true,
    practiceCertUploaded: true,
    healthCertUploaded: true,
  })

  assert.equal(normalized.idCardUploaded, false)
  assert.equal(normalized.practiceCertUploaded, false)
  assert.equal(normalized.healthCertUploaded, false)
})

test('qualification preview falls back to scan urls when original urls are missing', () => {
  const normalized = normalizeQualificationStatus({
    idCardFrontScanFileUrl: '/uploads/front-scan.jpg',
    idCardBackScanFileUrl: '/uploads/back-scan.jpg',
    practiceCertScanFileUrl: '/uploads/practice-scan.jpg',
    healthCertScanFileUrl: '/uploads/health-scan.jpg',
  })

  assert.equal(normalized.idCardUploaded, true)
  assert.equal(normalized.practiceCertUploaded, true)
  assert.equal(normalized.healthCertUploaded, true)
  assert.deepEqual(getQualificationPreviewUrls(normalized), [
    '/uploads/front-scan.jpg',
    '/uploads/back-scan.jpg',
    '/uploads/practice-scan.jpg',
    '/uploads/health-scan.jpg',
  ])
})
