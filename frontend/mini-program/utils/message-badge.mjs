export const MESSAGE_BADGE_STORAGE_KEY = 'tabbar_message_badge'
export const MESSAGE_BADGE_UPDATED_EVENT = 'message:badge-updated'
export const SYSTEM_MESSAGE_READ_EVENT = 'system-message:read'
export const USER_MESSAGE_TAB_INDEX = 3

export const normalizeBadgeCount = (count) => {
  const value = Number(count || 0)
  if (!Number.isFinite(value) || value <= 0) return 0
  return Math.floor(value)
}

export const formatBadgeText = (count) => {
  const value = normalizeBadgeCount(count)
  if (value <= 0) return ''
  return value > 99 ? '99+' : String(value)
}
