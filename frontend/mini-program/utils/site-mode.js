export const PUBLIC_SAFE_NOTICE = '当前网站暂不开放在线预约等功能'

export const isWeixinMiniProgramRuntime = () => typeof wx !== 'undefined' && typeof document === 'undefined'

export const isPublicSafeMode = () => {
  return !isWeixinMiniProgramRuntime()
}

export const showPublicSafeNotice = (message = PUBLIC_SAFE_NOTICE) => {
  uni.showToast({
    title: message,
    icon: 'none',
    duration: 1800
  })
}

export const redirectPublicSafeToHome = (message = PUBLIC_SAFE_NOTICE) => {
  if (!isPublicSafeMode()) return false
  showPublicSafeNotice(message)
  setTimeout(() => {
    uni.switchTab({ url: '/pages/role-user/home' })
  }, 220)
  return true
}
