const hasText = (value) => typeof value === 'string' && value.trim().length > 0

const firstText = (...values) => {
  for (const value of values) {
    if (hasText(value)) return value.trim()
  }
  return ''
}

export const getQualificationImageFields = (profile = {}) => ({
  idCardFront: firstText(
    profile.idCardFrontFileUrl,
    profile.idCardFileUrl,
    profile.idCardFrontScanFileUrl,
  ),
  idCardBack: firstText(
    profile.idCardBackFileUrl,
    profile.idCardBackScanFileUrl,
  ),
  practiceCert: firstText(
    profile.practiceCertFileUrl,
    profile.practiceCertScanFileUrl,
  ),
  healthCert: firstText(
    profile.healthCertFileUrl,
    profile.healthCertScanFileUrl,
  ),
})

export const normalizeQualificationStatus = (profile = {}) => {
  const images = getQualificationImageFields(profile)
  return {
    ...profile,
    idCardFileUrl: firstText(profile.idCardFileUrl, images.idCardFront),
    idCardFrontFileUrl: firstText(profile.idCardFrontFileUrl, profile.idCardFileUrl, profile.idCardFrontScanFileUrl),
    idCardFrontScanFileUrl: firstText(profile.idCardFrontScanFileUrl, profile.idCardFrontFileUrl, profile.idCardFileUrl),
    idCardBackFileUrl: firstText(profile.idCardBackFileUrl, profile.idCardBackScanFileUrl),
    idCardBackScanFileUrl: firstText(profile.idCardBackScanFileUrl, profile.idCardBackFileUrl),
    practiceCertFileUrl: firstText(profile.practiceCertFileUrl, profile.practiceCertScanFileUrl),
    practiceCertScanFileUrl: firstText(profile.practiceCertScanFileUrl, profile.practiceCertFileUrl),
    healthCertFileUrl: firstText(profile.healthCertFileUrl, profile.healthCertScanFileUrl),
    healthCertScanFileUrl: firstText(profile.healthCertScanFileUrl, profile.healthCertFileUrl),
    idCardUploaded: hasText(images.idCardFront) && hasText(images.idCardBack),
    practiceCertUploaded: hasText(images.practiceCert),
    healthCertUploaded: hasText(images.healthCert),
  }
}

export const getQualificationPreviewUrls = (profile = {}) => {
  const images = getQualificationImageFields(profile)
  return [
    images.idCardFront,
    images.idCardBack,
    images.practiceCert,
    images.healthCert,
  ].filter(Boolean)
}
