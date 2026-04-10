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

    <scroll-view class="message-list" scroll-y>
      <view
        v-for="msg in filteredMessages"
        :key="getMessageId(msg)"
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

const inferType = (content = '') => {
  if (!content) return '平台公告'
  if (content.includes('订单') || content.includes('支付')) return '订单状态'
  if (content.includes('就诊') || content.includes('服务') || content.includes('评价')) return '服务提醒'
  if (content.includes('公告') || content.includes('维护') || content.includes('升级')) return '平台公告'
  if (content.includes('余额') || content.includes('充值') || content.includes('退款')) return '账户相关'
  return '平台公告'
}

const inferTitle = (content = '') => {
  if (!content) return '系统通知'
  if (content.includes('支付完成') || content.includes('支付成功')) return '订单支付成功'
  if (content.includes('就诊安排') || content.includes('就诊提醒')) return '就诊提醒'
  if (content.includes('服务已完成') || content.includes('评价')) return '服务完成提醒'
  if (content.includes('余额不足')) return '账户提醒'
  if (content.includes('维护') || content.includes('升级')) return '平台公告'
  return '系统通知'
}

const inferAction = (content = '') => {
  if (!content) return ''
  if (content.includes('评价')) return '去评价'
  if (content.includes('订单')) return '查看订单'
  if (content.includes('余额不足')) return '去充值'
  return ''
}

const extractOrderNo = (content = '') => {
  if (!content) return null
  const patterns = [
    /订单(?:No\.?|号)?[:：\s]*([A-Za-z0-9_-]{8,})/i,
    /\b(ORD[A-Za-z0-9_-]{6,})\b/i
  ]
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match?.[1]) {
      return match[1].replace(/[，。；;,]+$/g, '')
    }
  }
  return null
}

const getMessageId = (msg) => Number(msg?.id || msg?.messageId || 0)
const getMessageOrderId = (msg) => Number(msg?.orderId || msg?.order?.orderId || 0)

const showOrderAccessError = (error, fallbackMessage = '订单暂时无法打开') => {
  const code = Number(error?.code || 0)
  if (code === 401) {
    uni.showToast({ title: error?.message || '无权限查看该订单', icon: 'none' })
    return
  }
  uni.showToast({ title: error?.message || fallbackMessage, icon: 'none' })
}

const resolveMessageOrderTarget = async (msg) => {
  const directOrderId = getMessageOrderId(msg)
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

const resolveEscortMessageOrderTarget = async (msg) => {
  const directOrderId = getMessageOrderId(msg)
  if (directOrderId > 0) {
    return { orderId: directOrderId, orderNo: extractOrderNo(msg?.content) }
  }

  const messageId = getMessageId(msg)
  if (messageId > 0) {
    try {
      const res = await get(`/api/chat/system/${messageId}`)
      const detail = res?.data || {}
      const orderId = Number(detail?.order?.orderId || detail?.orderId || 0)
      if (orderId > 0) {
        return {
          orderId,
          orderNo: detail?.order?.orderNo || extractOrderNo(msg?.content)
        }
      }
    } catch {}
  }

  return resolveMessageOrderTarget(msg)
}

const openOrderFromMessage = async (msg) => {
  if (isEscortRole.value) {
    const target = await resolveEscortMessageOrderTarget(msg)
    if (!target.orderId) {
      uni.showToast({ title: '未找到可跳转的订单', icon: 'none' })
      return
    }
    await get(`/attendant/orders/${target.orderId}`)
    uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${target.orderId}` })
    return
  }

  const target = await resolveMessageOrderTarget(msg)
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

const buildActionLoadingKey = (msg) =>
  `${getMessageId(msg)}_${msg?.createTime || ''}_${getMessageOrderId(msg)}`

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
  if (isEscortRole.value) return !!(getMessageOrderId(msg) || extractOrderNo(msg?.content))
  return !!msg.action
}

const switchTab = (index) => {
  currentTab.value = index
}

const openMessage = async (msg) => {
  if (!isEscortRole.value) return
  const messageId = getMessageId(msg)
  if (messageId > 0) {
    uni.navigateTo({ url: `/subpkg/system-message/escort-detail?messageId=${messageId}` })
    return
  }
  try {
    await openOrderFromMessage(msg)
  } catch (error) {
    showOrderAccessError(error)
  }
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
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.tabs-container {
  background: #fff;
  padding-top: 8rpx;
}

.tabs-scroll {
  white-space: nowrap;
}

.tabs-wrapper {
  display: inline-flex;
  align-items: center;
  min-width: 100%;
  padding: 0 20rpx;
  box-sizing: border-box;
}

.tab-item {
  position: relative;
  padding: 24rpx 20rpx 22rpx;
  margin-right: 20rpx;
  color: #6b7280;
  font-size: 30rpx;
  flex-shrink: 0;
}

.tab-item.active {
  color: #1677ff;
  font-weight: 600;
}

.tab-line {
  position: absolute;
  left: 20rpx;
  right: 20rpx;
  bottom: 8rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: #1677ff;
}

.message-list {
  flex: 1;
  padding: 20rpx 24rpx 32rpx;
  box-sizing: border-box;
}

.system-message-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 22rpx;
  box-shadow: 0 10rpx 26rpx rgba(15, 23, 42, 0.05);
}

.system-message-card.clickable:active {
  transform: scale(0.995);
}

.message-header,
.message-footer,
.header-left {
  display: flex;
  align-items: center;
}

.message-header,
.message-footer {
  justify-content: space-between;
}

.header-left {
  gap: 10rpx;
}

.message-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
}

.unread-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #ff4d4f;
}

.message-time {
  font-size: 26rpx;
  color: #9ca3af;
}

.message-content {
  padding: 24rpx 0 20rpx;
}

.content-text {
  font-size: 31rpx;
  line-height: 1.8;
  color: #4b5563;
}

.message-footer {
  padding-top: 20rpx;
  border-top: 1rpx solid #eef2f7;
}

.action-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #1677ff;
}

.action-arrow {
  font-size: 32rpx;
  color: #c4c9d4;
}

.empty-state {
  padding: 160rpx 40rpx;
  text-align: center;
  color: #94a3b8;
}

.empty-icon {
  width: 180rpx;
  height: 180rpx;
  margin-bottom: 24rpx;
}

.empty-text {
  font-size: 28rpx;
}
</style>
