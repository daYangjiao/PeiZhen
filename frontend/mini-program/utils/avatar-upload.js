import { uploadPublicAvatarImage } from '@/api/user.js'

const AVATAR_CROP_PAGE_PATH = '/subpkg/profile/avatar-crop'

let activeCropTask = null

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

const readSelectedFilePath = (result = {}) =>
  result.tempFilePaths?.[0] ||
  result.tempFiles?.[0]?.path ||
  result.tempFiles?.[0]?.tempFilePath ||
  ''

export const chooseAvatarFile = async () => {
  const result = await chooseImageAsync({
    count: 1,
    sizeType: ['original', 'compressed'],
    sourceType: ['album', 'camera'],
    extension: ['jpg', 'jpeg', 'png', 'webp'],
  })
  const filePath = readSelectedFilePath(result)
  if (!filePath) throw new Error('未选择图片')
  return filePath
}

export const cropAvatarFile = (filePath) => {
  if (!filePath) return Promise.reject(new Error('未选择图片'))
  if (activeCropTask) return Promise.reject(new Error('已有头像裁剪任务正在进行'))

  return new Promise((resolve, reject) => {
    activeCropTask = {
      filePath,
      resolve,
      reject,
    }

    uni.navigateTo({
      url: AVATAR_CROP_PAGE_PATH,
      fail: (error) => {
        activeCropTask = null
        reject(error)
      }
    })
  })
}

export const consumeAvatarCropTask = () => activeCropTask?.filePath || ''

export const resolveAvatarCropTask = (croppedFilePath) => {
  if (!activeCropTask) return
  const task = activeCropTask
  activeCropTask = null
  task.resolve(croppedFilePath)
}

export const rejectAvatarCropTask = (error) => {
  if (!activeCropTask) return
  const task = activeCropTask
  activeCropTask = null
  task.reject(error instanceof Error ? error : new Error(error?.message || error?.errMsg || 'cancel'))
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

export const pickCropAndUploadAvatar = async (uploadFn = uploadPublicAvatarImage) => {
  const selectedFilePath = await chooseAvatarFile()
  const croppedFilePath = await cropAvatarFile(selectedFilePath)
  uni.showLoading({ title: '处理中...' })
  try {
    const compressedFilePath = await compressAvatarFile(croppedFilePath)
    const response = await uploadFn(compressedFilePath)
    const avatarUrl = response?.data?.avatarUrl || response?.data
    if (!avatarUrl) throw new Error('头像上传失败')
    return {
      avatarUrl,
      localFilePath: compressedFilePath || croppedFilePath || selectedFilePath
    }
  } finally {
    uni.hideLoading()
  }
}
