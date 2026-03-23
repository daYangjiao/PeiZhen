export const PUBLIC_SAFE_NOTICE = '当前网站为备案展示版，完整业务功能仅向小程序内测成员开放'

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
