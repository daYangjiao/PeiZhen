import { config, getToken } from './api.js'

let socketTask = null
let isConnected = false
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
    isConnected = true
    if (reconnectTimer) {
      clearInterval(reconnectTimer)
      reconnectTimer = null
    }
    
    // 连接成功时不自动清除未读状态
    console.log('WebSocket连接成功，保持现有未读状态')
  })

  socketTask.onMessage((res) => {
    try {
      const message = JSON.parse(res.data)
      console.log('Chat WebSocket收到消息:', message)
      
      // 通知所有监听器
      listeners.forEach(listener => listener(message))
      
      // 广播到全局事件总线
      uni.$emit('chat:message', message)
      uni.$emit('websocket:message', {
        type: 'new_message',
        data: message
      })
      
    } catch (e) {
      console.error('消息解析失败', e)
    }
  })

  socketTask.onClose(() => {
    console.log('Chat WebSocket 连接已关闭')
    isConnected = false
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        connectChatSocket()
      }, 5000)
    }
  })
}

export const sendChatMessage = (message) => {
    // 发送消息通常走 HTTP 接口以确保持久化，WebSocket 主要用于接收
    // 但如果需要也可以通过 WebSocket 发送
}

export const addChatListener = (callback) => {
  listeners.push(callback)
}

export const removeChatListener = (callback) => {
  const index = listeners.indexOf(callback)
  if (index > -1) {
    listeners.splice(index, 1)
  }
}
