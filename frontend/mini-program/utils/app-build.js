const env = import.meta.env || {}

export const APP_BUILD = Object.freeze({
  buildLabel: env.VITE_APP_BUILD_LABEL || 'beta6',
  appid: '__UNI__73289DB',
  versionName: '1.0.1-beta6',
  versionCode: 106,
  packageName: 'cn.yuanban.peizhen',
  h5Version: env.VITE_APP_H5_VERSION || 'h5-local',
  gitCommit: env.VITE_APP_GIT_COMMIT || '',
  gitShortCommit: env.VITE_APP_GIT_SHORT_COMMIT || '',
  gitBranch: env.VITE_APP_GIT_BRANCH || '',
  gitDirty: env.VITE_APP_GIT_DIRTY === '1',
  buildTime: env.VITE_APP_BUILD_TIME || ''
})

export const APP_UPDATE_TYPE = Object.freeze({
  NONE: 0,
  WGT: 101,
  APK: 102
})

export const APP_WGT_VERSION_STORAGE_KEY = '__app_wgt_version__'
