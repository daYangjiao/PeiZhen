export const PUBLIC_SAFE_NOTICE = '当前网站暂不开放在线预约等功能'
export const PUBLIC_SAFE_LANDING_URL = '/pages/public/index'

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
  if (message) {
    showPublicSafeNotice(message)
  }
  setTimeout(() => {
    uni.reLaunch({ url: PUBLIC_SAFE_LANDING_URL })
  }, 220)
  return true
}
