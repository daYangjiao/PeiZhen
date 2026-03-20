import { config, getToken } from './api.js'

let socketTask = null
const listeners = []

export const connectOrderSocket = () => {
  const token = getToken()
  const userInfo = uni.getStorageSync('userInfo')
  if (!token || !userInfo?.id) return
  const wsUrl = config.wsBaseURL + `/ws/orders?token=${encodeURIComponent(token)}`
  socketTask = uni.connectSocket({ url: wsUrl })
  socketTask.onMessage((res) => {
    try {
      const message = JSON.parse(res.data)
      listeners.forEach(cb => cb(message))
    } catch (e) {}
  })
}

export const addSocketListener = (callback) => {
  listeners.push(callback)
}

export const removeSocketListener = (callback) => {
  const i = listeners.indexOf(callback)
  if (i > -1) listeners.splice(i, 1)
}
