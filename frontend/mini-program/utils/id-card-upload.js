const ID_CARD_CROP_PAGE_PATH = '/subpkg/profile/id-card-crop'

let activeIdCardTask = null

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

export const chooseIdCardFile = async () => {
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

export const frameIdCardFile = (filePath, side = 'front') => {
  if (!filePath) return Promise.reject(new Error('未选择图片'))
  if (activeIdCardTask) return Promise.reject(new Error('已有身份证框选任务正在进行'))

  return new Promise((resolve, reject) => {
    activeIdCardTask = {
      filePath,
      side,
      resolve,
      reject,
    }

    uni.navigateTo({
      url: ID_CARD_CROP_PAGE_PATH,
      fail: (error) => {
        activeIdCardTask = null
        reject(error)
      },
    })
  })
}

export const consumeIdCardFrameTask = () => ({
  filePath: activeIdCardTask?.filePath || '',
  side: activeIdCardTask?.side || 'front',
})

export const resolveIdCardFrameTask = (framedFilePath) => {
  if (!activeIdCardTask) return
  const task = activeIdCardTask
  activeIdCardTask = null
  task.resolve(framedFilePath)
}

export const rejectIdCardFrameTask = (error) => {
  if (!activeIdCardTask) return
  const task = activeIdCardTask
  activeIdCardTask = null
  task.reject(error instanceof Error ? error : new Error(error?.message || error?.errMsg || 'cancel'))
}

export const compressIdCardFile = async (filePath) => {
  if (!filePath) return ''
  try {
    const result = await compressImageAsync({
      src: filePath,
      quality: 90,
      compressedWidth: 1280,
      compressedHeight: 808,
    })
    return result?.tempFilePath || filePath
  } catch (error) {
    return filePath
  }
}

export const pickFrameIdCardFile = async (side = 'front') => {
  const selectedFilePath = await chooseIdCardFile()
  const framedFilePath = await frameIdCardFile(selectedFilePath, side)
  return compressIdCardFile(framedFilePath || selectedFilePath)
}
