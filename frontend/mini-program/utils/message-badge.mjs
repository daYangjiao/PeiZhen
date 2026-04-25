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

const parseMessagePayload = (message = {}) => {
  if (message.data && typeof message.data === 'object') return message.data
  if (typeof message.data === 'string') {
    try {
      return JSON.parse(message.data)
    } catch (e) {
      return {}
    }
  }
  return {}
}

export const resolveRealtimeUnreadTarget = (message = {}, session = {}) => {
  const payload = parseMessagePayload(message)
  const type = String(message.type || payload.type || '')
  const msgType = Number(message.msgType || payload.msgType || 0)
  if (type === 'READ_RECEIPT' || msgType === 99) return null

  const currentUserId = Number(session.currentUserId || 0)
  const token = session.token || ''
  const isLoggedIn = session.isLoggedIn === true || session.isLoggedIn === 'true' || session.isLoggedIn === 1
  if (!currentUserId || !token || !isLoggedIn) return null

  const senderId = Number(message.senderId || payload.senderId || 0)
  const receiverId = Number(message.receiverId || payload.receiverId || 0)
  if (senderId && senderId === currentUserId) return null

  if (senderId === 0 || receiverId === 0) {
    return { type: 'system' }
  }

  const isNormalChatMsg = [1, 2, 3, 4].includes(msgType)
  if (!isNormalChatMsg) return null

  const contactId = senderId && senderId !== currentUserId
    ? senderId
    : (receiverId && receiverId !== currentUserId ? receiverId : 0)

  if (!contactId) return null
  return { type: 'contact', contactId }
}
