import { uploadPublicAvatarImage } from '@/api/user.js'

const chooseImageAsync = (options) =>
  new Promise((resolve, reject) => {
    uni.chooseImage({
      ...options,
      success: resolve,
      fail: reject,
    })
  })

const compressImageAsync = (options) =>
  new Promise((resolve, reject) => {
    uni.compressImage({
      ...options,
      success: resolve,
      fail: reject,
    })
  })

const isCancelError = (error) => {
  const message = error?.errMsg || error?.message || String(error || '')
  return /cancel/i.test(message)
}

const readSelectedFilePath = (result = {}) =>
  result.tempFilePaths?.[0] ||
  result.tempFiles?.[0]?.path ||
  result.tempFiles?.[0]?.tempFilePath ||
  ''

export const chooseAvatarFile = async () => {
  try {
    const cropped = await chooseImageAsync({
      count: 1,
      extension: ['jpg', 'jpeg', 'png', 'webp'],
      sourceType: ['album', 'camera'],
      crop: {
        width: 1,
        height: 1,
        quality: 88,
        resize: true,
      },
    })
    const filePath = readSelectedFilePath(cropped)
    if (!filePath) throw new Error('未选择图片')
    return filePath
  } catch (error) {
    if (isCancelError(error)) throw error
    const fallback = await chooseImageAsync({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      extension: ['jpg', 'jpeg', 'png', 'webp'],
    })
    const filePath = readSelectedFilePath(fallback)
    if (!filePath) throw new Error('未选择图片')
    return filePath
  }
}

export const compressAvatarFile = async (filePath) => {
  if (!filePath) return ''
  try {
    const result = await compressImageAsync({
      src: filePath,
      quality: 82,
      compressedWidth: 720,
      compressedHeight: 720,
    })
    return result?.tempFilePath || filePath
  } catch (error) {
    return filePath
  }
}

export const chooseAndUploadAvatar = async (uploadFn = uploadPublicAvatarImage) => {
  const selectedFilePath = await chooseAvatarFile()
  const compressedFilePath = await compressAvatarFile(selectedFilePath)
  const response = await uploadFn(compressedFilePath)
  const avatarUrl = response?.data?.avatarUrl || response?.data
  if (!avatarUrl) throw new Error('头像上传失败')
  return avatarUrl
}
