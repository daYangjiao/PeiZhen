<!-- 陪诊师端订单列表，样式对齐用户端订单页 -->
<template>
  <view class="order-page" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="header">
      <!-- 搜索框 -->
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
    </view>

    <view class="content-wrapper">
      <!-- 状态标签（陪诊师：全部、待核销、服务中、待确认、已完成、已取消） -->
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

      <!-- 加载状态 -->
      <view v-if="loading" class="loading-state">
        <text class="loading-text">加载中...</text>
      </view>

      <!-- 订单列表（与用户端结构保持一致） -->
      <scroll-view class="order-list" scroll-y>
        <view v-if="!loading && filteredOrders.length > 0" class="order-container">
          <view
            v-for="order in filteredOrders"
            :key="order.orderId"
            class="order-card"
            @click="goToDetail(order)"
          >
            <view class="card-content">
              <!-- 订单头部：订单号 + 状态 -->
              <view class="order-header">
                <text class="order-number">订单号: {{ order.orderNo }}</text>
                <view class="status-badge" :class="getStatusClass(order.orderStatus)">
                  <text class="status-text">{{ getStatusText(order.orderStatus) }}</text>
                </view>
              </view>

              <!-- 服务类型 + 医院 -->
              <view class="service-row">
                <text class="service-type">{{ order.serviceContent || order.serviceTypeName }}</text>
                <text class="hospital-name">{{ order.hospital }}</text>
              </view>

              <!-- 服务时间 & 金额 -->
              <view class="order-meta">
                <view class="time-row">
                  <image src="/static/time.png" class="meta-icon"></image>
                  <text class="service-time">
                    {{ order.serviceDate }} {{ order.serviceTimeSlot || '' }}
                  </text>
                </view>
                <text class="price">¥{{ formatAmount(getAttendantIncome(order.orderAmount)) }}</text>
              </view>

              <!-- 分割线 -->
              <view class="divider"></view>

              <!-- 底部：患者信息 + 查看详情 -->
              <view class="doctor-info">
                <view class="attendant-wrapper">
                  <image class="doctor-avatar" :src="getPatientAvatar(order)" mode="aspectFill"></image>
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

        <!-- 空状态 -->
        <view v-else-if="!loading" class="empty-state">
          <image class="empty-icon" src="/static/order.png" mode="aspectFit"></image>
          <text class="empty-text">暂无相关订单</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, config } from '@/utils/api.js'
import { addChatListener, removeChatListener } from '@/utils/chat-websocket.js'

const statusBarHeight = ref(0)
const searchKeyword = ref('')

// 陪诊师端状态：2 待核销(待服务)、3 服务中、4 待确认、6 已完成、7 已取消
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

onMounted(() => {
  const systemInfo = uni.getSystemInfoSync()
  statusBarHeight.value = systemInfo.statusBarHeight || 0
  loadOrders()
  setupWebSocketListener()
  startPolling()
})

// Tab 页面从其他页面切回来时不会触发 onMounted，需要在 onShow 主动刷新
onShow(() => {
  loadOrders()
})

const switchTab = (status) => {
  activeStatus.value = status
  loadOrders()
}

const handleSearch = () => {
  loadOrders()
}

const loadOrders = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    orders.value = []
    return
  }

  loading.value = true
  try {
    const res = await get('/attendant/orders', {
      attendantId: userInfo.id,
      orderStatus: activeStatus.value,
      page: 0,
      size: 50
    })
    if (res.code === 200 && res.data && res.data.content) {
      orders.value = res.data.content
    } else {
      orders.value = []
    }
  } catch (e) {
    console.error('获取陪诊师订单失败:', e)
    orders.value = []
  } finally {
    loading.value = false
  }
}

const filteredOrders = computed(() => {
  if (!searchKeyword.value) return orders.value
  const kw = searchKeyword.value.trim()
  if (!kw) return orders.value
  return orders.value.filter(o =>
    (o.patientName && o.patientName.includes(kw)) ||
    (o.hospital && o.hospital.includes(kw)) ||
    (o.serviceContent && o.serviceContent.includes(kw)) ||
    (o.serviceTypeName && o.serviceTypeName.includes(kw))
  )
})

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return Number(amount).toFixed(2)
}

const getAttendantIncome = (orderAmount) => {
  const raw = Number(orderAmount || 0)
  return raw * 0.9
}

const getStatusText = (status) => {
  const map = {
    1: '待接单',
    2: '待核销',
    3: '服务中',
    4: '待确认',
    5: '待补款',
    6: '已完成',
    7: '已取消'
  }
  return map[status] || '未知'
}

const getStatusClass = (status) => {
  const map = {
    1: 'status-waiting',
    2: 'status-accepted',
    3: 'status-service',
    4: 'status-confirm',
    6: 'status-completed',
    7: 'status-cancelled'
  }
  return map[status] || 'status-default'
}

// 获取患者头像（陪诊师端看到的是患者信息）
const getPatientAvatar = (order) => {
  const placeholder = '/static/user-placeholder.png'
  if (!order || !order.userAvatar) return placeholder
  const path = order.userAvatar
  if (path.startsWith('http')) return path
  const base = (config.baseURL || '').replace(/\/$/, '')
  if (path.startsWith('/')) return base + path
  return base + '/' + path
}

const goToDetail = (order) => {
  uni.navigateTo({
    url: `/subpkg/order/detail?orderId=${order.orderId}`
  })
}

// WebSocket 消息处理：订单状态变化时刷新列表
const handleSocketMessage = (message) => {
  console.log('陪诊师订单页收到WebSocket消息:', message)
  if (
    message.type === 'NEW_ORDER' ||
    message.type === 'ORDER_ACCEPTED' ||
    message.type === 'SERVICE_STARTED' ||
    message.type === 'SERVICE_COMPLETED' ||
    message.type === 'ORDER_STATUS_CHANGED'
  ) {
    setTimeout(() => {
      loadOrders()
    }, 1000)
  }
}

const setupWebSocketListener = () => {
  if (socketListener) {
    removeChatListener(socketListener)
  }
  socketListener = handleSocketMessage
  addChatListener(socketListener)
}

const startPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
  pollTimer = setInterval(() => {
    loadOrders()
  }, 15000)
}

onUnmounted(() => {
  if (socketListener) {
    removeChatListener(socketListener)
    socketListener = null
  }
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.order-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding-bottom: 40rpx;
}

.header {
  padding: 20rpx 30rpx;
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.search-box {
  background-color: #f5f7fa;
  border-radius: 40rpx;
  padding: 16rpx 30rpx;
  display: flex;
  align-items: center;
}

.search-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 20rpx;
  opacity: 0.5;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
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

.tab-text {
  font-size: 28rpx;
  color: #666;
  transition: all 0.3s;
}

.tab-item.active .tab-text {
  color: #4A90E2;
  font-weight: 600;
  font-size: 30rpx;
}

.active-line {
  width: 40rpx;
  height: 4rpx;
  background-color: #4A90E2;
  border-radius: 2rpx;
  position: absolute;
  bottom: 10rpx;
}

.content-wrapper {
  padding: 0 24rpx;
}

.loading-state {
  padding: 40rpx 0;
  text-align: center;
}

.loading-text {
  font-size: 26rpx;
  color: #999;
}

.order-list {
  height: calc(100vh - var(--status-bar-height) - 88px);
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
.status-waiting { background: #e6f7ff; color: #1890ff; }
.status-accepted { background: #f6ffed; color: #52c41a; }
.status-service { background: #f9f0ff; color: #722ed1; }
.status-completed { background: #f5f5f5; color: #8c8c8c; }
.status-cancelled { background: #fff1f0; color: #f5222d; }

.service-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.service-type {
  font-size: 32rpx;
  font-weight: 400;
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

.doctor-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background-color: #f0f0f0;
}

.doctor-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 400;
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
  font-weight: 400;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background-color: #fff;
  color: #666;
  border: 1rpx solid #ddd;
}

.detail-btn .btn-icon {
  width: 24rpx;
  height: 24rpx;
}

.pay-btn {
  background-color: #4A90E2;
  color: #fff;
  border: 1rpx solid #4A90E2;
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
