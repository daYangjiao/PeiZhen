import { get, post, put, upload } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

// 获取个人信息
export const getUserInfo = (userId) => {
  return get(`/api/users/${userId}`)
}

// 通过用户ID获取用户详细信息
export const getUserById = (userId) => {
  return get(`/api/users/${userId}`)
}

// 获取当前登录用户信息
export const getCurrentUserInfo = () => {
  return get('/api/users/current')
}

// 更新个人信息（兼容旧小程序逻辑）
export const updateUserInfo = (data) => {
  const store = useUserStore()
  const userInfo = store.userInfo || {}
  const userId =
    data.userId ||
    data.id ||
    userInfo.userId ||
    userInfo.id

  if (!userId) {
    return Promise.reject(new Error('用户ID不存在'))
  }

  const requestData = {
    id: userId,
  }

  if (data.name !== undefined) requestData.name = data.name
  if (data.sex !== undefined) requestData.sex = data.sex
  if (data.age !== undefined) requestData.age = data.age
  if (data.phone !== undefined) requestData.phone = data.phone
  if (data.password !== undefined && data.password !== '') requestData.password = data.password
  if (data.avatar !== undefined) requestData.avatar = data.avatar

  return put(`/api/users/${userId}`, requestData)
}

// 上传头像：先上传图片，再更新用户 avatar 字段
export const uploadAvatar = async (filePath) => {
  const store = useUserStore()
  const userInfo = store.userInfo || {}
  const userId = userInfo.id || userInfo.userId
  if (!userId) {
    return Promise.reject(new Error('请先登录'))
  }

  // 1. 上传图片，后端返回 /uploads/xxx
  const uploadRes = await upload('/api/common/upload-image', filePath, {}, 'file')
  const avatarPath = uploadRes.data
  if (!avatarPath) {
    return Promise.reject(new Error('上传失败'))
  }

  // 2. 更新用户头像字段
  await put(`/api/users/${userId}`, { id: userId, avatar: avatarPath })

  // 3. 同步到本地 store
  store.setUserInfo({ ...userInfo, avatar: avatarPath })
  return { code: 200, data: { avatarUrl: avatarPath } }
}
