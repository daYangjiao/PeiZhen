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

export const formatBuildTime = (value = APP_BUILD.buildTime) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (input) => String(input).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export const getH5VersionLabel = () => {
  const version = APP_BUILD.h5Version || APP_BUILD.buildLabel || APP_BUILD.versionName
  const time = formatBuildTime()
  return time ? `${version} · ${time}` : version
}

const isAppRuntime = () => {
  // #ifdef APP-PLUS
  return true
  // #endif
  // #ifndef APP-PLUS
  return false
  // #endif
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
  const appRuntime = isAppRuntime()
  const currentCodeVersion = appRuntime ? wgtVersion : APP_BUILD.versionName

  uni.showModal({
    title: '当前版本',
    showCancel: false,
    content: [
      `平台：${getPlatformLabel()}`,
      `H5版本：${getH5VersionLabel()}`,
      `代码版本：${APP_BUILD.buildLabel} / ${currentCodeVersion}`,
      `提交：${APP_BUILD.gitShortCommit || 'unknown'}${APP_BUILD.gitDirty ? ' (dirty)' : ''}`,
      `安装包版本：${appRuntime ? `${runtimeVersion} (${APP_BUILD.versionCode})` : 'H5 无安装包'}`,
      `资源包版本：${appRuntime ? wgtVersion : APP_BUILD.versionName}`,
      `包名：${APP_BUILD.packageName}`,
      `AppID：${runtimeAppid}`
    ].join('\n')
  })
}
