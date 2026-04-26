<template>
  <view class="order-page" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="content-wrapper">
      <view class="status-tabs">
        <scroll-view scroll-x class="tabs-scroll" :show-scrollbar="false">
          <view class="tabs-container">
            <view
              v-for="(tab, index) in statusTabs"
              :key="index"
              class="tab-item"
              :class="{ active: activeStatus === tab.value }"
              @click="switchTab(tab.value)"
            >
              <view class="tab-label">
                <text class="tab-text">{{ tab.name }}</text>
                <view v-if="shouldShowTabBadge(tab.value)" class="tab-red-dot"></view>
              </view>
              <view class="active-line" v-if="activeStatus === tab.value"></view>
            </view>
          </view>
        </scroll-view>
      </view>

      <view v-if="initialLoading" class="loading-state">
        <text class="loading-text">加载中...</text>
      </view>

      <scroll-view class="order-list" scroll-y>
        <view v-if="!initialLoading && filteredOrders.length > 0" class="order-container">
          <view
            v-for="order in filteredOrders"
            :key="order.orderNo"
            class="order-card"
            @click="handleOrderClick(order)"
          >
            <view class="card-content">
              <view class="order-header">
                <text class="order-number">订单号: {{ order.orderNo }}</text>
                <view class="status-badge" :class="getStatusClass(order.orderStatus)">
                  <text class="status-text">{{ getStatusText(order) }}</text>
                </view>
              </view>

              <view class="service-row">
                <text class="service-type">{{ order.serviceTypeName || order.serviceContent }}</text>
                <text class="hospital-name">{{ order.hospital }}</text>
              </view>

              <view class="order-meta">
                <view class="time-column">
                  <view class="time-row">
                    <image src="/static/time.png" class="meta-icon"></image>
                    <text class="service-time">{{ order.displayServiceTime }}</text>
                  </view>
                  <text v-if="isAssignedWaiting(order)" class="assigned-countdown">
                    剩余 {{ getAssignedCountdown(order) }}
                  </text>
                </view>
                <text class="price">¥{{ order.orderAmount.toFixed(2) }}</text>
              </view>

              <view class="divider"></view>

              <view class="doctor-info">
                <view class="attendant-wrapper clickable" v-if="order.attendantName" @click.stop="openAttendantDetail(order)">
                  <image class="doctor-avatar" :src="order.displayAttendantAvatar" mode="aspectFill"></image>
                  <text class="doctor-name">{{ order.attendantName }}</text>
                  <text class="attendant-arrow">›</text>
                </view>
                <view class="attendant-wrapper" v-else>
                  <image class="doctor-avatar" src="/static/default-avatar.jpg" mode="aspectFill"></image>
                  <text v-if="false" class="doctor-name text-gray">
                    {{
                      order.orderStatus === 7
                        ? '已取消'
                        : (order.paymentStatus === 0 ? '待支付' : '待分配')
                    }}
                  </text>
                  <text v-else class="doctor-name text-gray">{{ getDoctorNamePlaceholder(order) }}</text>
                </view>

                <view class="action-buttons">
                  <button class="btn detail-btn" @click.stop="handleDetailClick(order)">查看详情</button>
                  <button class="btn pay-btn" v-if="order.paymentStatus === 0 && order.orderStatus !== 7" @click.stop="handlePay(order)">去支付</button>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view v-else-if="!initialLoading" class="empty-state">
          <image class="empty-icon" src="/static/order.png" mode="aspectFit"></image>
          <text class="empty-text">暂无相关订单</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { get } from '@/utils/api.js'
import { useUserStore } from '@/stores/user'
import { ensureRole } from '@/utils/auth-guard.js'
import { addOrderListener, removeOrderListener, connectOrderSocket, isOrderSocketOpen } from '@/utils/order-websocket.js'
import { defaultAvatar } from '@/utils/assets.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { navigateToAttendantDetail } from '@/utils/attendant-detail.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'
import { isTimeoutClosedOrder, TIMEOUT_CLOSE_STATUS_TEXT } from '@/utils/order-timeout.js'
import { shouldShowOrderTabBadge } from '@/utils/order-tab-badges.mjs'

const statusBarHeight = ref(0)
const clockNow = ref(Date.now())

const statusTabs = ref([
  { name: '全部', value: null },
  { name: '待支付', value: 0 },
  { name: '待接单', value: 1 },
  { name: '专属待确认', value: 8 },
  { name: '待服务', value: 2 },
  { name: '服务中', value: 3 },
  { name: '待确认时长', value: 4 },
  { name: '争议中', value: 5 },
  { name: '待补差额', value: 9 },
  { name: '已完成', value: 6 },
  { name: '已取消', value: 7 }
])

const activeStatus = ref(null)
const orders = ref([])
const initialLoading = ref(false)
const isRefreshing = ref(false)
const userStore = useUserStore()
let pollTimer = null
let socketListener = null
let socketRefreshTimer = null
let clockTimer = null
let pageActive = false
let queuedReload = false
let queuedReloadSilent = true
const ORDER_LIST_POLL_INTERVAL = 30000
const ORDER_EVENT_TYPES = [
  'NEW_ORDER',
  'ORDER_ACCEPTED',
  'SERVICE_STARTED',
  'SERVICE_COMPLETED',
  'ORDER_STATUS_CHANGED',
  'ORDER_RELEASED_BY_ATTENDANT',
  'SERVICE_PROGRESS_UPDATED',
  'SERVICE_PROGRESS_CHANGED',
  'ORDER_UPDATED',
  'ORDER_CANCELLED',
  'ORDER_FINISHED',
  'TIME_FEE_CONFIRMED',
  'TIME_FEE_DISPUTED',
  'BALANCE_PAYMENT_REQUIRED'
]

onMounted(() => {
  const systemInfo = uni.getSystemInfoSync()
  statusBarHeight.value = systemInfo.statusBarHeight || 0
})

onShow(() => {
  if (redirectPublicSafeToHome()) return
  pageActive = true
  // 如果是被守卫/401 拦截后从订单页自动跳转到登录，再从登录返回且仍未登录，则直接回到首页，避免死循环
  if (!userStore.isLoggedIn) {
    const fromRoute = uni.getStorageSync('guard_from_route')
    if (fromRoute === 'pages/role-user/order' || fromRoute === '/pages/role-user/order') {
      uni.removeStorageSync('guard_from_route')
      pageActive = false
      uni.switchTab({ url: '/pages/role-user/home' })
      return
    }
  }
  if (!ensureRole('user')) {
    pageActive = false
    return
  }
  userStore.restoreFromStorage()
  connectOrderSocket()
  setupWebSocketListener()
  startClockTicker()
  loadOrders({ silent: orders.value.length > 0 })
  startPolling()
})

onHide(() => {
  pageActive = false
  stopPolling()
  stopClockTicker()
  if (socketRefreshTimer) {
    clearTimeout(socketRefreshTimer)
    socketRefreshTimer = null
  }
  teardownWebSocketListener()
})

onUnload(() => {
  cleanupRealtime()
})

const switchTab = (status) => {
  activeStatus.value = status
  loadOrders({ silent: orders.value.length > 0 })
}

const shouldShowTabBadge = (tabValue) => shouldShowOrderTabBadge(tabValue, orders.value)

const isOrderListLoading = () => initialLoading.value || isRefreshing.value

const startClockTicker = () => {
  stopClockTicker()
  clockNow.value = Date.now()
  clockTimer = setInterval(() => {
    clockNow.value = Date.now()
  }, 1000)
}

const stopClockTicker = () => {
  if (clockTimer) {
    clearInterval(clockTimer)
    clockTimer = null
  }
}

const parseOrderTime = (value) => {
  if (!value) return null
  const normalized = String(value).replace(/-/g, '/')
  const time = Date.parse(normalized)
  return Number.isNaN(time) ? null : time
}

const formatCountdown = (remainingMs) => {
  const safe = Math.max(0, Number(remainingMs) || 0)
  const totalSeconds = Math.floor(safe / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

const isAssignedWaiting = (order) => Number(order?.paymentStatus) === 1 && Number(order?.orderStatus) === 8

const getAssignedCountdown = (order) => {
  if (!isAssignedWaiting(order)) return '00:00'
  const baseTime = parseOrderTime(order?.paymentTime)
  if (baseTime == null) return '00:00'
  const remainingMs = 15 * 60 * 1000 - (clockNow.value - baseTime)
  return formatCountdown(remainingMs)
}

const loadOrders = async ({ silent = false } = {}) => {
  if (!userStore.isLoggedIn) {
    orders.value = []
    return
  }
  if (isOrderListLoading()) {
    queuedReload = true
    queuedReloadSilent = queuedReloadSilent && silent
    return
  }
  const useSilentRefresh = silent && orders.value.length > 0
  if (useSilentRefresh) {
    isRefreshing.value = true
  } else {
    initialLoading.value = true
  }
  try {
    const response = await get('/api/orders/user-orders', {
      page: 0,
      pageSize: 100
    })
    if (response.code === 200 && response.data && response.data.content) {
      orders.value = (response.data.content || []).map((order) => ({
        ...order,
        displayAttendantAvatar: resolveAvatarUrl(order.attendantAvatar, defaultAvatar),
        displayServiceTime: [order.serviceDate, formatServiceTimeSlot(order.serviceTimeSlot)].filter(Boolean).join(' ').trim()
      }))
    } else {
      orders.value = []
    }
  } catch (e) {
    orders.value = []
    console.error('加载订单数据出错:', e)
  } finally {
    initialLoading.value = false
    isRefreshing.value = false
    if (queuedReload && pageActive) {
      const nextSilent = queuedReloadSilent
      queuedReload = false
      queuedReloadSilent = true
      loadOrders({ silent: nextSilent })
    } else {
      queuedReload = false
      queuedReloadSilent = true
    }
  }
}

const filteredOrders = computed(() => {
  if (activeStatus.value === null) return orders.value
  return orders.value.filter(order => {
    if (activeStatus.value === 0) {
      return order.paymentStatus === 0 && order.orderStatus !== 7
    }
    return order.orderStatus === activeStatus.value
  })
})

const getStatusText = (orderOrStatus) => {
  if (typeof orderOrStatus === 'object' && orderOrStatus !== null) {
    if (orderOrStatus.orderStatusDesc) return orderOrStatus.orderStatusDesc
    if (isTimeoutClosedOrder(orderOrStatus)) return TIMEOUT_CLOSE_STATUS_TEXT
    return getStatusText(orderOrStatus.orderStatus)
  }
  const status = orderOrStatus
  const map = {
    0: '待支付',
    1: '待接单',
    8: '专属派单待确认',
    2: '待服务',
    3: '服务中',
    4: '待确认时长',
    5: '争议处理中',
    6: '已完成',
    7: '已取消',
    9: '待补差额'
  }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = {
    0: 'status-pending',
    1: 'status-waiting',
    8: 'status-waiting',
    2: 'status-accepted',
    3: 'status-service',
    4: 'status-confirm',
    5: 'status-dispute',
    9: 'status-balance',
    6: 'status-completed',
    7: 'status-cancelled'
  }
  return map[status] || 'status-default'
}

const getDoctorNamePlaceholder = (order) => {
  if (!order) return '待分配'
  if (Number(order.orderStatus) === 7) {
    return getStatusText(order)
  }
  return Number(order.paymentStatus) === 0 ? '待支付' : '待分配'
}


const handleOrderClick = (order) => {
  uni.navigateTo({
    url: `/subpkg/order/order-detail?orderNo=${order.orderNo}`
  })
}

const handleDetailClick = (order) => {
  handleOrderClick(order)
}

const openAttendantDetail = async (order) => {
  if (navigateToAttendantDetail(order)) return

  if (!order?.orderNo) {
    uni.showToast({ title: '陪诊师资料暂不可用', icon: 'none' })
    return
  }

  try {
    const response = await get(`/api/orders/${order.orderNo}`)
    const detail = response?.code === 200 ? response.data : null
    if (!navigateToAttendantDetail(detail || {})) {
      uni.showToast({ title: '陪诊师资料暂不可用', icon: 'none' })
    }
  } catch (error) {
    console.error('获取陪诊师详情入口失败:', error)
    uni.showToast({ title: '陪诊师资料暂不可用', icon: 'none' })
  }
}

const handlePay = (order) => {
  uni.navigateTo({
    url: `/subpkg/appointment-flow/04-order-confirm-page?orderNo=${order.orderNo}`
  })
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(() => {
    if (!pageActive || isOrderListLoading() || isOrderSocketOpen()) return
    const shouldPoll = orders.value.some(order =>
      (order.paymentStatus === 0 && order.orderStatus !== 7) ||
      [1, 2, 3, 4, 5, 8, 9].includes(order.orderStatus)
    )
    if (shouldPoll) loadOrders({ silent: true })
  }, ORDER_LIST_POLL_INTERVAL)
}

const isOrderRelatedMessage = (message) => {
  if (!message) return false
  const type = String(message.type || message.eventType || '').toUpperCase()
  return ORDER_EVENT_TYPES.includes(type)
}

const extractOrderEventPayload = (message) => {
  if (!message) return {}
  const payload = message.data && typeof message.data === 'object' ? message.data : {}
  return {
    orderId: Number(message.orderId || payload.orderId || 0),
    orderNo: String(message.orderNo || payload.orderNo || ''),
    orderStatus: payload.orderStatus ?? message.orderStatus,
    reason: String(payload.reason ?? message.reason ?? '')
  }
}

const applyOrderStatusPatch = ({ orderId, orderNo, orderStatus, reason }) => {
  const nextStatus = Number(orderStatus)
  if (!Number.isFinite(nextStatus)) return
  orders.value = orders.value.map((order) => {
    const matchById = orderId && Number(order.orderId || 0) === orderId
    const matchByNo = orderNo && String(order.orderNo || '') === orderNo
    if (!matchById && !matchByNo) return order
    const nextOrder = {
      ...order,
      orderStatus: nextStatus
    }
    if (reason) {
      nextOrder.cancelReason = reason
      if (nextStatus === 7 && reason.includes('超时未匹配到陪诊师')) {
        nextOrder.orderStatusDesc = TIMEOUT_CLOSE_STATUS_TEXT
      }
    }
    if (Number(order.orderStatus) === nextStatus && !reason) return order
    return {
      ...nextOrder
    }
  })
}

const scheduleOrderListRefresh = (delay = 700) => {
  if (!pageActive) return
  if (socketRefreshTimer) clearTimeout(socketRefreshTimer)
  socketRefreshTimer = setTimeout(() => {
    loadOrders({ silent: true })
  }, delay)
}

const handleSocketMessage = (message) => {
  if (!pageActive) return
  if (!isOrderRelatedMessage(message)) return
  applyOrderStatusPatch(extractOrderEventPayload(message))
  scheduleOrderListRefresh()
}

const setupWebSocketListener = () => {
  if (socketListener) removeOrderListener(socketListener)
  socketListener = handleSocketMessage
  addOrderListener(socketListener)
}

const teardownWebSocketListener = () => {
  if (socketListener) {
    removeOrderListener(socketListener)
    socketListener = null
  }
}

const cleanupRealtime = () => {
  stopPolling()
  stopClockTicker()
  if (socketRefreshTimer) {
    clearTimeout(socketRefreshTimer)
    socketRefreshTimer = null
  }
  teardownWebSocketListener()
}

onUnmounted(() => {
  cleanupRealtime()
})
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.order-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
}
.status-tabs {
  background-color: #fff;
  padding: 0 10rpx;
  margin-bottom: 20rpx;
}
.tabs-scroll {
  white-space: nowrap;
  width: 100%;
}
.tabs-container {
  display: flex;
  padding: 0 10rpx;
}
.tab-item {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 30rpx;
  position: relative;
  flex-shrink: 0;
}
.tab-label {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40rpx;
}
.tab-text {
  font-size: 28rpx;
  color: #666;
  transition: all 0.3s;
}
.tab-red-dot {
  position: absolute;
  top: -5rpx;
  right: -15rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: #ff3b30;
  box-shadow: 0 0 0 5rpx rgba(255, 59, 48, 0.12);
}
.tab-item.active .tab-text {
  color: #007AFF;
  font-weight: 600;
  font-size: 30rpx;
}
.tab-item.active .tab-red-dot {
  background: #ff4d4f;
  box-shadow: 0 0 0 5rpx rgba(255, 77, 79, 0.16);
}
.active-line {
  width: 40rpx;
  height: 4rpx;
  background-color: #007AFF;
  border-radius: 2rpx;
  position: absolute;
  bottom: 10rpx;
}
.content-wrapper {
  padding: 0 24rpx;
}
.order-card {
  background-color: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f5f7fa;
}
.order-number {
  font-size: 24rpx;
  color: #999;
}
.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
}
.status-pending { background: #fff7e6; color: #fa8c16; }
.status-waiting { background: #e6f7ff; color: #007AFF; }
.status-accepted { background: #f6ffed; color: #52c41a; }
.status-service { background: #f9f0ff; color: #722ed1; }
.status-confirm { background: #e6f4ff; color: #007AFF; }
.status-balance { background: #fff0f6; color: #c41d7f; }
.status-dispute { background: #fff7e6; color: #ad6800; }
.status-completed { background: #f5f7fa; color: #8c8c8c; }
.status-cancelled { background: #fff1f0; color: #f5222d; }
.service-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.service-type {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}
.hospital-name {
  font-size: 26rpx;
  color: #666;
}
.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.time-column {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8rpx;
  min-width: 0;
}
.time-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.meta-icon {
  width: 28rpx;
  height: 28rpx;
  opacity: 0.6;
}
.service-time {
  font-size: 26rpx;
  color: #666;
}
.assigned-countdown {
  font-size: 22rpx;
  color: #007AFF;
  background: #eef5ff;
  border-radius: 999rpx;
  padding: 4rpx 12rpx;
  line-height: 1.4;
}
.price {
  font-size: 32rpx;
  font-weight: 600;
  color: #ff4d4f;
}
.divider {
  height: 1rpx;
  background-color: #f5f7fa;
  margin: 0 -30rpx 24rpx;
}
.doctor-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.attendant-wrapper {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.attendant-wrapper.clickable {
  cursor: pointer;
}
.doctor-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background-color: #f0f0f0;
}
.doctor-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}
.attendant-arrow {
  font-size: 34rpx;
  color: #b8c4d3;
  line-height: 1;
}
.text-gray {
  color: #999;
}
.action-buttons {
  display: flex;
  gap: 16rpx;
}
.btn {
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  border-radius: 30rpx;
  margin: 0;
  line-height: 1.5;
}
.detail-btn {
  background-color: #fff;
  color: #666;
  border: 1rpx solid #ddd;
}
.pay-btn {
  background-color: #007AFF;
  color: #fff;
  border: 1rpx solid #007AFF;
}
.empty-state {
  padding-top: 100rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 20rpx;
  opacity: 0.5;
}
.empty-text {
  font-size: 28rpx;
  color: #999;
}
</style>
