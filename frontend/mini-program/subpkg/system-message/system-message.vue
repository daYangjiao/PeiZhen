<template>
  <view class="container" :class="roleClass">
    <view class="tabs-container">
      <scroll-view scroll-x class="tabs-scroll" :show-scrollbar="false">
        <view class="tabs-wrapper">
          <view
            v-for="(tab, index) in tabs"
            :key="tab"
            class="tab-item"
            :class="{ active: currentTab === index }"
            @click="switchTab(index)"
          >
            <text class="tab-text">{{ tab }}</text>
            <view v-if="currentTab === index" class="tab-line"></view>
          </view>
        </view>
      </scroll-view>
    </view>

    <scroll-view class="message-list-unified" scroll-y>
      <view
        v-for="msg in filteredMessages"
        :key="msg.id"
        class="system-message-card"
        :class="{ clickable: isEscortRole }"
        @click="openMessage(msg)"
      >
        <view class="message-header">
          <view class="header-left">
            <text class="message-title">{{ getMessageTitle(msg) }}</text>
            <view v-if="isEscortRole && !msg.isRead" class="unread-dot"></view>
          </view>
          <text class="message-time">{{ formatTime(msg.createTime) }}</text>
        </view>

        <view class="message-content">
          <text class="content-text">{{ msg.content }}</text>
        </view>

        <view
          v-if="showFooterAction(msg)"
          class="message-footer"
          @click.stop="handleAction(msg)"
        >
          <text class="action-text">{{ getActionText(msg) }}</text>
          <text class="action-arrow">></text>
        </view>
      </view>

      <view v-if="filteredMessages.length === 0" class="empty-state">
        <image class="empty-icon" src="/static/xiaoxi_1.png" mode="aspectFit"></image>
        <text class="empty-text">暂无{{ tabs[currentTab] }}消息</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { get, post } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'
import { useMessageStore } from '@/stores/message.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'

const systemMessages = ref([])
const currentTab = ref(0)
const tabs = ['全部', '订单状态', '服务提醒', '平台公告', '账户相关']
const role = ref(uni.getStorageSync('role') || 'user')
const roleClass = computed(() => (role.value === 'escort' ? 'role-escort' : 'role-user'))
const isEscortRole = computed(() => role.value === 'escort')
const messageStore = useMessageStore()
const actionLoadingKey = ref('')

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
  if (content.includes('就诊安排') || content.includes('就诊提醒')) return '就诊提醒'
  if (content.includes('服务已完成') || content.includes('评价')) return '服务完成提醒'
  if (content.includes('余额不足')) return '账户提醒'
  if (content.includes('维护') || content.includes('升级')) return '平台公告'
  return '系统通知'
}

const inferAction = (content) => {
  if (!content) return ''
  if (content.includes('评价')) return '去评价'
  if (content.includes('订单')) return '查看订单'
  if (content.includes('余额不足')) return '去充值'
  return ''
}

const extractOrderNo = (content) => {
  if (!content) return null
  const patterns = [
    /订单(?:No\.?|号)?\s*[：: ]?\s*([A-Za-z0-9_-]{8,})/i,
    /订单\s*([A-Za-z0-9_-]{8,})/i,
    /\b(ORD[A-Za-z0-9_-]{6,})\b/i
  ]
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match?.[1]) {
      return match[1].replace(/[，。,;；]+$/g, '')
    }
  }
  return null
}

const showOrderAccessError = (error, fallbackMessage = '订单暂时无法打开') => {
  const code = Number(error?.code || 0)
  if (code === 401) {
    uni.showToast({ title: error?.message || '无权限查看该订单', icon: 'none' })
    return
  }
  uni.showToast({ title: error?.message || fallbackMessage, icon: 'none' })
}

const resolveMessageOrderTarget = async (msg) => {
  const directOrderId = Number(msg?.orderId || 0)
  const orderNo = extractOrderNo(msg?.content)

  if (directOrderId > 0) {
    return { orderId: directOrderId, orderNo }
  }

  if (!orderNo || orderNo.includes('*')) {
    return { orderId: null, orderNo: null }
  }

  try {
    const res = await get(`/ai/guide/orders/${encodeURIComponent(orderNo)}/complete-info`)
    if (res?.data?.orderId) {
      return { orderId: Number(res.data.orderId), orderNo: res.data.orderNo || orderNo }
    }
  } catch {}

  return { orderId: null, orderNo }
}

const openOrderFromMessage = async (msg) => {
  const target = await resolveMessageOrderTarget(msg)

  if (isEscortRole.value) {
    if (!target.orderId) {
      uni.showToast({ title: '未找到可跳转的订单', icon: 'none' })
      return
    }
    await get(`/attendant/orders/${target.orderId}`)
    uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${target.orderId}` })
    return
  }

  if (target.orderId) {
    await get(`/api/orders/${target.orderId}`)
    uni.navigateTo({ url: `/subpkg/order/order-detail?orderId=${target.orderId}` })
    return
  }

  if (target.orderNo) {
    uni.navigateTo({ url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(target.orderNo)}` })
    return
  }

  uni.showToast({ title: '未找到可跳转的订单', icon: 'none' })
}

const buildActionLoadingKey = (msg) => `${msg?.id || ''}_${msg?.createTime || ''}_${msg?.orderId || ''}`

const loadSystemMessages = async () => {
  const res = await get('/api/chat/system')
  const apiData = (res.data || []).map((msg) => ({
    ...msg,
    type: inferType(msg.content),
    title: inferTitle(msg.content),
    action: inferAction(msg.content)
  }))
  systemMessages.value = apiData
  if (isEscortRole.value) {
    messageStore.systemUnreadCount = apiData.filter((item) => !item.isRead).length
    messageStore.updateTabBarBadge()
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
  if (!isEscortRole.value && systemMessages.value.length > 0) {
    await markSystemMessagesRead()
    return
  }
  if (!isEscortRole.value) {
    messageStore.resetSystemUnread()
    messageStore.updateTabBarBadge()
  }
}

const filteredMessages = computed(() => {
  if (currentTab.value === 0) return systemMessages.value
  return systemMessages.value.filter((msg) => msg.type === tabs[currentTab.value])
})

const getMessageTitle = (msg) => msg.title || '系统通知'
const getActionText = (msg) => {
  if (isEscortRole.value) return '查看订单'
  return msg.action || ''
}
const showFooterAction = (msg) => {
  if (isEscortRole.value) return !!msg.orderId
  return !!msg.action
}

const switchTab = (index) => {
  currentTab.value = index
}

const openMessage = (msg) => {
  if (!isEscortRole.value) return
  uni.navigateTo({ url: `/subpkg/system-message/escort-detail?messageId=${msg.id}` })
}

const handleAction = async (msg) => {
  const orderTab = isEscortRole.value ? '/pages/role-escort/order' : '/pages/role-user/order'
  const loadingKey = buildActionLoadingKey(msg)
  if (actionLoadingKey.value === loadingKey) return

  if (isEscortRole.value || msg.action === '查看订单') {
    actionLoadingKey.value = loadingKey
    uni.showLoading({ title: '跳转中...', mask: true })
    try {
      await openOrderFromMessage(msg)
    } catch (error) {
      showOrderAccessError(error)
      if (Number(error?.code || 0) !== 401) {
        uni.switchTab({ url: orderTab })
      }
    } finally {
      uni.hideLoading()
      actionLoadingKey.value = ''
    }
    return
  }

  if (msg.action === '去评价') {
    const orderNo = extractOrderNo(msg.content)
    if (orderNo && !orderNo.includes('*')) {
      uni.navigateTo({ url: `/subpkg/evaluate/evaluate?orderNo=${orderNo}` })
    } else {
      uni.switchTab({ url: orderTab })
    }
    return
  }

  if (msg.action === '去充值') {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}

const handleNewMessage = () => {
  syncSystemMessages()
}

const handleSystemMessageRead = ({ messageId } = {}) => {
  if (!messageId) {
    syncSystemMessages()
    return
  }

  systemMessages.value = systemMessages.value.map((item) =>
    Number(item.id) === Number(messageId)
      ? { ...item, isRead: true }
      : item
  )
  messageStore.systemUnreadCount = systemMessages.value.filter((item) => !item.isRead).length
  messageStore.updateTabBarBadge()
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

onLoad(() => {
  if (redirectPublicSafeToHome()) return
  role.value = uni.getStorageSync('role') || 'user'
  syncSystemMessages()
  addChatListener(handleNewMessage)
  uni.$on('system-message:read', handleSystemMessageRead)
})

onShow(() => {
  if (redirectPublicSafeToHome()) return
  role.value = uni.getStorageSync('role') || 'user'
  syncSystemMessages()
})

onUnmounted(() => {
  removeChatListener(handleNewMessage)
  uni.$off('system-message:read', handleSystemMessageRead)
})
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
  font-size: 22rpx;
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

.message-list-unified {
  padding: 24rpx;
  box-sizing: border-box;
  min-height: calc(100vh - 180rpx);
}

.system-message-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
}

.system-message-card.clickable {
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.system-message-card.clickable:active {
  transform: scale(0.99);
  box-shadow: 0 8rpx 18rpx rgba(0, 0, 0, 0.06);
}

.message-header {
  margin-bottom: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.message-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.unread-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #ff4d4f;
  box-shadow: 0 0 0 6rpx rgba(255, 77, 79, 0.12);
}

.message-time {
  font-size: 24rpx;
  color: #999;
  flex-shrink: 0;
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
