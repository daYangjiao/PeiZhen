<template>
  <view class="container" :class="roleClass">
    <view class="tabs-container">
      <scroll-view scroll-x class="tabs-scroll" :show-scrollbar="false">
        <view class="tabs-wrapper">
          <view
            v-for="(tab, index) in tabs"
            :key="index"
            class="tab-item"
            :class="{ active: currentTab === index }"
            @click="switchTab(index)"
          >
            <text class="tab-text">{{ tab }}</text>
            <view class="tab-line" v-if="currentTab === index"></view>
          </view>
        </view>
      </scroll-view>
    </view>

    <scroll-view class="message-list" scroll-y :scroll-top="scrollTop" :scroll-into-view="scrollIntoView">
      <view class="system-message-card" v-for="(msg, index) in filteredMessages" :key="index" :id="'msg-' + index">
        <view class="message-header">
          <view class="header-info">
            <text class="message-title">【{{ getMessageTitle(msg) }}】</text>
            <text class="message-time">{{ formatTime(msg.createTime) }}</text>
          </view>
        </view>
        <view class="message-content">
          <text class="content-text">{{ msg.content }}</text>
        </view>
        <view class="message-footer" v-if="getActionText(msg)" @click="handleAction(msg)">
          <text class="action-text">{{ getActionText(msg) }}</text>
          <text class="action-arrow">></text>
        </view>
      </view>

      <view class="empty-state" v-if="filteredMessages.length === 0">
        <image class="empty-icon" src="/static/xiaoxi_1.png" mode="aspectFit"></image>
        <text class="empty-text">暂无{{ tabs[currentTab] }}消息</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { get, post } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'
import { useMessageStore } from '@/stores/message.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'

const systemMessages = ref([])
const scrollTop = ref(0)
const scrollIntoView = ref('')
const currentTab = ref(0)
const tabs = ['全部', '订单状态', '服务提醒', '平台公告', '账户相关']
const role = ref(uni.getStorageSync('role') || 'user')
const roleClass = computed(() => (role.value === 'escort' ? 'role-escort' : 'role-user'))
const messageStore = useMessageStore()

onLoad(() => {
  if (redirectPublicSafeToHome()) return
  syncSystemMessages()
  addChatListener(handleNewMessage)
})

onShow(() => {
  if (redirectPublicSafeToHome()) return
  syncSystemMessages()
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
})

const handleNewMessage = () => {
  syncSystemMessages()
}

const loadSystemMessages = async () => {
  try {
    const res = await get('/api/chat/system')
    if (res.code === 200 && res.data) {
      const apiData = res.data.map(msg => ({
        ...msg,
        type: inferType(msg.content),
        title: inferTitle(msg.content),
        action: inferAction(msg.content)
      }))
      systemMessages.value = apiData
    } else {
      systemMessages.value = []
    }
  } catch (error) {
    console.error('加载系统通知失败', error)
    systemMessages.value = []
  }
}

const markSystemMessagesRead = async () => {
  try {
    await post('/api/chat/read?senderId=0')
  } catch {}
  messageStore.resetSystemUnread()
  messageStore.updateTabBarBadge()
}

const syncSystemMessages = async () => {
  await loadSystemMessages()
  if (systemMessages.value.length > 0) {
    await markSystemMessagesRead()
    return
  }
  messageStore.resetSystemUnread()
  messageStore.updateTabBarBadge()
}

const inferType = (content) => {
  if (!content) return '平台公告'
  if (content.includes('订单') || content.includes('支付')) return '订单状态'
  if (content.includes('就诊') || content.includes('服务') || content.includes('评价')) return '服务提醒'
  if (content.includes('公告') || content.includes('维护') || content.includes('升级')) return '平台公告'
  if (content.includes('余额') || content.includes('充值') || content.includes('退款')) return '账户相关'
  return '平台公告'
}

const inferTitle = (content) => {
  if (!content) return '系统通知'
  if (content.includes('支付完成') || content.includes('支付成功')) return '订单支付成功'
  if (content.includes('就诊安排') || content.includes('就诊提醒')) return '就诊温馨提醒'
  if (content.includes('服务已完成') || content.includes('评价')) return '服务完成评价'
  if (content.includes('余额不足')) return '账户余额提醒'
  if (content.includes('维护') || content.includes('升级')) return '平台公告'
  return '系统通知'
}

const inferAction = (content) => {
  if (!content) return ''
  if (content.includes('评价')) return '去评价'
  if (content.includes('订单')) return '查看订单'
  if (content.includes('服务已完成')) return '去评价'
  if (content.includes('余额不足')) return '去充值'
  return ''
}

const extractOrderNo = (content) => {
  if (!content) return null
  const patterns = [
    /订单(?:No\.?|号)?\s*[：:（(]?\s*([A-Za-z0-9_-]{8,})/i,
    /订单\s*([A-Za-z0-9_-]{8,})/i,
    /\b(ORD[A-Za-z0-9_-]{6,})\b/i
  ]
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match && match[1]) {
      return match[1].replace(/[）)。，,;；。]+$/g, '')
    }
  }
  return null
}

const filteredMessages = computed(() => {
  if (currentTab.value === 0) return systemMessages.value
  const type = tabs[currentTab.value]
  return systemMessages.value.filter(msg => msg.type === type)
})

const switchTab = (index) => {
  currentTab.value = index
  scrollTop.value = 0
}

const getMessageTitle = (msg) => msg.title || '系统通知'
const getActionText = (msg) => msg.action

const handleAction = (msg) => {
  const orderTab = role.value === 'escort' ? '/pages/role-escort/order' : '/pages/role-user/order'
  if (msg.action === '查看订单') {
    if (role.value === 'escort') {
      uni.switchTab({ url: orderTab })
      return
    }
    const orderNo = extractOrderNo(msg.content)
    if (orderNo && !orderNo.includes('*')) {
      uni.navigateTo({ url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(orderNo)}` })
    } else {
      uni.switchTab({ url: orderTab })
    }
  } else if (msg.action === '去评价') {
    if (role.value === 'escort') {
      uni.switchTab({ url: orderTab })
    } else {
      const orderNo = extractOrderNo(msg.content)
      if (orderNo && !orderNo.includes('*')) {
        uni.navigateTo({ url: `/subpkg/evaluate/Evaluate?orderNo=${orderNo}` })
      } else {
        uni.switchTab({ url: orderTab })
      }
    }
  } else if (msg.action === '去充值') {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return `今天 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return `昨天 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
@import '@/styles/escort-ui.scss';
.container {
  --msg-primary: #{$user-color-primary};
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}
.container.role-escort {
  --msg-primary: #{$escort-color-primary};
}
.tabs-container {
  background-color: #fff;
  padding: 10rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.tabs-scroll {
  white-space: nowrap;
  width: 100%;
}
.tabs-wrapper {
  display: flex;
  padding: 0 20rpx;
}
.tab-item {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 30rpx;
  position: relative;
}
.tab-item.active .tab-text {
  color: var(--msg-primary);
  font-weight: 600;
  font-size:22rpx;
}
.tab-text {
  font-size: 22rpx;
  color: #666;
  transition: all 0.3s;
}
.tab-line {
  width: 40rpx;
  height: 6rpx;
  background-color: var(--msg-primary);
  border-radius: 3rpx;
  position: absolute;
  bottom: 6rpx;
}
.message-list {
  flex: 1;
  padding: 24rpx;
  box-sizing: border-box;
  height: 0;
}
.system-message-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
}
.message-header {
  margin-bottom: 20rpx;
}
.header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.message-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}
.message-time {
  font-size: 24rpx;
  color: #999;
}
.message-content {
  margin-bottom: 20rpx;
}
.content-text {
  font-size: 28rpx;
  line-height: 1.6;
  color: #666;
  text-align: justify;
}
.message-footer {
  border-top: 1rpx solid #f5f7fa;
  padding-top: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.action-text {
  font-size: 28rpx;
  color: var(--msg-primary);
  font-weight: 500;
}
.action-arrow {
  font-size: 28rpx;
  color: #ccc;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
}
.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 20rpx;
  opacity: 0.5;
}
.empty-text {
  color: #999;
  font-size: 28rpx;
}
</style>
