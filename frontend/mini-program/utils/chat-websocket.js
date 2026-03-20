import { config, getToken } from './api.js'

let socketTask = null
let reconnectTimer = null
let isConnecting = false
let isOpen = false
let activeUserId = null
const listeners = []

export const connectChatSocket = () => {
  const token = getToken()
  const userInfo = uni.getStorageSync('userInfo')

  if (!token || !userInfo || !userInfo.id) {
    activeUserId = null
    isConnecting = false
    isOpen = false
    if (socketTask) {
      try { socketTask.close() } catch (e) {}
      socketTask = null
    }
    if (reconnectTimer) {
      clearInterval(reconnectTimer)
      reconnectTimer = null
    }
    return
  }
  const userId = Number(userInfo.id)
  if (!userId) return

  // 同一用户已有连接/连接中时，直接复用，避免重复建链导致重复回调
  if (activeUserId === userId && (isConnecting || isOpen)) return

  // 切换账号时重置旧连接状态
  if (activeUserId && activeUserId !== userId && socketTask) {
    try { socketTask.close() } catch (e) {}
    socketTask = null
    isConnecting = false
    isOpen = false
  }

  isConnecting = true
  activeUserId = userId
  const wsUrl = config.wsBaseURL + `/ws/chat?token=${encodeURIComponent(token)}`

  socketTask = uni.connectSocket({
    url: wsUrl,
    success: () => console.log('Chat WebSocket 连接请求发送成功')
  })

  socketTask.onOpen(() => {
    console.log('Chat WebSocket 连接已打开')
    isConnecting = false
    isOpen = true
    if (reconnectTimer) {
      clearInterval(reconnectTimer)
      reconnectTimer = null
    }
    console.log('WebSocket连接成功，保持现有未读状态', !!token)
  })

  socketTask.onMessage((res) => {
    try {
      const message = JSON.parse(res.data)
      listeners.forEach((listener) => listener(message))
      uni.$emit('chat:message', message)
      uni.$emit('websocket:message', { type: 'new_message', data: message })
    } catch (e) {
      console.error('消息解析失败', e)
    }
  })

  socketTask.onClose(() => {
    console.log('Chat WebSocket 连接已关闭')
    isConnecting = false
    isOpen = false
    socketTask = null
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectChatSocket()
      }, 5000)
    }
  })

  socketTask.onError(() => {
    isConnecting = false
    isOpen = false
    socketTask = null
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectChatSocket()
      }, 5000)
    }
  })
}

export const addChatListener = (callback) => {
  if (typeof callback !== 'function') return
  if (!listeners.includes(callback)) listeners.push(callback)
}

export const removeChatListener = (callback) => {
  const index = listeners.indexOf(callback)
  if (index > -1) listeners.splice(index, 1)
}
