<!-- 陪诊师端订单列表（迁入） -->
<template>
  <view class="order-page">
    <view class="header">
      <view class="header-module">
        <view class="search-box">
          <image class="search-icon" src="/static/sous.png"></image>
          <input
            class="search-input"
            placeholder="搜索患者、医院或服务类型"
            v-model="searchKeyword"
            @confirm="handleSearch"
            confirm-type="search"
          />
        </view>

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
                <text class="tab-text">{{ tab.name }}</text>
                <view class="active-line" v-if="activeStatus === tab.value"></view>
              </view>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>

    <view class="content-wrapper">
      <view v-if="loading" class="loading-state">
        <text class="loading-text">加载中...</text>
      </view>

      <scroll-view class="order-list" scroll-y>
        <view v-if="!loading && filteredOrders.length > 0" class="order-container">
          <view
            v-for="order in filteredOrders"
            :key="order.orderId"
            class="order-card"
            @click="goToDetail(order)"
          >
            <view class="card-content">
              <view class="order-header">
                <text class="order-number">订单号: {{ order.orderNo }}</text>
                <view class="status-badge" :class="getStatusClass(order.orderStatus)">
                  <text class="status-text">{{ getStatusText(order.orderStatus) }}</text>
                </view>
              </view>

              <view class="service-row">
                <text class="service-type">{{ order.serviceContent || order.serviceTypeName }}</text>
                <text class="hospital-name">{{ order.hospital }}</text>
              </view>

              <view class="order-meta">
                <view class="time-row">
                  <image src="/static/time.png" class="meta-icon"></image>
                  <text class="service-time">
                    {{ order.displayServiceTime }}
                  </text>
                </view>
                <text class="price">¥{{ formatAmount(getAttendantIncome(order.orderAmount)) }}</text>
              </view>

              <view class="divider"></view>

              <view class="doctor-info">
                <view class="attendant-wrapper">
                  <image class="doctor-avatar" :src="order.displayUserAvatar" mode="aspectFill"></image>
                  <text class="doctor-name">{{ order.patientName || order.contactPerson }}</text>
                </view>

                <view class="action-buttons">
                  <button class="btn detail-btn" @click.stop="goToDetail(order)">
                    <text>查看详情</text>
                  </button>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view v-else-if="!loading" class="empty-state">
          <image class="empty-icon" src="/static/order.png" mode="aspectFit"></image>
          <text class="empty-text">暂无相关订单</text>
        </view>
      </scroll-view>
    </view>

    <EscortBottomBar active="order" />
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import EscortBottomBar from '@/components/EscortBottomBar.vue'
import { get } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'
import { ensureRole } from '@/utils/auth-guard.js'
import { userPlaceholder } from '@/utils/assets.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'

const searchKeyword = ref('')

const statusTabs = ref([
  { name: '全部', value: null },
  { name: '待核销', value: 2 },
  { name: '服务中', value: 3 },
  { name: '待确认', value: 4 },
  { name: '已完成', value: 6 },
  { name: '已取消', value: 7 }
])

const activeStatus = ref(null)
const orders = ref([])
const loading = ref(false)
let socketListener = null
let pollTimer = null
let localOrderUpdatedListener = null

onMounted(() => {
  loadOrders()
  setupWebSocketListener()
  setupLocalOrderUpdatedListener()
  startPolling()
})

onShow(() => {
  if (redirectPublicSafeToHome()) return
  if (ensureRole('escort')) {
    loadOrders()
  }
})

const switchTab = (status) => {
  activeStatus.value = status
  loadOrders()
}

const handleSearch = () => {}

const loadOrders = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    orders.value = []
    return
  }

  loading.value = true
  try {
    const params = { attendantId: userInfo.id, page: 0, size: 200 }
    if (activeStatus.value !== null) params.orderStatus = activeStatus.value

    const res = await get('/attendant/orders', params)
    if (res.code === 200 && res.data && res.data.content) {
      orders.value = (res.data.content || []).map((order) => ({
        ...order,
        displayUserAvatar: resolveAvatarUrl(order?.userAvatar, userPlaceholder),
        displayServiceTime: [order.serviceDate, formatServiceTimeSlot(order.serviceTimeSlot || '')].filter(Boolean).join(' ').trim()
      }))
    }
    else orders.value = []
  } catch (e) {
    console.error('获取陪诊师订单失败:', e)
    orders.value = []
  } finally {
    loading.value = false
  }
}

const normalizeStatus = (status) => {
  const val = Number(status)
  if (Number.isFinite(val)) return val
  const map = {
    pending: 1,
    accepted: 2,
    in_progress: 3,
    waiting_confirm: 4,
    waiting_balance: 5,
    completed: 6,
    cancelled: 7
  }
  return map[String(status || '').toLowerCase()] || 0
}

const filteredOrders = computed(() => {
  let list = orders.value
  if (activeStatus.value !== null) {
    list = list.filter((o) => normalizeStatus(o.orderStatus) === Number(activeStatus.value))
  }

  const kw = (searchKeyword.value || '').trim()
  if (!kw) return list

  return list.filter(o =>
    (o.patientName && o.patientName.includes(kw)) ||
    (o.hospital && o.hospital.includes(kw)) ||
    (o.serviceContent && o.serviceContent.includes(kw)) ||
    (o.serviceTypeName && o.serviceTypeName.includes(kw))
  )
})

const formatAmount = (amount) => (amount ? Number(amount).toFixed(2) : '0.00')
const getAttendantIncome = (orderAmount) => Number(orderAmount || 0) * 0.9

const getStatusText = (status) => {
  const map = { 1: '待接单', 2: '待核销', 3: '服务中', 4: '待确认', 5: '待补款', 6: '已完成', 7: '已取消' }
  return map[status] || '未知'
}
const getStatusClass = (status) => {
  const map = { 1: 'status-waiting', 2: 'status-accepted', 3: 'status-service', 4: 'status-confirm', 6: 'status-completed', 7: 'status-cancelled' }
  return map[status] || 'status-default'
}


const goToDetail = (order) => {
  uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${order.orderId}` })
}

const handleSocketMessage = (message) => {
  if (
    message.type === 'NEW_ORDER' ||
    message.type === 'ORDER_ACCEPTED' ||
    message.type === 'SERVICE_STARTED' ||
    message.type === 'SERVICE_COMPLETED' ||
    message.type === 'ORDER_STATUS_CHANGED' ||
    message.type === 'ORDER_RELEASED_BY_ATTENDANT'
  ) {
    setTimeout(() => loadOrders(), 1000)
  }
}

const setupWebSocketListener = () => {
  if (socketListener) removeChatListener(socketListener)
  socketListener = handleSocketMessage
  addChatListener(socketListener)
}

const setupLocalOrderUpdatedListener = () => {
  if (localOrderUpdatedListener) uni.$off('escort-order-updated', localOrderUpdatedListener)
  localOrderUpdatedListener = () => {
    loadOrders()
  }
  uni.$on('escort-order-updated', localOrderUpdatedListener)
}

const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(() => loadOrders(), 15000)
}

onUnmounted(() => {
  if (socketListener) removeChatListener(socketListener)
  if (pollTimer) clearInterval(pollTimer)
  if (localOrderUpdatedListener) uni.$off('escort-order-updated', localOrderUpdatedListener)
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.order-page {
  @include escort-page;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  padding: 18rpx 24rpx 14rpx;
  background: #f5f7fa;
  flex-shrink: 0;
  z-index: 20;
}

.header-module {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 18rpx 16rpx 16rpx;
  box-shadow: $escort-shadow-card;
}

.search-box {
  background: #f5f7fa;
  border-radius: 40rpx;
  border: 1rpx solid #e6edf5;
  padding: 14rpx 22rpx;
  display: flex;
  align-items: center;
  min-height: 72rpx;
  box-sizing: border-box;
}

.search-icon {
  width: 30rpx;
  height: 30rpx;
  margin-right: 14rpx;
  opacity: 0.55;
}

.search-input {
  flex: 1;
  font-size: 27rpx;
  color: #1f2937;
}

.status-tabs {
  background: transparent;
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #edf2f8;
}

.tabs-scroll {
  white-space: nowrap;
  width: 100%;
}

.tabs-container {
  display: flex;
  padding: 2rpx 2rpx 2rpx 0;
  gap: 10rpx;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #ffffff;
  border: 1rpx solid #e1e8f2;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.tab-text {
  font-size: 25rpx;
  color: #5b6575;
  transition: all 0.25s;
}

.tab-item.active {
  background: $escort-color-primary;
  border-color: $escort-color-primary;
  box-shadow: $escort-shadow-primary;
}

.tab-item.active .tab-text {
  color: #ffffff;
  font-weight: 600;
}

.active-line {
  display: none;
}

.content-wrapper {
  padding: 12rpx 24rpx 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.order-list {
  flex: 1;
  min-height: 0;
  box-sizing: border-box;
}

.order-container {
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.loading-state {
  padding: 36rpx 0;
  text-align: center;
}

.loading-text {
  font-size: 25rpx;
  color: #7d8898;
}

.order-card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 26rpx;
  margin-bottom: 20rpx;
  box-shadow: $escort-shadow-card;
  animation: fadeUp 0.24s ease both;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.order-card:active {
  transform: scale(0.985);
  box-shadow: 0 10rpx 28rpx rgba(31, 41, 55, 0.11);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #edf2f8;
}

.order-number {
  font-size: 23rpx;
  color: #8b95a6;
}

.status-badge {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  border: 1rpx solid transparent;
}

.status-waiting {
  background: #e8f1ff;
  color: #007AFF;
}

.status-accepted {
  background: #edf9ef;
  color: #52c41a;
}

.status-service {
  background: #f2edff;
  color: #6e45d7;
}

.status-confirm {
  background: #fff6e7;
  color: #e08b28;
}

.status-completed {
  background: #eef1f5;
  color: #6b7280;
}

.status-cancelled {
  background: #fff0f0;
  color: #ef4444;
}

.service-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.service-type {
  font-size: 31rpx;
  font-weight: 600;
  color: #1f2937;
}

.hospital-name {
  font-size: 24rpx;
  color: #5f6b7b;
}

.order-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.meta-icon {
  width: 26rpx;
  height: 26rpx;
  opacity: 0.55;
}

.service-time {
  font-size: 24rpx;
  color: #6b7280;
}

.price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ff5b4d;
}

.divider {
  height: 1rpx;
  background: #edf2f8;
  margin: 0 -26rpx 18rpx;
}

.doctor-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.attendant-wrapper {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.doctor-avatar {
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: #eef2f7;
}

.doctor-name {
  font-size: 27rpx;
  color: #1f2937;
}

.btn {
  font-size: 23rpx;
  padding: 10rpx 22rpx;
  border-radius: 28rpx;
  margin: 0;
  line-height: 1.4;
}

.detail-btn {
  background: #f2f6fb;
  color: #4b5a70;
  border: 1rpx solid #dfe7f1;
}

.empty-state {
  padding-top: 90rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  width: 190rpx;
  height: 190rpx;
  margin-bottom: 16rpx;
  opacity: 0.55;
}

.empty-text {
  font-size: 27rpx;
  color: #7d8898;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(10rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
