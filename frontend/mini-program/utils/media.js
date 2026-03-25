import { canUseRemoteImageUrl, config, isAppRuntime } from '@/utils/api.js'
import { defaultAvatar, userPlaceholder } from '@/utils/assets.js'

const trimTrailingSlash = (value = '') => value.replace(/\/+$/, '')
const imageDisplayCache = new Map()
const imageDisplayTasks = new Map()

export const resolveImageUrl = (path, fallback = '') => {
  if (!path || typeof path !== 'string') return fallback

  const raw = path.trim()
  if (!raw) return fallback

  if (/^(data:|blob:|wxfile:|file:)/i.test(raw)) return raw

  if (/^https?:\/\//i.test(raw)) {
    return canUseRemoteImageUrl(raw) ? raw : fallback
  }

  if (raw.startsWith('/static/')) return raw

  const base = trimTrailingSlash(config.assetBaseURL || config.baseURL)
  const normalized = raw.startsWith('/') ? raw : `/${raw}`
  const fullUrl = `${base}${normalized}`
  return canUseRemoteImageUrl(fullUrl) ? fullUrl : fallback
}

export const resolveAvatarUrl = (path, fallback = defaultAvatar || userPlaceholder) => {
  return resolveImageUrl(path, fallback)
}

const downloadRemoteImage = (url, fallback = '') => {
  return new Promise((resolve) => {
    uni.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
          resolve(res.tempFilePath)
          return
        }
        resolve(fallback)
      },
      fail: () => resolve(fallback)
    })
  })
}

export const resolveDisplayImageUrl = async (path, fallback = '') => {
  const resolvedUrl = resolveImageUrl(path, fallback)
  if (!resolvedUrl || resolvedUrl === fallback) return resolvedUrl

  if (!isAppRuntime() || !/^https?:\/\//i.test(resolvedUrl)) {
    return resolvedUrl
  }

  if (imageDisplayCache.has(resolvedUrl)) {
    return imageDisplayCache.get(resolvedUrl)
  }

  if (imageDisplayTasks.has(resolvedUrl)) {
    return imageDisplayTasks.get(resolvedUrl)
  }

  const task = downloadRemoteImage(resolvedUrl, fallback)
    .then((localPath) => {
      if (localPath && localPath !== fallback) {
        imageDisplayCache.set(resolvedUrl, localPath)
      }
      return localPath
    })
    .finally(() => {
      imageDisplayTasks.delete(resolvedUrl)
    })

  imageDisplayTasks.set(resolvedUrl, task)
  return task
}
