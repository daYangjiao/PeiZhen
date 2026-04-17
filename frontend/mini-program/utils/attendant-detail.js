const getAttendantId = (payload = {}) => {
  const candidates = [
    payload.id,
    payload.attendantId,
    payload.userId
  ]
  const hit = candidates.find((value) => value !== undefined && value !== null && `${value}`.trim() !== '')
  return hit == null ? '' : String(hit)
}

export const navigateToAttendantDetail = (payload = {}) => {
  const attendantId = getAttendantId(payload)
  if (!attendantId) {
    uni.showToast({ title: '陪诊师资料暂不可用', icon: 'none' })
    return false
  }

  uni.navigateTo({
    url: `/subpkg/profile/attendant-detail?id=${encodeURIComponent(attendantId)}`
  })
  return true
}
