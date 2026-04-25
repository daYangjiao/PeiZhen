<template>
  <view class="container">
    <scroll-view class="message-list" scroll-y>
      <view class="message-card system-card" @click="openSystemChat">
        <view class="avatar-container system-avatar-container">
          <image class="avatar" :src="systemNoticeAvatar" mode="aspectFill"></image>
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
          v-for="contact in contacts"
          :key="contact.id"
          class="message-card contact-card"
          @click="openChat(contact)"
        >
          <view class="avatar-container">
            <image
              class="avatar"
              :src="contact.displayAvatar"
              mode="aspectFill"
              @error="handleImageError(contact)"
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
              <text class="last-message">{{ formatLastMessage(contact) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="contacts.length === 0 && !lastSystemMsg.content">
        <image class="empty-icon" src="/static/xiaoxi_1.png" mode="aspectFit"></image>
        <text class="empty-text">暂无消息</text>
        <text class="empty-subtext">当用户咨询或系统通知到来时，消息会显示在这里</text>
      </view>
    </scroll-view>

    <exclusive-dispatch-popup />
    <escort-bottom-bar active="message" />
  </view>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import ExclusiveDispatchPopup from '@/components/exclusive-dispatch-popup.vue'
import { get } from '@/utils/api.js'
import { addChatListener, connectChatSocket, removeChatListener } from '@/utils/chat-websocket.js'
import { useMessageStore } from '@/stores/message.js'
import { ensureRole } from '@/utils/auth-guard.js'
import EscortBottomBar from '@/components/escort-bottom-bar.vue'
import { brandLogo, defaultAvatar } from '@/utils/assets.js'
import { resolveDisplayImageUrl } from '@/utils/media.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { guardEscortHallAccess } from '@/utils/escort-qualification-guard.js'

const contacts = ref([])
const lastSystemMsg = ref({})
const messageStore = useMessageStore()

const systemUnreadCount = computed(() => messageStore.systemUnreadCount)
const systemNoticeAvatar = brandLogo
const isReadReceiptMessage = (msg = {}) => String(msg.type || '') === 'READ_RECEIPT' || Number(msg.msgType || 0) === 99

const parseDateTimeSafe = (value) => {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const normalized = typeof value === 'string' ? value.replace(/-/g, '/') : value
  const parsed = new Date(normalized)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

let cleanupTimer = null
let isRefreshing = false
let refreshTimeout = null
let processedMessages = new Set()

const decorateContact = async (contact = {}) => ({
  ...contact,
  displayAvatar: await resolveDisplayImageUrl(contact.senderAvatar, defaultAvatar)
})

const updateContactUnreadMap = (contactList) => {
  const user = uni.getStorageSync('userInfo') || {}
  const currentUserId = Number(user.id || 0)
  if (!currentUserId) return

  contactList.forEach((contact) => {
    const contactId = Number(contact.senderId) === currentUserId
      ? Number(contact.receiverId)
      : Number(contact.senderId)
    if (!contactId) return
    messageStore.updateContactUnread(contactId, contact.unreadCount || 0)
  })
}

const loadContacts = async ({ force = false } = {}) => {
  if (isRefreshing && !force) return
  if (refreshTimeout) {
    clearTimeout(refreshTimeout)
    refreshTimeout = null
  }
  isRefreshing = true
  try {
    const res = await get('/api/chat/contacts')
    if (res.code === 200) {
      const allContacts = res.data || []
      const systemContact = allContacts.find((item) => Number(item.senderId) === 0 || Number(item.receiverId) === 0)
      const normalContacts = allContacts.filter((item) => Number(item.senderId) !== 0 && Number(item.receiverId) !== 0)

      lastSystemMsg.value = systemContact || {}
      messageStore.setSystemUnreadCount(systemContact?.unreadCount || 0)
      contacts.value = await Promise.all(normalContacts.map(decorateContact))
      updateContactUnreadMap(normalContacts)
    }
  } finally {
    refreshTimeout = setTimeout(() => {
      isRefreshing = false
      refreshTimeout = null
    }, force ? 0 : 800)
  }
}

const forceReloadContacts = () => {
  loadContacts({ force: true })
}

const handleNewMessage = (msg) => {
  if (!msg || isReadReceiptMessage(msg)) return
  const msgKey = `${msg.senderId}-${msg.receiverId}-${msg.createTime || msg.id}`
  if (processedMessages.has(msgKey)) return
  processedMessages.add(msgKey)

  if (processedMessages.size > 100) {
    const recent = Array.from(processedMessages).slice(-50)
    processedMessages = new Set(recent)
  }

  if (msg.senderId === 0 || (msg.senderId && msg.receiverId)) {
    loadContacts()
  }
}

const getContactUnreadCount = (contact) => {
  const user = uni.getStorageSync('userInfo') || {}
  const currentUserId = Number(user.id || 0)
  if (!currentUserId) return 0
  const contactId = Number(contact.senderId) === currentUserId
    ? Number(contact.receiverId)
    : Number(contact.senderId)
  return messageStore.getContactUnreadCount(contactId)
}

const openSystemChat = () => {
  uni.navigateTo({ url: '/subpkg/system-message/system-message' })
}

const openChat = (contact) => {
  const user = uni.getStorageSync('userInfo') || {}
  const currentUserId = Number(user.id || 0)
  if (!currentUserId) {
    uni.navigateTo({ url: '/pages/auth/login?role=escort' })
    return
  }

  const targetId = Number(contact.senderId) === currentUserId
    ? Number(contact.receiverId)
    : Number(contact.senderId)
  if (!targetId) return

  messageStore.updateContactUnread(targetId, 0)
  messageStore.updateTabBarBadge()
  uni.navigateTo({
    url: `/subpkg/chat/chat-escort?userId=${targetId}&name=${encodeURIComponent(contact.senderName || '用户')}&avatar=${encodeURIComponent(contact.senderAvatar || '')}`
  })
}

const handleImageError = (contact) => {
  if (!contact) return
  contact.displayAvatar = defaultAvatar
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = parseDateTimeSafe(timeStr)
  if (!date) return ''
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}/${date.getDate()}`
  }
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

const formatLastMessage = (contact = {}) => {
  if (contact.msgType === 2) return '[图片]'
  if (contact.msgType === 3) return '[语音]'
  if (contact.msgType === 4) return '[位置]'
  return contact.content || ''
}

onMounted(async () => {
  cleanupTimer = setInterval(() => {
    if (processedMessages.size > 100) {
      processedMessages = new Set(Array.from(processedMessages).slice(-50))
    }
  }, 30000)

  await messageStore.initMessageStatus()
  connectChatSocket()
  addChatListener(handleNewMessage)
  uni.$on('chat:return', loadContacts)
  uni.$on('system-message:read', forceReloadContacts)
})

onShow(() => {
  if (redirectPublicSafeToHome()) return
  if (!ensureRole('escort')) return
  guardEscortHallAccess({ showPopup: true, redirectOnConfirm: false })
  loadContacts()
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
  uni.$off('chat:return', loadContacts)
  uni.$off('system-message:read', forceReloadContacts)
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
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}

.system-card {
  background: linear-gradient(to right, #ffffff, #f0f7ff);
  border-left: 6rpx solid #007AFF;
}

.avatar-container {
  position: relative;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background-color: #f0f0f0;
  border: 2rpx solid #fff;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.08);
}

.system-avatar-container .avatar {
  padding: 8rpx;
  background-color: #e6f7ff;
}

.unread-badge {
  position: absolute;
  top: -4rpx;
  right: -4rpx;
  background-color: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  min-width: 28rpx;
  height: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
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
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300rpx;
}

.system-name {
  color: #007AFF;
}

.time {
  font-size: 20rpx;
  color: #999;
}

.message-body {
  display: flex;
  align-items: center;
}

.last-message {
  font-size: 24rpx;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
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
