<template>
  <view class="invite-attendant-page">
    <!-- 导航栏 -->
    <view class="nav-bar">
      <navigator url="/pages/order/order" class="back-btn">
        <text class="iconfont icon-back"> 返回</text>
      </navigator>
      <text class="page-title">邀请陪诊师接单</text>
    </view>

    <!-- 订单信息 -->
    <view class="order-summary">
      <view class="order-header">
        <text class="order-title">订单信息</text>
      </view>
      <view class="order-details">
        <view class="detail-item">
          <text class="label">订单号：</text>
          <text class="value">{{ orderInfo.orderNo }}</text>
        </view>
        <view class="detail-item">
          <text class="label">服务时间：</text>
          <text class="value">{{ formatDate(orderInfo.appointmentTime) }}</text>
        </view>
        <view class="detail-item">
          <text class="label">服务医院：</text>
          <text class="value">{{ orderInfo.hospital }}</text>
        </view>
      </view>
    </view>

    <!-- 筛选条件 -->
    <view class="filter-section">
      <view class="filter-header">
        <text class="filter-title">筛选条件</text>
      </view>
      <view class="filter-options">
        <picker mode="date" :value="selectedDate" @change="onDateChange">
          <view class="filter-item">
            <text class="filter-label">日期：</text>
            <text class="filter-value">{{ selectedDate || '请选择日期' }}</text>
          </view>
        </picker>
        <picker :range="timeSlots" @change="onTimeSlotChange">
          <view class="filter-item">
            <text class="filter-label">时段：</text>
            <text class="filter-value">{{ selectedTimeSlot || '请选择时段' }}</text>
          </view>
        </picker>
        <view class="filter-item">
          <text class="filter-label">专业：</text>
          <input 
            class="filter-input" 
            placeholder="请输入专业领域" 
            v-model="professionalField"
            @confirm="searchAttendants"
          />
        </view>
      </view>
      <button class="search-btn" @click="searchAttendants">搜索陪诊师</button>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading-container">
      <text>正在加载陪诊师信息...</text>
    </view>

    <!-- 陪诊师列表 -->
    <view v-else class="attendant-list">
      <view class="list-header">
        <text class="list-title">可预约陪诊师 ({{ attendants.length }}位)</text>
      </view>
      
      <view 
        v-for="attendant in attendants" 
        :key="attendant.id" 
        class="attendant-card"
        @click="selectAttendant(attendant)"
      >
        <image 
          :src="getAvatarUrl(attendant.avatarUrl)" 
          class="attendant-avatar" 
          mode="aspectFill"
          @error="handleImageError"
        />
        <view class="attendant-info">
          <view class="attendant-basic">
            <text class="attendant-name">{{ attendant.name }}</text>
            <text class="attendant-score">⭐ {{ attendant.score || 5.0 }}</text>
          </view>
          <text class="attendant-intro">{{ attendant.introduction || '暂无简介' }}</text>
          <view class="attendant-details">
            <text class="detail-tag">{{ attendant.professionalField || '通用陪诊' }}</text>
            <text class="detail-tag">{{ attendant.experienceYears || 0 }}年经验</text>
            <text class="detail-tag">¥{{ attendant.price || 0 }}/次</text>
          </view>
          <view class="available-times">
            <text class="times-label">可预约时间：</text>
            <view class="time-tags">
              <text 
                v-for="time in attendant.availableTimes" 
                :key="time"
                class="time-tag"
              >
                {{ time }}
              </text>
            </view>
          </view>
        </view>
        <button 
          class="invite-btn" 
          :class="{ 'invited': attendant.isInvited }"
          @click.stop="inviteAttendant(attendant)"
          :disabled="attendant.isInvited"
        >
          {{ attendant.isInvited ? '已邀请' : '邀请接单' }}
        </button>
      </view>

      <!-- 空状态 -->
      <view v-if="attendants.length === 0 && !loading" class="empty-state">
        <image class="empty-icon" src="/static/no_data.png" mode="aspectFit"></image>
        <text class="empty-text">暂无可预约的陪诊师</text>
        <text class="empty-subtext">请调整筛选条件或稍后再试</text>
      </view>
    </view>

    <!-- 邀请弹窗 -->
    <view v-if="showInviteModal" class="modal-overlay" @click="closeInviteModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">邀请陪诊师</text>
        </view>
        <view class="modal-body">
          <text class="modal-text">确定要邀请 {{ selectedAttendant.name }} 接单吗？</text>
          <text class="modal-desc">邀请后陪诊师将在其端收到接单通知</text>
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel-btn" @click="closeInviteModal">取消</button>
          <button class="modal-btn confirm-btn" @click="confirmInvite">确定邀请</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post } from '@/utils/api.js'

// 页面数据
const orderInfo = ref({})
const attendants = ref([])
const loading = ref(true)
const showInviteModal = ref(false)
const selectedAttendant = ref({})

// 筛选条件
const selectedDate = ref('')
const selectedTimeSlot = ref('')
const professionalField = ref('')
const timeSlots = ['09:00-12:00', '14:00-17:00', '19:00-21:00']

// 页面加载
onLoad((options) => {
  console.log('邀请陪诊师页面加载参数:', options)
  if (options && options.orderNo) {
    loadOrderInfo(options.orderNo)
  } else {
    uni.showToast({
      title: '缺少订单信息',
      icon: 'none'
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  }
})

// 加载订单信息
const loadOrderInfo = async (orderNo) => {
  try {
    const response = await get(`/ai/guide/orders/${orderNo}`)
    if (response && response.data) {
      orderInfo.value = response.data
      // 设置默认筛选条件
      if (orderInfo.value.appointmentTime) {
        const date = new Date(orderInfo.value.appointmentTime)
        selectedDate.value = date.toISOString().split('T')[0]
      }
      searchAttendants()
    }
  } catch (error) {
    console.error('加载订单信息失败:', error)
    uni.showToast({
      title: '加载订单信息失败',
      icon: 'none'
    })
  }
}

// 搜索陪诊师
const searchAttendants = async () => {
  loading.value = true
  try {
    const params = {
      hospitalName: orderInfo.value.hospital || '',
      serviceDate: selectedDate.value,
      serviceTimeSlot: selectedTimeSlot.value,
      professionalField: professionalField.value
    }
    
    const response = await get('/user/attendants/available', params)
    
    if (response && response.data) {
      attendants.value = response.data.map(attendant => ({
        ...attendant,
        isInvited: false,
        availableTimes: attendant.availableTimeSlots ? 
          attendant.availableTimeSlots.split(',') : ['09:00-12:00']
      }))
    }
  } catch (error) {
    console.error('搜索陪诊师失败:', error)
    uni.showToast({
      title: '搜索失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 日期选择
const onDateChange = (e) => {
  selectedDate.value = e.detail.value
}

// 时段选择
const onTimeSlotChange = (e) => {
  selectedTimeSlot.value = timeSlots[e.detail.value]
}

// 选择陪诊师
const selectAttendant = (attendant) => {
  console.log('选择陪诊师:', attendant)
  // 可以跳转到陪诊师详情页
}

// 邀请陪诊师
const inviteAttendant = (attendant) => {
  if (attendant.isInvited) return
  
  selectedAttendant.value = attendant
  showInviteModal.value = true
}

// 确认邀请
const confirmInvite = async () => {
  try {
    const response = await post('/user/invitations', {
      orderId: orderInfo.value.orderId,
      attendantId: selectedAttendant.value.id,
      invitationMessage: `您好，邀请您接单：${orderInfo.value.hospital} ${formatDate(orderInfo.value.appointmentTime)}`
    })
    
    if (response && response.code === 200) {
      uni.showToast({
        title: '邀请已发送',
        icon: 'success'
      })
      
      // 标记为已邀请
      const index = attendants.value.findIndex(a => a.id === selectedAttendant.value.id)
      if (index !== -1) {
        attendants.value[index].isInvited = true
      }
      
      closeInviteModal()
      
      // 3秒后返回订单详情
      setTimeout(() => {
        uni.navigateBack()
      }, 3000)
    }
  } catch (error) {
    console.error('邀请失败:', error)
    uni.showToast({
      title: '邀请失败',
      icon: 'none'
    })
  }
}

// 关闭邀请弹窗
const closeInviteModal = () => {
  showInviteModal.value = false
  selectedAttendant.value = {}
}

// 获取头像URL
const getAvatarUrl = (avatarPath) => {
  if (!avatarPath) return '/static/default-avatar.jpg'
  if (avatarPath.startsWith('http')) return avatarPath
  return `http://localhost:8080${avatarPath.startsWith('/') ? avatarPath : '/' + avatarPath}`
}

// 图片加载错误处理
const handleImageError = (e) => {
  console.log('图片加载失败:', e)
  // 可以设置默认头像
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(/\//g, '-')
}
</script>

<style scoped>
.invite-attendant-page {
  background-color: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 20rpx;
}

/* 导航栏 */
.nav-bar {
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  padding: 60rpx 30rpx 30rpx;
  display: flex;
  align-items: center;
  position: relative;
}

.back-btn {
  position: absolute;
  left: 30rpx;
  top: 60rpx;
  color: white;
  font-size: 28rpx;
}

.page-title {
  flex: 1;
  text-align: center;
  color: white;
  font-size: 36rpx;
  font-weight: bold;
}

/* 订单信息 */
.order-summary {
  background: white;
  margin: 20rpx 30rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.order-header {
  margin-bottom: 20rpx;
}

.order-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.label {
  font-size: 28rpx;
  color: #666;
}

.value {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}

/* 筛选条件 */
.filter-section {
  background: white;
  margin: 20rpx 30rpx;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.filter-header {
  margin-bottom: 20rpx;
}

.filter-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.filter-options {
  margin-bottom: 30rpx;
}

.filter-item {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  padding: 20rpx;
  background: #f8f9fa;
  border-radius: 12rpx;
}

.filter-label {
  font-size: 28rpx;
  color: #666;
  width: 120rpx;
}

.filter-value, .filter-input {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.filter-input {
  background: transparent;
  border: none;
}

.search-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  color: white;
  border: none;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
}

/* 加载状态 */
.loading-container {
  text-align: center;
  padding: 60rpx 30rpx;
  color: #666;
  font-size: 28rpx;
}

/* 陪诊师列表 */
.attendant-list {
  margin: 20rpx 30rpx;
}

.list-header {
  margin-bottom: 20rpx;
}

.list-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.attendant-card {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: flex-start;
}

.attendant-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.attendant-info {
  flex: 1;
  min-width: 0;
}

.attendant-basic {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.attendant-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.attendant-score {
  font-size: 26rpx;
  color: #ff9500;
}

.attendant-intro {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 16rpx;
  line-height: 1.4;
}

.attendant-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.detail-tag {
  font-size: 22rpx;
  color: #4A90E2;
  background: #e8f4ff;
  padding: 6rpx 12rpx;
  border-radius: 8rpx;
}

.available-times {
  margin-top: 16rpx;
}

.times-label {
  font-size: 24rpx;
  color: #666;
  display: block;
  margin-bottom: 12rpx;
}

.time-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.time-tag {
  font-size: 22rpx;
  color: #4A90E2;
  background: #e8f4ff;
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  border: 1rpx solid #4A90E2;
}

.invite-btn {
  width: 140rpx;
  height: 60rpx;
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
  border: none;
  border-radius: 30rpx;
  font-size: 24rpx;
  flex-shrink: 0;
  margin-left: 20rpx;
}

.invite-btn.invited {
  background: #cccccc;
  color: #666;
}

.invite-btn:disabled {
  background: #cccccc;
  color: #666;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80rpx 30rpx;
}

.empty-icon {
  width: 200rpx;
  height: 200rpx;
  margin-bottom: 30rpx;
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

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  width: 80%;
  max-width: 600rpx;
  background: white;
  border-radius: 20rpx;
  padding: 40rpx;
}

.modal-header {
  text-align: center;
  margin-bottom: 30rpx;
}

.modal-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.modal-body {
  margin-bottom: 40rpx;
}

.modal-text {
  font-size: 30rpx;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
  text-align: center;
}

.modal-desc {
  font-size: 26rpx;
  color: #666;
  display: block;
  text-align: center;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  border-radius: 40rpx;
  font-size: 30rpx;
  font-weight: bold;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
  border: 1rpx solid #ddd;
}

.confirm-btn {
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  color: white;
  border: none;
}
</style>