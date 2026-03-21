<template>
  <view class="container">
    <view class="header">
      <text class="title">消息中心</text>
    </view>
    
    <scroll-view class="message-list" scroll-y>
      <!-- 系统消息卡片 -->
      <view class="message-card system-card" @click="openSystemChat">
        <view class="avatar-container system-avatar-container">
          <image class="avatar" src="/static/brand-logo.png" mode="aspectFill"></image>
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

      <!-- 普通消息列表 -->
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
              <text class="name">{{ contact.senderName || '未知用户' }}</text>
              <text class="time">{{ formatTime(contact.createTime) }}</text>
            </view>
            <view class="message-body">
              <text class="last-message">{{ contact.msgType === 2 ? '[图片]' : contact.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view class="empty-state" v-if="contacts.length === 0 && !lastSystemMsg.content">
        <image class="empty-icon" src="/static/xiaoxi_1.png" mode="aspectFit"></image>
        <text class="empty-text">暂无消息</text>
        <text class="empty-subtext">当有新订单或用户咨询时，消息会显示在这里</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { get, post, config } from '@/utils/api.js'
import { useMessageStore } from '@/stores/message.js'
import { connectChatSocket, addChatListener, removeChatListener } from '@/utils/chat-websocket.js'

const contacts = ref([])
const lastSystemMsg = ref({})
const messageStore = useMessageStore()

// 计算属性：系统未读消息数
const systemUnreadCount = computed(() => messageStore.systemUnreadCount)

onMounted(async () => {
  console.log('消息页面 mounted')
  
  // 先初始化store状态
  await messageStore.initMessageStatus()
  console.log('初始化后store状态:', {
    systemUnreadCount: messageStore.systemUnreadCount,
    unreadTotal: messageStore.unreadTotal,
    totalUnreadCount: messageStore.totalUnreadCount,
    contactUnreadMap: messageStore.contactUnreadMap
  })
  
  await loadContacts()
  
  // 连接WebSocket并监听消息
  connectChatSocket()
  addChatListener(handleNewMessage)
  
  // 监听聊天页面返回事件
  uni.$on('chat:return', handleChatReturn)
  
  // 检查最终状态
  console.log('加载完成后最终状态:', {
    systemUnreadCount: messageStore.systemUnreadCount,
    unreadTotal: messageStore.unreadTotal,
    totalUnreadCount: messageStore.totalUnreadCount,
    contactUnreadMap: messageStore.contactUnreadMap
  })
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
  uni.$off('chat:return', handleChatReturn)
})

// 已处理的消息ID集合，防止重复处理
const processedMessages = new Set()

// 处理新的WebSocket消息（store 更新由 App 全局监听处理，本页仅刷新列表）
const handleNewMessage = (msg) => {
  if (!msg) return
  const msgKey = msg.id || `${msg.senderId}-${msg.receiverId}-${msg.createTime}`
  if (processedMessages.has(msgKey)) return
  processedMessages.add(msgKey)
  if (processedMessages.size > 100) {
    const arr = Array.from(processedMessages).slice(-50)
    processedMessages.clear()
    arr.forEach(k => processedMessages.add(k))
  }
  if ((msg.senderId === 0 || (msg.senderId && msg.receiverId)) && !(msg.msgType == 3 || msg.content === 'READ_RECEIPT')) {
    loadContacts()
  }
}

const loadContacts = async () => {
  try {
    console.log('开始加载联系人...')
    const res = await get('/api/chat/contacts')
    console.log('API响应:', res)
    
    if (res.code === 200) {
      const allContacts = res.data
      console.log('获取到的联系人列表:', allContacts)
      
      // 分离系统消息和普通联系人
      const sysMsg = allContacts.find(c => c.senderId === 0 || c.receiverId === 0)
      const normalContacts = allContacts.filter(c => c.senderId !== 0 && c.receiverId !== 0)
      
      console.log('系统消息:', sysMsg)
      console.log('普通联系人:', normalContacts)

      if (sysMsg) {
        lastSystemMsg.value = sysMsg
        // 使用store更新系统未读数
        const systemUnread = sysMsg.unreadCount || 0
        messageStore.systemUnreadCount = systemUnread
        console.log('设置系统未读数:', systemUnread, '原始数据:', sysMsg)
      } else {
        lastSystemMsg.value = {}
        messageStore.systemUnreadCount = 0
        console.log('未找到系统消息')
      }

      contacts.value = normalContacts
      
      // 更新联系人未读消息映射
      updateContactUnreadMap(normalContacts)
      
      // 更新tabBar
      messageStore.updateTabBarBadge()
      console.log('更新tabBar，当前总未读数:', messageStore.totalUnreadCount)
    }
  } catch (e) {
    console.error('加载联系人失败', e)
    uni.showToast({
      title: '加载消息失败',
      icon: 'none'
    })
  }
}

// 更新联系人未读消息映射
const updateContactUnreadMap = (contactList) => {
  console.log('更新联系人未读映射，联系人列表:', contactList)
  contactList.forEach(contact => {
    // 正确计算联系人ID
    const currentUserId = uni.getStorageSync('userInfo').id
    const contactId = contact.senderId === currentUserId ? contact.receiverId : contact.senderId
    
    console.log(`联系人映射 - 当前用户:${currentUserId}, 发送者:${contact.senderId}, 接收者:${contact.receiverId}, 计算出的联系人ID:${contactId}`)
    
    messageStore.updateContactUnread(contactId, contact.unreadCount || 0)
  })
}

// 获取联系人的未读消息数
const getContactUnreadCount = (contact) => {
  const currentUserId = uni.getStorageSync('userInfo').id
  const contactId = contact.senderId === currentUserId ? contact.receiverId : contact.senderId
  const unreadCount = messageStore.getContactUnreadCount(contactId)
  
  console.log('计算联系人未读数:', {
    contactId: contactId,
    senderId: contact.senderId,
    receiverId: contact.receiverId,
    currentUserId: currentUserId,
    storeUnread: unreadCount,
    contactInfo: contact
  })
  
  return unreadCount
}

const openSystemChat = async () => {
  try {
    await post('/api/chat/read?senderId=0')
  } catch (e) {
    console.error('标记系统消息已读失败', e)
  }
  messageStore.resetSystemUnread()
  messageStore.updateTabBarBadge()
  uni.navigateTo({
    url: `/subpkg/system-message/system-message`
  })
}

const openChat = async (contact) => {
  const currentUserId = uni.getStorageSync('userInfo').id
  let targetId
  
  // 计算正确的联系人ID
  if (contact.senderId === currentUserId) {
    targetId = contact.receiverId
  } else {
    targetId = contact.senderId
  }

  if (!targetId) return

  const targetName = contact.senderName === '我' ? '用户' : (contact.senderName || '用户')

  // 关键修复：调用后端API标记消息为已读，触发放到WebSocket通知
  try {
    await post(`/api/chat/read?senderId=${targetId}`);
    console.log('已调用后端标记消息为已读，senderId:', targetId);
  } catch (error) {
    console.error('标记消息为已读失败:', error);
  }

  // 立即重置未读状态（在跳转前）
  messageStore.resetContactUnread(targetId)
  
  // 立即更新UI显示
  updateContactUnreadMap(contacts.value)
  messageStore.updateTabBarBadge()

  uni.navigateTo({
    url: `/subpkg/chat/chat?userId=${targetId}&name=${targetName}`
  })
}

const getAvatarUrl = (url) => {
  if (!url) return '/static/user-placeholder.png'
  
  if (url.startsWith('http') || url.startsWith('https')) {
    return url
  }
  
  let baseUrl = config.baseURL
  if (baseUrl.endsWith('/')) {
    baseUrl = baseUrl.slice(0, -1)
  }
  
  let path = url
  if (!path.startsWith('/')) {
    path = '/' + path
  }
  
  return baseUrl + path
}

const handleImageError = (e) => {
  console.error('头像加载失败:', e.detail.errMsg)
}

// 处理聊天页面返回事件
const handleChatReturn = (data) => {
  console.log('收到聊天页面返回事件:', data)
  
  // 重置对应联系人的未读状态
  if (data && data.targetUserId) {
    messageStore.resetContactUnread(data.targetUserId)
    
    // 立即更新UI
    updateContactUnreadMap(contacts.value)
    messageStore.updateTabBarBadge()
    
    // 重新加载联系人列表确保数据同步
    loadContacts()
  }
}

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
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
}

.header {
  padding: 24rpx 32rpx;
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.02);
}

.title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.message-list {
  height: calc(100vh - 90rpx);
  padding: 16rpx 24rpx;
  box-sizing: border-box;
}

.message-card {
  display: flex;
  padding: 24rpx;
  background-color: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03);
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.98);
    background-color: #fafafa;
  }
}

.system-card {
  background: linear-gradient(to right, #ffffff, #f0f7ff);
  border-left: 6rpx solid #4A90E2;
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
  box-shadow: 0 2rpx 6rpx rgba(0,0,0,0.08);
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
  margin-bottom: 8rpx;
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
  color: #4A90E2;
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
  padding-top: 160rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 24rpx;
  opacity: 0.6;
}

.empty-text {
  color: #333;
  font-size: 28rpx;
  font-weight: 500;
  margin-bottom: 8rpx;
}

.empty-subtext {
  color: #999;
  font-size: 22rpx;
}
</style>