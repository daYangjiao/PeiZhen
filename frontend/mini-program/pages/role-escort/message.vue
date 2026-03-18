<template>
  <view class="container">
    <scroll-view class="message-list" scroll-y>
      <view class="message-card system-card" @click="openSystemChat">
        <view class="avatar-container system-avatar-container">
          <image class="avatar" :src="getBackendImageUrl('mynewlogo.png')" mode="aspectFill"></image>
          <view class="unread-badge" v-if="systemUnreadCount > 0">
            <text>{{ systemUnreadCount > 99 ? '99+' : systemUnreadCount }}</text>
          </view>
        </view>
        <view class="message-content">
          <view class="message-header">
            <text class="name system-name">系统通知</text>
            <text class="time">{{ formatTime(lastSystemMsg.createTime) }}</text>
          </view>
          <view class="message-body">
            <text class="last-message">{{ lastSystemMsg.content || '暂无新通知' }}</text>
          </view>
        </view>
      </view>

      <view class="contact-list">
        <view
          class="message-card contact-card"
          v-for="contact in contacts"
          :key="contact.id"
          @click="openChat(contact)"
        >
          <view class="avatar-container">
            <image
              class="avatar"
              :src="getAvatarUrl(contact.senderAvatar)"
              mode="aspectFill"
              @error="handleImageError"
            ></image>
            <view class="unread-badge" v-if="getContactUnreadCount(contact) > 0">
              <text>{{ getContactUnreadCount(contact) > 99 ? '99+' : getContactUnreadCount(contact) }}</text>
            </view>
          </view>

          <view class="message-content">
            <view class="message-header">
              <text class="name">{{ contact.senderName || '用户' }}</text>
              <text class="time">{{ formatTime(contact.createTime) }}</text>
            </view>
            <view class="message-body">
              <text class="last-message">{{ contact.msgType === 2 ? '[图片]' : contact.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="contacts.length === 0 && !lastSystemMsg.content">
        <image class="empty-icon" src="/static/xiaoxi_1.png" mode="aspectFit"></image>
        <text class="empty-text">暂无消息</text>
        <text class="empty-subtext">当有新订单或用户咨询时，消息会显示在这里</text>
      </view>
    </scroll-view>

    <EscortBottomBar active="message" />
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, post, config, getBackendImageUrl } from '@/utils/api.js'
import { connectChatSocket, addChatListener, removeChatListener } from '@/utils/chat-websocket.js'
import { useMessageStore } from '@/stores/message.js'
import { ensureRole } from '@/utils/auth-guard.js'
import EscortBottomBar from '@/components/EscortBottomBar.vue'

const contacts = ref([])
const lastSystemMsg = ref({})
const messageStore = useMessageStore()

const systemUnreadCount = computed(() => messageStore.systemUnreadCount)

let cleanupTimer = null
let isRefreshing = false
let refreshTimeout = null
let processedMessages = new Set()

const loadContacts = async () => {
  if (isRefreshing) return
  isRefreshing = true
  try {
    const res = await get('/api/chat/contacts')
    if (res.code === 200) {
      const allContacts = res.data || []
      const sysMsg = allContacts.find(c => c.senderId === 0 || c.receiverId === 0)
      const normalContacts = allContacts.filter(c => c.senderId !== 0 && c.receiverId !== 0)
      if (sysMsg) {
        lastSystemMsg.value = sysMsg
        messageStore.systemUnreadCount = sysMsg.unreadCount || 0
      } else {
        lastSystemMsg.value = {}
        messageStore.systemUnreadCount = 0
      }
      contacts.value = normalContacts
      updateContactUnreadMap(normalContacts)
      messageStore.updateTabBarBadge()
    }
  } finally {
    clearTimeout(refreshTimeout)
    refreshTimeout = setTimeout(() => {
      isRefreshing = false
    }, 1000)
  }
}

const handleNewMessage = (msg) => {
  if (!msg) return
  const msgKey = `${msg.senderId}-${msg.receiverId}-${msg.createTime || msg.id}`
  if (processedMessages.has(msgKey)) return
  processedMessages.add(msgKey)
  if (processedMessages.size > 100) {
    const arr = Array.from(processedMessages).slice(-50)
    processedMessages.clear()
    arr.forEach(k => processedMessages.add(k))
  }
  const isReadReceipt = msg.msgType === 3 || msg.content === 'READ_RECEIPT' || msg.type === 'READ_RECEIPT'
  if ((msg.senderId === 0 || (msg.senderId && msg.receiverId)) && !isReadReceipt) {
    loadContacts()
  }
}

const updateContactUnreadMap = (contactList) => {
  const user = uni.getStorageSync('userInfo') || {}
  const currentUserId = user.id
  if (!currentUserId) return
  contactList.forEach(contact => {
    const contactId = contact.senderId === currentUserId ? contact.receiverId : contact.senderId
    messageStore.updateContactUnread(contactId, contact.unreadCount || 0)
  })
}

const getContactUnreadCount = (contact) => {
  const user = uni.getStorageSync('userInfo') || {}
  const currentUserId = user.id
  if (!currentUserId) return 0
  const contactId = contact.senderId === currentUserId ? contact.receiverId : contact.senderId
  return messageStore.getContactUnreadCount(contactId)
}

const openSystemChat = () => {
  uni.navigateTo({ url: '/subpkg/system-message/system-message' })
  ;(async () => {
    try {
      await post('/api/chat/read?senderId=0')
    } catch {}
    messageStore.resetSystemUnread()
    messageStore.updateTabBarBadge()
  })()
}

const openChat = (contact) => {
  const user = uni.getStorageSync('userInfo') || {}
  const currentUserId = user.id
  if (!currentUserId) {
    uni.navigateTo({ url: '/pages/auth/login?role=escort' })
    return
  }
  let targetId
  if (contact.senderId === currentUserId) targetId = contact.receiverId
  else targetId = contact.senderId
  if (!targetId) return
  const targetName = contact.senderName === '我' ? '用户' : (contact.senderName || '用户')
  uni.navigateTo({
    url: `/subpkg/chat/chat?userId=${targetId}&name=${targetName}`
  })
  ;(async () => {
    try {
      await post(`/api/chat/read?senderId=${targetId}`)
    } catch {}
    messageStore.resetContactUnread(targetId)
    messageStore.updateTabBarBadge()
  })()
}

const getAvatarUrl = (url) => {
  if (!url) return '/static/default-avatar.jpg'
  if (url.startsWith('http') || url.startsWith('https')) return url
  let baseUrl = config.baseURL
  if (baseUrl.endsWith('/')) baseUrl = baseUrl.slice(0, -1)
  let path = url
  if (!path.startsWith('/')) path = '/' + path
  return baseUrl + path
}

const handleImageError = () => {}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}/${date.getDate()}`
  }
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

onMounted(async () => {
  cleanupTimer = setInterval(() => {
    const cacheSize = processedMessages.size
    if (cacheSize > 100) {
      const recentMessages = Array.from(processedMessages).slice(-50)
      processedMessages = new Set(recentMessages)
    }
  }, 30000)
  await messageStore.initMessageStatus()
  connectChatSocket()
  addChatListener(handleNewMessage)
  uni.$on('chat:return', loadContacts)
})

onShow(() => {
  if (!ensureRole('escort')) return
  loadContacts()
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
  uni.$off('chat:return', loadContacts)
  if (cleanupTimer) {
    clearInterval(cleanupTimer)
    cleanupTimer = null
  }
  processedMessages.clear()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.container {
  @include escort-page;
  min-height: 100vh;
  padding-bottom: 140rpx;
}

.message-list {
  height: calc(100vh - 140rpx);
  padding: 18rpx 20rpx;
  box-sizing: border-box;
}

.message-card {
  display: flex;
  padding: 24rpx;
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  margin-bottom: 14rpx;
  border: 1rpx solid #e7edf5;
  box-shadow: $escort-shadow-card;
}

.system-card {
  background: linear-gradient(120deg, #f3f8ff 0%, #e9f2ff 100%);
  border: 1rpx solid #d5e4f9;
}

.avatar-container {
  position: relative;
  margin-right: 18rpx;
  flex-shrink: 0;
}

.avatar {
  width: 78rpx;
  height: 78rpx;
  border-radius: 50%;
  background-color: #eef2f7;
  border: 2rpx solid #fff;
  box-shadow: 0 6rpx 12rpx rgba(31, 41, 55, 0.1);
}

.system-avatar-container .avatar {
  padding: 8rpx;
  background-color: #dcecff;
}

.unread-badge {
  position: absolute;
  top: -4rpx;
  right: -4rpx;
  background-color: #ff5b5d;
  color: #fff;
  font-size: 18rpx;
  min-width: 30rpx;
  height: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 15rpx;
  border: 2rpx solid #fff;
  padding: 0 4rpx;
  box-sizing: border-box;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.name {
  font-size: 27rpx;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300rpx;
}

.system-name {
  color: #2f78d4;
}

.time {
  font-size: 21rpx;
  color: #8c96a8;
}

.last-message {
  font-size: 24rpx;
  color: #5f6b7b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 140rpx;
  background: #edf2f8;
  border-radius: 20rpx;
  margin-top: 20rpx;
  min-height: 460rpx;
}

.empty-icon {
  width: 180rpx;
  height: 180rpx;
  margin-bottom: 18rpx;
  opacity: 0.6;
}

.empty-text {
  color: #1f2937;
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.empty-subtext {
  color: #7d8898;
  font-size: 22rpx;
}
</style>
