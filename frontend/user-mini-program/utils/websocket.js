import { config, getToken } from './api.js'

let socketTask = null
let isConnected = false
let reconnectTimer = null
const listeners = []

// 连接 WebSocket
export const connectSocket = () => {
  const token = getToken()
  const userInfo = uni.getStorageSync('userInfo')
  
  if (!userInfo || !userInfo.id) {
    console.log('WebSocket: 用户未登录，跳过连接')
    return
  }

  // 这里的 URL 需要根据您的后端地址进行调整
  // 注意：ws://localhost:8080/ws/orders?userId=...
  // 如果是真机调试，请将 localhost 换成局域网 IP
  const wsUrl = config.baseURL.replace('http', 'ws') + `/ws/orders?userId=${userInfo.id}`

  console.log('WebSocket: 开始连接', wsUrl)

  socketTask = uni.connectSocket({
    url: wsUrl,
    success: () => {
      console.log('WebSocket: 连接请求发送成功')
    },
    fail: (err) => {
      console.error('WebSocket: 连接请求发送失败', err)
    }
  })

  socketTask.onOpen(() => {
    console.log('WebSocket: 连接已打开')
    isConnected = true
    if (reconnectTimer) {
      clearInterval(reconnectTimer)
      reconnectTimer = null
    }
  })

  socketTask.onMessage((res) => {
    console.log('WebSocket: 收到消息', res.data)
    try {
      const message = JSON.parse(res.data)
      // 通知所有监听器
      listeners.forEach(listener => listener(message))
      
      // 全局处理：如果是订单状态更新，可以弹窗提示
      // 添加验证：确保消息来自有效的订单
      if (message.type === 'ORDER_ACCEPTED' && message.attendantName && message.orderId) {
        // 检查是否是当前用户的订单
        const currentOrders = uni.getStorageSync('userOrders') || []
        const orderExists = currentOrders.some(order => order.orderId === message.orderId)
        
        if (orderExists) {
          uni.showToast({
            title: `陪诊师 ${message.attendantName} 已接单`,
            icon: 'none',
            duration: 3000
          })
          
          // 更新本地订单状态
          const updatedOrders = currentOrders.map(order => {
            if (order.orderId === message.orderId) {
              return {
                ...order,
                orderStatus: 2, // 已接单
                attendantName: message.attendantName
              }
            }
            return order
          })
          uni.setStorageSync('userOrders', updatedOrders)
          
          // 触发全局事件通知订单页面更新
          uni.$emit('orderStatusChanged', {
            orderId: message.orderId,
            status: 2,
            attendantName: message.attendantName
          })
        } else {
          console.log('WebSocket: 收到其他用户的订单通知，忽略')
        }
      }
      
      // 处理系统消息
      if (message.type === 'SYSTEM_MESSAGE' || message.type === 'NOTIFICATION') {
        console.log('收到系统消息:', message)
        // 广播系统消息事件
        uni.$emit('system:message', message)
        uni.$emit('websocket:message', {
          type: 'system_message',
          data: message
        })
        
        // 如果是新消息，刷新未读状态
        if (message.action === 'NEW_MESSAGE') {
          uni.$emit('newSystemMessage')
        }
      }
    } catch (e) {
      console.error('WebSocket: 消息解析失败', e)
    }
  })

  socketTask.onClose(() => {
    console.log('WebSocket: 连接已关闭')
    isConnected = false
    // 尝试重连
    if (!reconnectTimer) {
      reconnectTimer = setInterval(() => {
        console.log('WebSocket: 尝试重连...')
        connectSocket()
      }, 5000)
    }
  })

  socketTask.onError((err) => {
    console.error('WebSocket: 连接错误', err)
    isConnected = false
  })
}

// 关闭连接
export const closeSocket = () => {
  if (socketTask) {
    socketTask.close()
    socketTask = null
  }
  if (reconnectTimer) {
    clearInterval(reconnectTimer)
    reconnectTimer = null
  }
}

// 添加消息监听器
export const addSocketListener = (callback) => {
  listeners.push(callback)
}

// 移除消息监听器
export const removeSocketListener = (callback) => {
  const index = listeners.indexOf(callback)
  if (index > -1) {
    listeners.splice(index, 1)
  }
}
