import { config, getToken } from './api.js'

let socketTask = null
let reconnectTimer = null
const listeners = []

export const connectChatSocket = () => {
  const token = getToken()
  const userInfo = uni.getStorageSync('userInfo')

  if (!userInfo || !userInfo.id) return

  const wsUrl = config.baseURL.replace('http', 'ws') + `/ws/chat?userId=${userInfo.id}`

  socketTask = uni.connectSocket({
    url: wsUrl,
    success: () => console.log('Chat WebSocket 连接请求发送成功')
  })

  socketTask.onOpen(() => {
    console.log('Chat WebSocket 连接已打开')
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
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectChatSocket()
      }, 5000)
    }
  })
}

export const addChatListener = (callback) => {
  listeners.push(callback)
}

export const removeChatListener = (callback) => {
  const index = listeners.indexOf(callback)
  if (index > -1) listeners.splice(index, 1)
}

