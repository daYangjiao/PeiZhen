import { connectChatSocket } from './chat-websocket.js'
import { connectOrderSocket } from './order-websocket.js'

let initialized = false

export const initWebSockets = () => {
  if (initialized) return
  initialized = true
  connectChatSocket()
  connectOrderSocket()
}

export const ensureChatConnected = () => {
  connectChatSocket()
  connectOrderSocket()
}
