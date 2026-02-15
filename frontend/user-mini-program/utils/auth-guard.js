// 权限检查工具函数
import { useUserStore } from '../stores/user.js'

/**
 * 检查用户是否已登录
 * @returns {boolean} 是否已登录
 */
export function isAuthenticated() {
  const userStore = useUserStore()
  userStore.restoreFromStorage()
  return userStore.isLoggedIn
}

/**
 * 需要登录的接口调用前检查
 * @param {Function} apiCall - 要执行的API调用函数
 * @param {Object} options - 选项配置
 * @returns {Promise} API调用结果或重定向到登录页
 */
export async function requireAuth(apiCall, options = {}) {
  const userStore = useUserStore()
  userStore.restoreFromStorage()
  
  if (!userStore.isLoggedIn) {
    // 未登录，显示提示并跳转到登录页
    uni.showToast({
      title: options.message || '请先登录',
      icon: 'none',
      duration: 2000
    })
    
    // 获取当前页面路径用于重定向
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const redirectUrl = '/' + currentPage.route
    
    setTimeout(() => {
      uni.navigateTo({
        url: '/subpkg/auth/login?redirect=' + encodeURIComponent(redirectUrl)
      })
    }, 2000)
    
    throw new Error('用户未登录')
  }
  
  // 已登录，执行API调用
  return await apiCall()
}

/**
 * 处理401错误的通用方法
 * @param {Object} error - 错误对象
 * @param {string} redirectUrl - 重定向URL
 */
export function handleAuthError(error, redirectUrl = '/pages/index/index') {
  if (error.statusCode === 401 || (error.data && error.data.code === 401)) {
    uni.showToast({
      title: '请先登录',
      icon: 'none',
      duration: 2000
    })
    
    setTimeout(() => {
      uni.navigateTo({
        url: '/subpkg/auth/login?redirect=' + encodeURIComponent(redirectUrl)
      })
    }, 2000)
    
    return true // 表示已处理
  }
  return false // 表示未处理
}