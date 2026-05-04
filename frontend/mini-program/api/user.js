import { get, put, upload } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'

export const getUserInfo = (userId) => {
  return get(`/api/users/${userId}`)
}

export const getUserById = (userId) => {
  return get(`/api/users/${userId}`)
}

export const getCurrentUserInfo = () => {
  return get('/api/users/current')
}

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
    id: userId
  }

  if (data.name !== undefined) requestData.name = data.name
  if (data.sex !== undefined) requestData.sex = data.sex
  if (data.age !== undefined) requestData.age = data.age
  if (data.phone !== undefined) requestData.phone = data.phone
  if (data.password !== undefined && data.password !== '') requestData.password = data.password
  if (data.avatar !== undefined) requestData.avatar = data.avatar

  return put(`/api/users/${userId}`, requestData)
}

export const uploadAvatar = async (filePath) => {
  const store = useUserStore()
  const userInfo = store.userInfo || {}
  const userId = userInfo.id || userInfo.userId
  if (!userId) {
    return Promise.reject(new Error('请先登录'))
  }

  const uploadRes = await upload('/api/common/upload-avatar', filePath, {}, 'file')
  const avatarPath = uploadRes.data
  if (!avatarPath) {
    return Promise.reject(new Error('上传失败'))
  }

  await put(`/api/users/${userId}`, { id: userId, avatar: avatarPath })
  store.setUserInfo({ ...userInfo, avatar: avatarPath, avatarUrl: avatarPath })
  return { code: 200, data: { avatarUrl: avatarPath } }
}

export const uploadPublicAvatarImage = async (filePath) => {
  const uploadRes = await upload('/api/common/upload-avatar', filePath, {}, 'file')
  const avatarPath = uploadRes.data
  if (!avatarPath) {
    return Promise.reject(new Error('上传失败'))
  }
  return { code: 200, data: { avatarUrl: avatarPath } }
}

export const uploadEscortAvatar = async (filePath) => {
  const uploadRes = await upload('/attendant/profile/avatar', filePath, {}, 'file')
  const avatarPath = uploadRes?.data?.avatarUrl || uploadRes?.data || uploadRes?.url
  if (!avatarPath) {
    return Promise.reject(new Error('头像上传失败'))
  }
  return { code: 200, data: { avatarUrl: avatarPath } }
}
