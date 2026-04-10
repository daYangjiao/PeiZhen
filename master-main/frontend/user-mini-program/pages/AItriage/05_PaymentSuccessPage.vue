<template>
  <view class="payment-success-page">
    <!-- 支付成功卡片 -->
    <view class="card success-card">
      <view class="icon">
        <text class="icon-check">✓</text>
      </view>
      <text class="main-title">支付成功！</text>
      <text class="desc">您的陪诊订单已确认，正在为您匹配最合适的陪诊员</text>
      <!-- 新增：查看订单详情按钮 -->
      <button class="view-order-btn" @click="goToOrderDetail">查看订单详情</button>
    </view>

    <!-- 订单信息 -->
    <view class="card order-info">
      <view class="title"><text class="iconfont">📝</text> 订单信息</view>
      <view class="info-item">
        <text class="label">订单编号</text>
        <text class="value">{{ orderData.orderNo }}</text>
      </view>
      <view class="info-item">
        <text class="label">支付金额</text>
        <text class="value">¥{{ formatAmount(orderData.totalPrice) }}</text>
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
        <text class="value">{{ orderData.orderStatusDesc }}</text>
      </view>
      <view class="info-item">
        <text class="label">支付状态</text>
        <text class="value">{{ orderData.paymentStatusDesc }}</text>
      </view>
    </view>

    <!-- 服务详情 -->
    <view class="card service-detail">
      <view class="title"><text class="iconfont">💼</text> 服务详情</view>

      <view class="info-item">
        <text class="label">就诊时间</text>
        <text class="value">{{ orderData.serviceDate }} {{ orderData.serviceTime }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊医院</text>
        <text class="value">{{ orderData.hospital }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊人</text>
        <text class="value">{{ orderData.patientName }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊人电话</text>
        <text class="value">{{ orderData.patientPhone }}</text>
      </view>
      <view class="info-item">
        <text class="label">服务类型</text>
        <text class="value">{{ orderData.serviceTypeName }}</text>
      </view>

      <view class="info-item">
        <text class="label">症状描述</text>
        <text class="value">{{ getSymptomDescription() }}</text>
      </view>
      <!-- 修改：显示 otherRequirement 字段 -->
      <view class="info-item">
        <text class="label">其他要求</text>
        <text class="value">{{ orderData.otherRequirement || '无' }}</text>
      </view>

    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get } from '@/utils/api.js';

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
    uni.redirectTo({ url: '/pages/index/index' });
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
 */
const goToOrderDetail = () => {
  if (orderData.value.orderNo) {
    uni.redirectTo({
      url: `/pages/OrderDetailPage/OrderDetailPage?orderNo=${encodeURIComponent(orderData.value.orderNo)}`
    });
  } else {
    uni.showToast({ title: '无法跳转：缺少订单号', icon: 'none' });
  }
};
</script>

<style scoped>
.payment-success-page {
  background-color: #f5f5f5;
  padding: 20rpx 40rpx;
  font-size: 28rpx;
  min-height: 100vh;
}
.card {
  background-color: white;
  border-radius: 16rpx;
  padding: 60rpx 40rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  width: 100%;
  box-sizing: border-box;
}
.success-card {
  text-align: center;
}
.icon {
  width: 80rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 50%;
  background-color: #00d46c;
  color: white;
  font-size: 48rpx;
  margin: 0 auto 20rpx;
}
.main-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin: 20rpx 0 16rpx;
}
.desc {
  color: #666;
  font-size: 28rpx;
  line-height: 1.5;
  padding: 0 20rpx;
  margin-bottom: 100rpx;
}
.view-order-btn {
  width: 240rpx;
  height: 60rpx;
  background-color: #ffffff;
  color: #007AFF;
  border: 1rpx solid #007AFF;
  border-radius: 30rpx;
  font-size: 28rpx;
  line-height: 60rpx;
  margin: 20rpx auto 0;
  display: block;
}
.card .title {
  font-weight: bold;
  font-size: 32rpx;
  margin-bottom: 30rpx;
  display: flex;
  align-items: center;
  color: #333;
}
.info-item {
  display: flex;
  justify-content: space-between;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eee;
}
.info-item:last-child {
  border-bottom: none;
}
.label {
  color: #666;
}
.value {
  color: #333;
  font-weight: 500;
}
</style>