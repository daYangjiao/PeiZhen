<template>
  <view class="payment-success-page">
    <view class="success-hero-card">
      <view class="success-hero-icon">
        <text class="success-hero-icon-text">✓</text>
      </view>
      <text class="success-hero-title">支付成功</text>
      <text class="success-hero-desc">订单已锁定</text>
    </view>

    <view class="card summary-card">
      <view class="section-head">
        <view>
          <text class="section-title">订单摘要</text>
        </view>
        <view class="summary-amount-chip">
          <text class="summary-amount-label">实付</text>
          <text class="summary-amount-value">¥{{ formatAmount(orderData.totalPrice) }}</text>
        </view>
      </view>

      <view class="summary-grid">
        <view class="summary-item">
          <text class="summary-label">订单编号</text>
          <text class="summary-value mono">{{ orderData.orderNo || '—' }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">支付时间</text>
          <text class="summary-value">{{ orderData.paymentTime || payTime }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">支付方式</text>
          <text class="summary-value">在线支付</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">支付状态</text>
          <text class="summary-value">{{ orderData.paymentStatusDesc || '已支付' }}</text>
        </view>
      </view>
    </view>

    <view class="card service-card">
      <view class="section-head compact">
        <view>
          <text class="section-title">本次陪诊安排</text>
        </view>
      </view>

      <view class="service-highlight-card">
        <text class="service-highlight-label">就诊时间</text>
        <text class="service-highlight-value">{{ orderData.serviceDate }} {{ formatServiceTime(orderData.serviceTime) }}</text>
        <text class="service-highlight-desc">{{ orderData.hospital || '未填写医院信息' }}</text>
      </view>

      <view class="service-list">
        <view class="service-row">
          <text class="service-row-label">就诊人</text>
          <text class="service-row-value">{{ orderData.patientName || '—' }}</text>
        </view>
        <view class="service-row">
          <text class="service-row-label">联系电话</text>
          <text class="service-row-value">{{ orderData.patientPhone || '—' }}</text>
        </view>
        <view class="service-row">
          <text class="service-row-label">服务类型</text>
          <text class="service-row-value">{{ orderData.serviceTypeName || '陪诊服务' }}</text>
        </view>
      </view>

      <view class="detail-note-card">
        <text class="detail-note-title">症状描述</text>
        <text class="detail-note-text">{{ getSymptomDescription() }}</text>
      </view>
      <view class="detail-note-card muted">
        <text class="detail-note-title">其他要求</text>
        <text class="detail-note-text">{{ orderData.otherRequirement || '暂无补充要求' }}</text>
      </view>
    </view>

    <view class="success-action-bar">
      <button class="hero-primary-btn" @click="goToOrderDetail">查看订单详情</button>
      <button class="hero-secondary-btn" @click="goToOrderList">返回订单列表</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get } from '@/utils/api.js';
import { formatServiceTimeSlot } from '@/utils/order-display.js'

const orderData = ref({});
const payTime = ref('');

// 获取订单详情 (使用完整信息接口)
const fetchOrderDetail = async (orderNo) => {
  try {
    console.log('【PaymentSuccessPage】正在调用后端接口获取订单完整信息:', orderNo);
    const response = await get(`/ai/guide/orders/${orderNo}/complete-info`);

    console.log('【PaymentSuccessPage】后端返回的订单完整数据:', response);

    if (response && response.code === 200 && response.data) {
      orderData.value = response.data;
    } else {
      console.warn('【PaymentSuccessPage】后端返回数据为空或格式不正确:', response);
      uni.showToast({
        title: '获取订单信息失败',
        icon: 'none'
      });
    }
  } catch (error) {
    console.error('【PaymentSuccessPage】获取订单完整信息失败:', error);
    uni.showToast({
      title: '网络错误，获取订单信息失败',
      icon: 'none'
    });
  }
};

// 页面加载
onLoad(async (options) => {
  console.log('【PaymentSuccessPage】页面加载参数:', options);
  const now = new Date();
  payTime.value = now.getFullYear() + '-' +
                   String(now.getMonth() + 1).padStart(2, '0') + '-' +
                   String(now.getDate()).padStart(2, '0') + ' ' +
                   String(now.getHours()).padStart(2, '0') + ':' +
                   String(now.getMinutes()).padStart(2, '0') + ':' +
                   String(now.getSeconds()).padStart(2, '0');

  const orderNo = options.orderNo || '';
  if (!orderNo) {
    uni.showToast({ title: '订单号错误', icon: 'none' });
    uni.redirectTo({ url: '/pages/role-user/home' });
    return;
  }

  await fetchOrderDetail(orderNo);
});

/**
 * 格式化金额
 */
const formatAmount = (amount) => {
  const num = Number(amount);
  if (isNaN(num)) return '0.00';
  return num.toFixed(2);
};

const formatServiceTime = (serviceTime) => {
  return formatServiceTimeSlot(serviceTime || '')
}

const goToOrderList = () => {
  uni.reLaunch({
    url: '/pages/role-user/order'
  })
}

/**
 * 获取症状描述文本
 */
const getSymptomDescription = () => {
  const { symptoms } = orderData.value;
  if (!symptoms) {
    return '无';
  }
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms.filter(s => s && s.trim() && s !== '无' && s !== 'null');
    return validSymptoms.length > 0 ? validSymptoms.join(', ') : '无';
  }
  return symptoms;
};

/**
 * 跳转到订单详情页
 * 先重启到“订单列表”作为栈底，再从订单页进入详情，
 * 确保从详情页返回时回到订单列表，而不是回到下单/支付页。
 */
const goToOrderDetail = () => {
  if (!orderData.value.orderNo) {
    uni.showToast({ title: '无法跳转：缺少订单号', icon: 'none' });
    return;
  }

  const targetOrderNo = encodeURIComponent(orderData.value.orderNo);

  // 先重启到订单列表，再从订单页跳转到详情，保证返回时回到订单列表
  uni.reLaunch({
    url: '/pages/role-user/order',
    success() {
      setTimeout(() => {
        uni.navigateTo({
          url: `/subpkg/order/order-detail?orderNo=${targetOrderNo}`
        });
      }, 150);
    }
  });
};
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.payment-success-page {
  @include user-page;
  padding: 28rpx 24rpx calc(172rpx + env(safe-area-inset-bottom));
  font-size: 28rpx;
  min-height: 100vh;
  box-sizing: border-box;
}

.success-hero-card,
.card {
  @include user-card(28rpx);
}

.success-hero-card {
  padding: 40rpx 30rpx 32rpx;
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #f1f8ff 100%);
  margin-bottom: 24rpx;
}

.success-hero-icon {
  width: 112rpx;
  height: 112rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #16c47f 0%, #0ea765 100%);
  box-shadow: 0 18rpx 36rpx rgba(22, 196, 127, 0.24);
  margin: 0 auto 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-hero-icon-text {
  font-size: 56rpx;
  line-height: 1;
  color: #ffffff;
  font-weight: 700;
}

.success-hero-title {
  display: block;
  font-size: 42rpx;
  line-height: 1.2;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.success-hero-desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.4;
  color: $user-color-text-sub;
}

.hero-primary-btn,
.hero-secondary-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
}

.hero-primary-btn {
  background: linear-gradient(135deg, #007aff 0%, #2563eb 100%);
  color: #ffffff;
  box-shadow: 0 16rpx 30rpx rgba(37, 99, 235, 0.24);
}

.hero-secondary-btn {
  background: #ffffff;
  color: $user-color-primary;
  border: 1rpx solid rgba(0, 122, 255, 0.22);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18rpx;
  margin-bottom: 22rpx;
}

.section-head.compact {
  margin-bottom: 18rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  line-height: 1.18;
  color: $user-color-text-main;
  font-weight: 700;
}

.summary-amount-chip {
  padding: 14rpx 18rpx;
  border-radius: 22rpx;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.10) 0%, rgba(37, 99, 235, 0.14) 100%);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
}

.summary-amount-label,
.summary-label,
.detail-note-title {
  display: block;
  font-size: 22rpx;
  color: $user-color-text-sub;
  margin-bottom: 8rpx;
}

.summary-amount-value {
  font-size: 30rpx;
  line-height: 1.2;
  color: $user-color-primary;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.summary-item,
.service-highlight-card,
.detail-note-card {
  padding: 20rpx 22rpx;
  border-radius: 22rpx;
  background: #ffffff;
  border: 1rpx solid rgba(220, 232, 248, 0.9);
  box-shadow: 0 10rpx 24rpx rgba(18, 56, 109, 0.05);
}

.summary-value,
.service-row-value,
.detail-note-text {
  display: block;
  font-size: 24rpx;
  line-height: 1.65;
  color: $user-color-text-main;
  font-weight: 600;
  word-break: break-word;
}

.summary-value.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 22rpx;
}

.service-card {
  margin-top: 24rpx;
}

.service-highlight-card {
  margin-bottom: 18rpx;
  background: linear-gradient(145deg, #edf6ff 0%, #f7fbff 100%);
}

.service-highlight-label {
  display: block;
  font-size: 22rpx;
  color: $user-color-text-sub;
  margin-bottom: 10rpx;
}

.service-highlight-value {
  display: block;
  font-size: 30rpx;
  line-height: 1.45;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.service-highlight-desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
}

.service-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-bottom: 18rpx;
}

.service-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  padding: 0 4rpx;
}

.service-row-label {
  font-size: 22rpx;
  color: $user-color-text-sub;
}

.detail-note-card {
  margin-bottom: 14rpx;
}

.detail-note-card.muted {
  margin-bottom: 0;
  background: linear-gradient(180deg, #fbfdff 0%, #f5f9ff 100%);
}

.success-action-bar {
  position: sticky;
  bottom: 0;
  z-index: 20;
  margin-top: 28rpx;
  padding: 20rpx 0 calc(8rpx + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(244, 248, 255, 0) 0%, rgba(244, 248, 255, 0.96) 28%, rgba(244, 248, 255, 1) 100%);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

@media (max-width: 680px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
