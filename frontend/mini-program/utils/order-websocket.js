import { config, getToken } from './api.js'

let socketTask = null
let reconnectTimer = null
let isConnecting = false
let isOpen = false
let activeUserId = null
const listeners = []

const clearReconnectTimer = () => {
  if (reconnectTimer) {
    clearInterval(reconnectTimer)
    reconnectTimer = null
  }
}

const resetSocketState = () => {
  isConnecting = false
  isOpen = false
  socketTask = null
}

export const connectOrderSocket = () => {
  const token = getToken()
  const userInfo = uni.getStorageSync('userInfo')

  if (!token || !userInfo || !userInfo.id) {
    activeUserId = null
    clearReconnectTimer()
    if (socketTask) {
      try { socketTask.close() } catch (e) {}
    }
    resetSocketState()
    return
  }

  const userId = Number(userInfo.id)
  if (!userId) return

  if (activeUserId === userId && (isConnecting || isOpen)) return

  if (activeUserId && activeUserId !== userId && socketTask) {
    try { socketTask.close() } catch (e) {}
    resetSocketState()
  }

  isConnecting = true
  activeUserId = userId
  const wsUrl = config.wsBaseURL + `/ws/orders?token=${encodeURIComponent(token)}`

  socketTask = uni.connectSocket({
    url: wsUrl,
    success: () => console.log('Order WebSocket 连接请求发送成功')
  })

  socketTask.onOpen(() => {
    isConnecting = false
    isOpen = true
    clearReconnectTimer()
    console.log('Order WebSocket 连接已打开')
  })

  socketTask.onMessage((res) => {
    try {
      const message = JSON.parse(res.data)
      listeners.forEach((callback) => callback(message))
      uni.$emit('order:message', message)
    } catch (e) {
      console.error('订单消息解析失败', e)
    }
  })

  socketTask.onClose(() => {
    console.log('Order WebSocket 连接已关闭')
    resetSocketState()
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectOrderSocket()
      }, 5000)
    }
  })

  socketTask.onError(() => {
    resetSocketState()
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectOrderSocket()
      }, 5000)
    }
  })
}

export const ensureOrderConnected = () => {
  connectOrderSocket()
}

export const isOrderSocketOpen = () => isOpen

export const addOrderListener = (callback) => {
  if (typeof callback !== 'function') return
  if (!listeners.includes(callback)) listeners.push(callback)
}

export const removeOrderListener = (callback) => {
  const index = listeners.indexOf(callback)
  if (index > -1) listeners.splice(index, 1)
}
