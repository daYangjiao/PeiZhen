<template>
  <view class="payment-success-page">
    <view class="card success-card">
      <view class="icon-wrap success">
        <text class="icon-symbol">✓</text>
      </view>
      <text class="main-title">支付成功！</text>
      <text class="desc">您的陪诊订单已确认，正在为您匹配最合适的陪诊员</text>
      <button class="view-order-btn" @click="goToOrderDetail">查看订单详情</button>
    </view>

    <view class="card info-card">
      <view class="card-title">订单信息</view>
      <view class="info-item">
        <text class="label">订单编号</text>
        <text class="value mono">{{ orderData.orderNo || '—' }}</text>
      </view>
      <view class="info-item">
        <text class="label">支付金额</text>
        <text class="value price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
      <view class="info-item">
        <text class="label">支付时间</text>
        <text class="value">{{ orderData.paymentTime || payTime }}</text>
      </view>
      <view class="info-item">
        <text class="label">支付方式</text>
        <text class="value">微信支付</text>
      </view>
      <view class="info-item">
        <text class="label">订单状态</text>
        <text class="value">{{ orderData.orderStatusDesc || '已确认' }}</text>
      </view>
      <view class="info-item last">
        <text class="label">支付状态</text>
        <text class="value">{{ orderData.paymentStatusDesc || '已支付' }}</text>
      </view>
    </view>

    <view class="card info-card">
      <view class="card-title">服务详情</view>
      <view class="info-item">
        <text class="label">就诊时间</text>
        <text class="value">{{ orderData.serviceDate }} {{ formatServiceTime(orderData.serviceTime) }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊医院</text>
        <text class="value">{{ orderData.hospital || '—' }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊人</text>
        <text class="value">{{ orderData.patientName || '—' }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊人电话</text>
        <text class="value">{{ orderData.patientPhone || '—' }}</text>
      </view>
      <view class="info-item">
        <text class="label">服务类型</text>
        <text class="value">{{ orderData.serviceTypeName || '—' }}</text>
      </view>
      <view class="info-item">
        <text class="label">症状描述</text>
        <text class="value">{{ getSymptomDescription() }}</text>
      </view>
      <view class="info-item last">
        <text class="label">其他要求</text>
        <text class="value">{{ orderData.otherRequirement || '无' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get } from '@/utils/api.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'

const orderData = ref({})
const payTime = ref('')

const fetchOrderDetail = async (orderNo) => {
  try {
    const response = await get(`/ai/guide/orders/${orderNo}/complete-info`)
    if (response && response.code === 200 && response.data) {
      orderData.value = response.data
    } else {
      uni.showToast({ title: '获取订单信息失败', icon: 'none' })
    }
  } catch (error) {
    console.error('【PaymentSuccessPage】获取订单完整信息失败:', error)
    uni.showToast({ title: '网络错误，获取订单信息失败', icon: 'none' })
  }
}

onLoad(async (options) => {
  const now = new Date()
  payTime.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`

  const orderNo = options.orderNo || ''
  if (!orderNo) {
    uni.showToast({ title: '订单号错误', icon: 'none' })
    uni.redirectTo({ url: '/pages/role-user/home' })
    return
  }

  await fetchOrderDetail(orderNo)
})

const formatAmount = (amount) => {
  const num = Number(amount)
  if (Number.isNaN(num)) return '0.00'
  return num.toFixed(2)
}

const formatServiceTime = (serviceTime) => formatServiceTimeSlot(serviceTime || '')

const getSymptomDescription = () => {
  const { symptoms } = orderData.value
  if (!symptoms) return '无'
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms.filter(item => item && item.trim() && item !== '无' && item !== 'null')
    return validSymptoms.length > 0 ? validSymptoms.join(', ') : '无'
  }
  return symptoms
}

const goToOrderDetail = () => {
  if (!orderData.value.orderNo) {
    uni.showToast({ title: '无法跳转：缺少订单号', icon: 'none' })
    return
  }

  const targetOrderNo = encodeURIComponent(orderData.value.orderNo)
  uni.reLaunch({
    url: '/pages/role-user/order',
    success() {
      setTimeout(() => {
        uni.navigateTo({
          url: `/subpkg/order/order-detail?orderNo=${targetOrderNo}`
        })
      }, 150)
    }
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';

.payment-success-page {
  @include user-page;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(44rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.card {
  @include user-card(28rpx);
  margin-bottom: 18rpx;
}

.success-card {
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #f3fbf7 100%);
}

.icon-wrap {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 22rpx;
}

.icon-wrap.success {
  background: linear-gradient(135deg, #28c76f 0%, #14b86a 100%);
  box-shadow: 0 18rpx 36rpx rgba(40, 199, 111, 0.22);
}

.icon-symbol {
  font-size: 54rpx;
  line-height: 1;
  color: #ffffff;
  font-weight: 700;
}

.main-title {
  display: block;
  font-size: 40rpx;
  line-height: 1.2;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: $user-color-text-sub;
  margin-bottom: 34rpx;
}

.view-order-btn {
  width: 100%;
  @include user-primary-btn;
  border: none;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
}

.card-title {
  font-size: 30rpx;
  line-height: 1.2;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 18rpx;
}

.info-item {
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(220, 232, 248, 0.92);
}

.info-item.last {
  border-bottom: none;
  padding-bottom: 0;
}

.label {
  display: block;
  font-size: 22rpx;
  line-height: 1.3;
  color: $user-color-text-sub;
  margin-bottom: 10rpx;
}

.value {
  display: block;
  font-size: 26rpx;
  line-height: 1.6;
  color: $user-color-text-main;
  font-weight: 600;
  word-break: break-word;
}

.value.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 23rpx;
}

.value.price {
  color: $user-color-primary;
}
</style>
