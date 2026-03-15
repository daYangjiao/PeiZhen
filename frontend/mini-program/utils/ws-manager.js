import { connectChatSocket } from './chat-websocket.js'

let initialized = false

export const initWebSockets = () => {
  if (initialized) return
  initialized = true
  connectChatSocket()
}

export const ensureChatConnected = () => {
  connectChatSocket()
}

