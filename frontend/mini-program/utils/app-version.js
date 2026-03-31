import { APP_BUILD, APP_WGT_VERSION_STORAGE_KEY } from '@/utils/app-build'

const getPlatformLabel = () => {
  try {
    const sys = uni.getSystemInfoSync()
    const platform = (sys.platform || '').toLowerCase()
    if (platform === 'android') return 'Android'
    if (platform === 'ios') return 'iOS'
    if (platform === 'devtools') return 'DevTools'
    return platform || 'Unknown'
  } catch (error) {
    return 'Unknown'
  }
}

const getStoredWgtVersion = () => {
  const value = uni.getStorageSync(APP_WGT_VERSION_STORAGE_KEY)
  return typeof value === 'string' && value.trim() ? value.trim() : APP_BUILD.versionName
}

const getRuntimeInfo = () => {
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

export const showCurrentVersionInfo = async () => {
  const runtimeInfo = await getRuntimeInfo()
  const runtimeVersion = runtimeInfo.version || APP_BUILD.versionName
  const runtimeAppid = runtimeInfo.appid || APP_BUILD.appid
  const wgtVersion = getStoredWgtVersion()

  uni.showModal({
    title: '当前版本',
    showCancel: false,
    content: [
      `平台：${getPlatformLabel()}`,
      `安装包版本：${runtimeVersion} (${APP_BUILD.versionCode})`,
      `资源包版本：${wgtVersion}`,
      `包名：${APP_BUILD.packageName}`,
      `AppID：${runtimeAppid}`
    ].join('\n')
  })
}
