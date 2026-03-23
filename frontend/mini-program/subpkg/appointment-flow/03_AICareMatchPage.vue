<template>
  <view class="page">
    <!-- 流程编号 -->
    <view class="process-number">
      <text class="number">03</text>
      <text class="number-label">陪诊师接单大厅</text>
    </view>
    
    <!-- 页面标题 -->
    <view class="header-section">
      <text class="page-title">陪诊师接单大厅</text>
      <text class="subtitle">您可以在这里接取待服务的订单</text>
    </view>

    <!-- 筛选条件 -->
    <view class="filter-section">
      <view class="filter-item">
        <text class="filter-label">服务类型:</text>
        <picker mode="selector" :range="serviceTypes" @change="onServiceTypeChange">
          <view class="filter-value">{{ selectedServiceTypeName }}</view>
        </picker>
      </view>
      <view class="filter-item">
        <text class="filter-label">排序方式:</text>
        <picker mode="selector" :range="sortOptions" @change="onSortChange">
          <view class="filter-value">{{ selectedSortName }}</view>
        </picker>
      </view>
    </view>

    <!-- 加载指示器 -->
    <view v-if="loading" class="loading-indicator">
      <text>正在加载订单...</text>
    </view>

    <!-- 订单列表 -->
    <view v-else class="order-list-container">
      <view v-for="(order, index) in displayedOrders" :key="order.orderId" class="order-card">
        <!-- 订单基本信息 -->
        <view class="order-header">
          <text class="order-no">订单号: {{ order.orderNo }}</text>
          <text class="order-status" :class="getStatusClass(order.orderStatus)">
            {{ getOrderStatusText(order.orderStatus) }}
          </text>
        </view>
        
        <!-- 服务信息 -->
        <view class="service-info">
          <view class="info-row">
            <text class="info-label">服务类型:</text>
            <text class="info-value">{{ getServiceTypeName(order.clinicType) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">医院:</text>
            <text class="info-value">{{ order.hospital }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">就诊时间:</text>
            <text class="info-value">{{ formatDate(order.appointmentTime) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">就诊人:</text>
            <text class="info-value">{{ order.patientName }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">预估时长:</text>
            <text class="info-value">{{ order.consultationDuration }}小时</text>
          </view>
          <view class="info-row">
            <text class="info-label">预估费用:</text>
            <text class="info-value price">¥{{ order.orderAmount }}</text>
          </view>
        </view>
        
        <!-- 操作按钮 -->
        <view class="action-buttons">
          <button class="btn-outline" @click="viewOrderDetail(order)">查看详情</button>
          <button 
            class="btn-primary" 
            :disabled="!canAcceptOrder(order)"
            @click="acceptOrder(order)">
            {{ canAcceptOrder(order) ? '立即接单' : '不可接单' }}
          </button>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-if="orders.length === 0 && !loading" class="empty-state">
        <text class="empty-text">暂无待接单的订单</text>
        <text class="empty-subtext">请稍后刷新或调整筛选条件</text>
      </view>

      <!-- 加载更多 -->
      <view v-if="hasMore && orders.length > 0" class="load-more-section">
        <button class="btn-load-more" @click="loadMoreOrders">加载更多</button>
      </view>
    </view>

    <!-- 订单详情弹窗 -->
    <view v-if="showOrderDetail" class="detail-modal" @click="closeOrderDetail">
      <view class="modal-content" @click.stop>
        <view class="detail-header">
          <view class="detail-title-wrap">
            <text class="detail-title">订单详情</text>
            <text class="detail-order-no">订单号：{{ currentOrder.orderNo }}</text>
          </view>
          <view class="close-btn" @click="closeOrderDetail">×</view>
        </view>

        <view class="detail-status-row">
          <text class="status-label">订单状态</text>
          <text class="order-status" :class="getStatusClass(currentOrder.orderStatus)">
            {{ getOrderStatusText(currentOrder.orderStatus) }}
          </text>
        </view>

        <scroll-view class="detail-scroll" scroll-y="true">
          <view class="detail-section">
            <text class="section-heading">基础信息</text>
            <view class="detail-item">
              <text class="item-label">服务类型</text>
              <text class="item-value">{{ getServiceTypeName(currentOrder.clinicType) }}</text>
            </view>
            <view class="detail-item">
              <text class="item-label">医院</text>
              <text class="item-value">{{ currentOrder.hospital || '--' }}</text>
            </view>
            <view class="detail-item">
              <text class="item-label">就诊时间</text>
              <text class="item-value">{{ formatDate(currentOrder.appointmentTime) }}</text>
            </view>
            <view class="detail-item">
              <text class="item-label">就诊人</text>
              <text class="item-value">{{ currentOrder.patientName || '--' }}</text>
            </view>
            <view class="detail-item">
              <text class="item-label">联系电话</text>
              <text class="item-value">{{ currentOrder.contactPhone || '--' }}</text>
            </view>
          </view>

          <view class="detail-section">
            <text class="section-heading">费用信息</text>
            <view class="detail-item">
              <text class="item-label">预估时长</text>
              <text class="item-value">{{ currentOrder.consultationDuration }}小时</text>
            </view>
            <view class="detail-item">
              <text class="item-label">单价</text>
              <text class="item-value">¥{{ currentOrder.unitPrice }}/小时</text>
            </view>
            <view class="detail-item">
              <text class="item-label">预估总额</text>
              <text class="item-value price">¥{{ currentOrder.orderAmount }}</text>
            </view>
            <view class="detail-item">
              <text class="item-label">已付定金</text>
              <text class="item-value price">¥{{ currentOrder.depositAmount }}</text>
            </view>
          </view>

          <view class="detail-section" v-if="currentOrder.selectedOptions && currentOrder.selectedOptions.length > 0">
            <text class="section-heading">特殊需求</text>
            <view class="options-list">
              <text class="option-tag" v-for="(option, index) in currentOrder.selectedOptions" :key="index">
                {{ getOptionText(option) }}
              </text>
            </view>
          </view>

          <view class="detail-section" v-if="currentOrder.customRequirement">
            <text class="section-heading">自定义需求</text>
            <text class="requirement-text">{{ currentOrder.customRequirement }}</text>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <button
            class="confirm-btn"
            :disabled="!canAcceptOrder(currentOrder)"
            @click="acceptOrderFromDetail"
          >
            {{ canAcceptOrder(currentOrder) ? '立即接单' : '不可接单' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post } from '@/utils/api.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'

// 页面数据
const loading = ref(true)
const orders = ref([])
const showOrderDetail = ref(false)
const currentOrder = ref({})

// 筛选条件
const serviceTypeFilter = ref('')
const sortOption = ref('time')
const currentPage = ref(0)
const pageSize = ref(10)
const hasMore = ref(true)

// 筛选选项
const serviceTypes = ['全部', '普通陪诊', '术后护理', '急诊陪同', '上门陪诊']
const sortOptions = ['时间优先', '距离优先', '价格优先']

const selectedServiceTypeName = computed(() => {
  return serviceTypes[serviceTypeFilter.value] || '全部'
})

const selectedSortName = computed(() => {
  return sortOptions[sortOption.value] || '时间优先'
})

// 显示的订单列表
const displayedOrders = computed(() => {
  let filtered = [...orders.value]
  
  // 服务类型筛选
  if (serviceTypeFilter.value > 0) {
    filtered = filtered.filter(order => order.clinicType === serviceTypeFilter.value)
  }
  
  // 排序
  if (sortOption.value === 0) {
    // 时间优先
    filtered.sort((a, b) => new Date(a.appointmentTime) - new Date(b.appointmentTime))
  } else if (sortOption.value === 1) {
    // 距离优先（简化处理）
    filtered.sort((a, b) => a.hospital.localeCompare(b.hospital))
  } else if (sortOption.value === 2) {
    // 价格优先
    filtered.sort((a, b) => a.orderAmount - b.orderAmount)
  }
  
  return filtered
})

// 页面加载
onLoad(() => {
  if (redirectPublicSafeToHome()) return
  loadOrders()
})

// 加载订单列表
const loadOrders = async () => {
  try {
    loading.value = true
    const response = await get('/attendant/orders/waiting', {
      page: currentPage.value,
      size: pageSize.value
    })
    
    if (response.data && response.data.content) {
      orders.value = [...orders.value, ...response.data.content]
      hasMore.value = response.data.content.length === pageSize.value
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMoreOrders = () => {
  if (hasMore.value) {
    currentPage.value++
    loadOrders()
  }
}

// 筛选变更
const onServiceTypeChange = (e) => {
  serviceTypeFilter.value = e.detail.value
}

const onSortChange = (e) => {
  sortOption.value = e.detail.value
}

// 查看订单详情
const viewOrderDetail = (order) => {
  currentOrder.value = order
  showOrderDetail.value = true
}

// 关闭详情弹窗
const closeOrderDetail = () => {
  showOrderDetail.value = false
}

// 判断是否可以接单
const canAcceptOrder = (order) => {
  return order.orderStatus === 1 // 待接单状态
}

// 接单操作
const acceptOrder = async (order) => {
  try {
    const response = await post(`/attendant/orders/${order.orderId}/accept`)
    if (response.data === '接单成功') {
      uni.showToast({ title: '接单成功', icon: 'success' })
      // 更新订单状态
      order.orderStatus = 2 // 已接单
      closeOrderDetail()
    } else {
      uni.showToast({ title: response.data || '接单失败', icon: 'none' })
    }
  } catch (error) {
    console.error('接单失败:', error)
    uni.showToast({ title: '接单失败', icon: 'none' })
  }
}

// 从详情页接单
const acceptOrderFromDetail = () => {
  acceptOrder(currentOrder.value)
}

// 工具函数
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

const getServiceTypeName = (type) => {
  const types = {
    1: '普通陪诊',
    2: '术后护理',
    3: '急诊陪同',
    4: '上门陪诊'
  }
  return types[type] || '未知类型'
}

const getOptionText = (option) => {
  const options = {
    1: '代取药',
    2: '代取报告',
    3: '需轮椅协助',
    4: '复查',
    5: '药物过敏'
  }
  return options[option] || '其他'
}

const getOrderStatusText = (status) => {
  const statuses = {
    1: '待接单',
    2: '已接单',
    3: '服务中',
    4: '待确认时长',
    5: '待支付差价',
    6: '已完成',
    7: '已取消'
  }
  return statuses[status] || '未知状态'
}

const getStatusClass = (status) => {
  const classes = {
    1: 'status-waiting',
    2: 'status-accepted',
    3: 'status-service',
    4: 'status-confirm',
    5: 'status-balance',
    6: 'status-completed',
    7: 'status-cancelled'
  }
  return classes[status] || ''
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
/* 流程编号样式 */
.process-number {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20rpx 0;
  background: linear-gradient(135deg, #007AFF, #2563EB);
  margin-bottom: 20rpx;
  border-radius: 12rpx;
}

.number {
  font-size: 48rpx;
  font-weight: bold;
  color: white;
  margin-right: 16rpx;
  background: rgba(255, 255, 255, 0.2);
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.number-label {
  font-size: 32rpx;
  color: white;
  font-weight: 500;
}

.page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding: 20rpx;
}

.header-section {
  text-align: center;
  padding: 40rpx 0;
}

.page-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.subtitle {
  font-size: 28rpx;
  color: #666;
}

.filter-section {
  display: flex;
  gap: 20rpx;
  margin-bottom: 30rpx;
  background: white;
  padding: 20rpx;
  border-radius: 12rpx;
}

.filter-item {
  flex: 1;
}

.filter-label {
  font-size: 26rpx;
  color: #333;
  display: block;
  margin-bottom: 10rpx;
}

.filter-value {
  font-size: 24rpx;
  color: #666;
  padding: 10rpx;
  background: #f5f7fa;
  border-radius: 8rpx;
}

.loading-indicator {
  text-align: center;
  padding: 60rpx 0;
  font-size: 28rpx;
  color: #666;
}

.order-list-container {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.order-card {
  background: white;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.order-no {
  font-size: 26rpx;
  color: #333;
  font-weight: 500;
}

.order-status {
  font-size: 24rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 500;
}

.status-waiting {
  background: #FFE58F;
  color: #FAAD14;
}

.status-accepted {
  background: #BAE7FF;
  color: #007AFF;
}

.status-service {
  background: #D9F7BE;
  color: #52C41A;
}

.status-confirm {
  background: #FFD8BF;
  color: #FA8C16;
}

.status-balance {
  background: #FFA39E;
  color: #F5222D;
}

.status-completed {
  background: #D9F7BE;
  color: #52C41A;
}

.status-cancelled {
  background: #F0F0F0;
  color: #8C8C8C;
}

.service-info {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
}

.info-label {
  font-size: 26rpx;
  color: #666;
}

.info-value {
  font-size: 26rpx;
  color: #333;
}

.price {
  color: #FF4D4F;
  font-weight: bold;
}

.action-buttons {
  display: flex;
  gap: 20rpx;
}

.btn-outline, .btn-primary {
  flex: 1;
  height: 72rpx;
  border-radius: 36rpx;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-outline {
  background: #f5f7fa;
  color: #333;
  border: 1rpx solid #d9d9d9;
}

.btn-primary {
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: white;
  border: none;
}

.btn-primary:disabled {
  background: #f5f7fa;
  color: #ccc;
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
}

.empty-text {
  font-size: 32rpx;
  color: #666;
  display: block;
  margin-bottom: 16rpx;
}

.empty-subtext {
  font-size: 26rpx;
  color: #999;
}

.load-more-section {
  text-align: center;
  padding: 30rpx 0;
}

.btn-load-more {
  width: 400rpx;
  height: 72rpx;
  background: #f5f7fa;
  color: #333;
  border: 1rpx solid #d9d9d9;
  border-radius: 36rpx;
  font-size: 28rpx;
}

.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.56);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 24rpx;
  box-sizing: border-box;
}

.modal-content {
  width: 100%;
  max-width: 660rpx;
  max-height: 86vh;
  background: #fff;
  border-radius: 28rpx;
  border: 1rpx solid #dbe8fb;
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
}

.close-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #edf4ff;
  color: #2d73cf;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  padding: 24rpx 24rpx 0;
}

.detail-title-wrap {
  flex: 1;
  min-width: 0;
}

.detail-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1f3f68;
  display: block;
}

.detail-order-no {
  margin-top: 6rpx;
  display: block;
  font-size: 22rpx;
  color: #6f83a5;
  word-break: break-all;
}

.detail-status-row {
  margin: 16rpx 24rpx 0;
  padding: 14rpx 16rpx;
  border-radius: 14rpx;
  background: #f5f9ff;
  border: 1rpx solid #d6e5fb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.status-label {
  font-size: 24rpx;
  color: #6883aa;
}

.detail-scroll {
  margin-top: 14rpx;
  padding: 0 24rpx;
  max-height: 58vh;
  box-sizing: border-box;
}

.detail-section {
  margin-bottom: 20rpx;
  border-radius: 16rpx;
  border: 1rpx solid #e3ecfa;
  background: #fff;
  padding: 16rpx;
}

.section-heading {
  font-size: 27rpx;
  font-weight: 700;
  color: #1f3f68;
  display: block;
  margin-bottom: 12rpx;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 10rpx;
  padding: 8rpx 0;
  border-bottom: 1rpx solid #edf2f8;
}

.detail-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.item-label {
  font-size: 24rpx;
  color: #6f83a5;
  flex-shrink: 0;
}

.item-value {
  font-size: 24rpx;
  color: #1f2937;
  text-align: right;
  word-break: break-all;
}

.options-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.option-tag {
  background: #f3f8ff;
  color: #2d73cf;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  border: 1rpx solid #d3e4fc;
  font-size: 24rpx;
}

.requirement-text {
  font-size: 24rpx;
  color: #475569;
  line-height: 1.6;
}

.modal-footer {
  padding: 16rpx 24rpx 24rpx;
  border-top: 1rpx solid #e5eefb;
}

.confirm-btn {
  width: 100%;
  min-height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: #fff;
  border: none;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.confirm-btn[disabled] {
  background: #e5ebf5;
  color: #94a3b8;
}
</style>
