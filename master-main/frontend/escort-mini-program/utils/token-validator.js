/**
 * 陪诊师端Token有效性验证工具
 * 用于在应用启动时检测token是否有效
 */

import { clearUserInfo, navigateToLogin } from '@/utils/auth.js'
import { get } from '@/utils/api.js'

/**
 * 验证JWT token是否有效
 * @param {string} token JWT token
 * @returns {Promise<boolean>} token是否有效
 */
async function validateToken(token) {
  if (!token) {
    return false
  }

  try {
    // 调用后端接口验证token有效性
    const response = await get('/api/users/current')
    return response && response.code === 200
  } catch (error) {
    console.log('Token验证失败:', error.message)
    // 如果是401错误，说明token无效
    if (error.statusCode === 401) {
      return false
    }
    // 其他网络错误暂时认为token有效
    return true
  }
}

/**
 * 检查token过期时间
 * @param {string} token JWT token
 * @returns {boolean} 是否即将过期（1小时内）
 */
function isTokenExpiringSoon(token) {
  if (!token) return true
  
  try {
    // 解析JWT payload
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp // 过期时间戳（秒）
    const now = Math.floor(Date.now() / 1000) // 当前时间戳（秒）
    
    // 如果距离过期小于1小时，认为即将过期
    return (exp - now) < 3600
  } catch (error) {
    console.error('解析token失败:', error)
    return true // 解析失败认为需要刷新
  }
}

/**
 * 应用启动时的token检测和处理
 * @returns {Promise<void>}
 */
export async function startupTokenCheck() {
  console.log('开始启动时token检测...')
  
  let token = null
  let isLoggedIn = false
  
  // 从本地存储获取登录状态
  token = uni.getStorageSync('token')
  isLoggedIn = !!uni.getStorageSync('isLogin')
  
  console.log('当前登录状态:', isLoggedIn)
  console.log('Token存在:', !!token)
  
  // 如果没有登录状态，直接返回
  if (!isLoggedIn) {
    console.log('陪诊师未登录，跳过token检测')
    return
  }
  
  // 如果没有token，清理登录状态
  if (!token) {
    console.log('检测到登录状态但无token，清理登录状态')
    clearUserInfo()
    return
  }
  
  // 检查token是否即将过期
  if (isTokenExpiringSoon(token)) {
    console.log('Token即将过期，建议刷新')
    // 这里可以考虑自动刷新token的逻辑
  }
  
  // 验证token有效性
  const isValid = await validateToken(token)
  console.log('Token有效性验证结果:', isValid)
  
  if (!isValid) {
    console.log('Token已失效，执行自动登出')
    
    // 清理用户信息
    clearUserInfo()
    
    // 显示提示信息
    uni.showToast({
      title: '登录已过期，请重新登录',
      icon: 'none',
      duration: 2000
    })
    
    // 延迟跳转到登录页面
    setTimeout(() => {
      navigateToLogin()
    }, 2000)
  } else {
    console.log('Token验证通过，陪诊师已登录')
  }
}

/**
 * 定期检查token有效性（可选功能）
 * @param {number} interval 检查间隔（毫秒），默认30分钟
 */
export function startPeriodicTokenCheck(interval = 30 * 60 * 1000) {
  // 清除之前的定时器
  if (window.tokenCheckTimer) {
    clearInterval(window.tokenCheckTimer)
  }
  
  window.tokenCheckTimer = setInterval(async () => {
    const token = uni.getStorageSync('token')
    const isLoggedIn = !!uni.getStorageSync('isLogin')
    
    if (isLoggedIn && token && !await validateToken(token)) {
      console.log('定期检查发现token失效')
      clearUserInfo()
      
      uni.showToast({
        title: '登录已过期，请重新登录',
        icon: 'none'
      })
      
      navigateToLogin()
    }
  }, interval)
}

/**
 * 停止定期token检查
 */
export function stopPeriodicTokenCheck() {
  if (window.tokenCheckTimer) {
    clearInterval(window.tokenCheckTimer)
    window.tokenCheckTimer = null
  }
}

export default {
  startupTokenCheck,
  startPeriodicTokenCheck,
  stopPeriodicTokenCheck,
  validateToken,
  isTokenExpiringSoon
}