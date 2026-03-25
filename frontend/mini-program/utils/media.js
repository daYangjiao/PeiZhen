import { canUseRemoteImageUrl, config } from '@/utils/api.js'
import { defaultAvatar, userPlaceholder } from '@/utils/assets.js'

const trimTrailingSlash = (value = '') => value.replace(/\/+$/, '')
const localPathPrefixes = [
  '/var/',
  '/private/',
  '/storage/',
  '/data/',
  '/sdcard/',
  '/Users/',
  '/Volumes/',
  '_doc/',
  '_downloads/',
  'content://'
]

const isLikelyLocalFilePath = (raw = '') =>
  localPathPrefixes.some((prefix) => raw.startsWith(prefix))

export const resolveImageUrl = (path, fallback = '') => {
  if (!path || typeof path !== 'string') return fallback

  const raw = path.trim()
  if (!raw) return fallback

  if (/^(data:|blob:|wxfile:|file:)/i.test(raw) || isLikelyLocalFilePath(raw)) return raw

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

export const resolveDisplayImageUrl = async (path, fallback = '') => {
  return resolveImageUrl(path, fallback)
}
