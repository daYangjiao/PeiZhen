import { post } from '@/utils/api'
import { APP_BUILD, APP_UPDATE_TYPE, APP_WGT_VERSION_STORAGE_KEY } from '@/utils/app-build'

let checkingPromise = null

const getPlatform = () => {
  try {
    const sys = uni.getSystemInfoSync()
    return (sys.platform || '').toLowerCase()
  } catch (error) {
    return ''
  }
}

const getStoredWgtVersion = () => {
  const value = uni.getStorageSync(APP_WGT_VERSION_STORAGE_KEY)
  return typeof value === 'string' && value.trim() ? value.trim() : APP_BUILD.versionName
}

const setStoredWgtVersion = (version) => {
  if (!version) return
  uni.setStorageSync(APP_WGT_VERSION_STORAGE_KEY, version)
}

const getRuntimeProperty = () => {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.runtime.getProperty(plus.runtime.appid, (info) => {
      resolve(info || {})
    })
    // #endif
    // #ifndef APP-PLUS
    resolve({})
    // #endif
  })
}

const buildRequestPayload = async () => {
  const runtimeInfo = await getRuntimeProperty()
  return {
    appid: APP_BUILD.appid,
    platform: getPlatform(),
    appVersion: runtimeInfo.version || APP_BUILD.versionName,
    appVersionCode: APP_BUILD.versionCode,
    wgtVersion: getStoredWgtVersion()
  }
}

const downloadFile = (url) => {
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
          resolve(res.tempFilePath)
          return
        }
        reject(new Error(`下载失败: ${res.statusCode || 'unknown'}`))
      },
      fail: reject
    })
  })
}

const installPackage = (filePath, options = {}) => {
  return new Promise((resolve, reject) => {
    // #ifdef APP-PLUS
    plus.runtime.install(
      filePath,
      options,
      () => resolve(),
      (error) => reject(error)
    )
    // #endif
    // #ifndef APP-PLUS
    reject(new Error('当前环境不支持安装更新包'))
    // #endif
  })
}

const showConfirm = ({ title, content, forceUpdate = false }) => {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      showCancel: !forceUpdate,
      confirmText: '立即更新',
      cancelText: '稍后再说',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}

const installWgt = async (payload) => {
  uni.showLoading({ title: '更新资源中...', mask: true })
  try {
    const filePath = await downloadFile(payload.downloadUrl)
    await installPackage(filePath, { force: true })
    setStoredWgtVersion(payload.wgtVersion || payload.latestVersion || APP_BUILD.versionName)
    uni.hideLoading()
    uni.showToast({ title: '资源已更新，正在重启', icon: 'none', duration: 1200 })
    setTimeout(() => {
      // #ifdef APP-PLUS
      plus.runtime.restart()
      // #endif
    }, 600)
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '资源更新失败', icon: 'none' })
  }
}

const installApk = async (payload) => {
  const confirmed = await showConfirm({
    title: payload.title || '检测到新版本',
    content: payload.notes || '发现新的安装包版本，是否立即下载并安装？',
    forceUpdate: !!payload.forceUpdate
  })
  if (!confirmed) return

  uni.showLoading({ title: '下载新版本中...', mask: true })
  try {
    const filePath = await downloadFile(payload.downloadUrl)
    uni.hideLoading()
    await installPackage(filePath, { force: true })
  } catch (error) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '安装包更新失败', icon: 'none' })
  }
}

const applyUpgrade = async (payload) => {
  if (!payload || !payload.downloadUrl) return
  if (payload.updateType === APP_UPDATE_TYPE.WGT) {
    await installWgt(payload)
    return
  }
  if (payload.updateType === APP_UPDATE_TYPE.APK) {
    await installApk(payload)
  }
}

export const checkAppUpgrade = async () => {
  // #ifndef APP-PLUS
  return null
  // #endif
  // #ifdef APP-PLUS
  if (checkingPromise) return checkingPromise
  checkingPromise = (async () => {
    if (getPlatform() !== 'android') return null
    try {
      const payload = await buildRequestPayload()
      const res = await post('/api/app-upgrade/check', payload)
      if (res?.code !== 200 || !res.data) return null
      if (!res.data.updateType || res.data.updateType === APP_UPDATE_TYPE.NONE) return res.data
      await applyUpgrade(res.data)
      return res.data
    } catch (error) {
      console.warn('checkAppUpgrade failed', error)
      return null
    } finally {
      checkingPromise = null
    }
  })()
  return checkingPromise
  // #endif
}
