import { useSessionStore } from '@/stores/session'

export const ensureRole = (requiredRole) => {
  const session = useSessionStore()
  if (!session.isLoggedIn || !session.token) {
    uni.showToast({ title: '当前操作需要登录哦～', icon: 'none' })
    try {
      const pages = getCurrentPages()
      const current = pages && pages.length ? pages[pages.length - 1] : null
      const route = current ? (current.route || current.__route__ || '') : ''
      if (route) {
        uni.setStorageSync('guard_from_route', route)
      }
    } catch (e) {}
    const params = []
    if (requiredRole) params.push(`role=${requiredRole}`)
    params.push('from=guard')
    const query = `?${params.join('&')}`
    setTimeout(() => {
      uni.navigateTo({ url: `/pages/auth/login${query}` })
    }, 600)
    return false
  }
  if (requiredRole && session.role && session.role !== requiredRole) {
    // 身份不匹配
    // 特殊处理：当前是陪诊师身份，却访问用户端页面 -> 保持陪诊师登录状态，直接送回陪诊师首页
    if (session.role === 'escort' && requiredRole === 'user') {
      uni.showToast({ title: '当前为陪诊师身份，请在陪诊师端使用～', icon: 'none' })
      setTimeout(() => {
        uni.reLaunch({ url: '/pages/role-escort/hall' })
      }, 500)
      return false
    }

    uni.showToast({ title: '当前身份无权访问此页面，请重新登录选择正确身份～', icon: 'none' })
    setTimeout(() => {
      const params = []
      if (requiredRole) params.push(`role=${requiredRole}`)
      params.push('from=guard')
      const query = `?${params.join('&')}`
      uni.navigateTo({ url: `/pages/auth/login${query}` })
    }, 500)
    return false
  }
  return true
}

