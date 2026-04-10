// Token测试工具
import { getToken } from '../utils/api.js'
import { getUserInfo } from '../utils/auth.js'

/**
 * 测试当前token状态
 */
export function testTokenStatus() {
  console.log('=== Token状态测试 ===')
  
  // 检查storage中的token
  const storageToken = uni.getStorageSync('token')
  console.log('Storage中的token:', storageToken ? storageToken.substring(0, 20) + '...' : '无')
  
  // 检查用户信息中的token
  const userInfo = getUserInfo()
  console.log('用户信息中的token:', userInfo?.token ? userInfo.token.substring(0, 20) + '...' : '无')
  
  // 检查通过getToken函数获取的token
  const apiToken = getToken()
  console.log('API getToken函数返回的token:', apiToken ? apiToken.substring(0, 20) + '...' : '无')
  
  // 验证token格式
  if (apiToken) {
    console.log('Token长度:', apiToken.length)
    console.log('Token是否包含Bearer前缀:', apiToken.includes('Bearer'))
    console.log('Token是否包含点号(.JWT格式):', apiToken.includes('.'))
  }
  
  console.log('=== 测试结束 ===')
  return {
    storageToken,
    userInfoToken: userInfo?.token,
    apiToken,
    isValid: !!apiToken
  }
}

/**
 * 手动测试API请求
 */
export async function testAPIRequest() {
  console.log('=== API请求测试 ===')
  
  try {
    // 导入API工具
    const { get } = await import('../utils/api.js')
    
    const response = await get('/api/users/15') // 测试获取当前用户信息
    console.log('API测试响应:', response)
    return response
  } catch (error) {
    console.error('API测试失败:', error)
    return error
  }
}