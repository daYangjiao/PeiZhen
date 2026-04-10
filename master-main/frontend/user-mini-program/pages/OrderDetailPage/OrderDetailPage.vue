<template>
  <view class="container">
    <view class="return-to-orders-section">
      <button class="return-to-orders-btn" @click="returnToOrders">返回订单列表</button>
    </view>

    <!-- 订单号 + 状态 -->
    <view class="order-info">
      <view class="header-row">
        <text class="label">订单号</text>
        <view class="status-tag" :class="getStatusClass(order)">
          {{ getOrderStatusText(order) }}
        </view>
      </view>
      <text class="order-number">{{ order.orderNo }}</text>
    </view>

    <!-- 服务信息 -->
    <view class="service-info">
      <view class="info-item">
        <text class="label">服务类型：</text>
        <text class="value">{{ order.serviceTypeName || getServiceTypeName(order.clinicType) }}</text>
      </view>
      <view class="info-item">
        <text class="label">服务时间：</text>
        <text class="value">{{ order.serviceDate }} {{ order.serviceTime }}</text>
      </view>
      <view class="info-item">
        <text class="label">服务医院：</text>
        <text class="value">{{ order.hospital }}</text>
      </view>
      <view class="info-item">
        <text class="label">就诊人：</text>
        <text class="value">{{ order.patientName }}</text>
      </view>
      <view class="info-item">
        <text class="label">联系电话：</text>
        <text class="value">{{ order.patientPhone || order.contactPhone }}</text>
      </view>
      <!-- 新增：症状描述 -->
      <view class="info-item">
        <text class="label">症状描述：</text>
        <text class="value">{{ getSymptomDescription() }}</text>
      </view>
      <!-- 新增：其他需求 -->
      <view class="info-item">
        <text class="label">其他需求：</text>
        <text class="value">{{ order.otherRequirement || '无' }}</text>
      </view>
    </view>

    <!-- 陪诊师信息 -->
    <view class="companion-info" v-if="order.attendantName && order.attendantName !== '待分配陪诊师'">
      <image
        :src="getAvatarUrl(order.attendantAvatar)"
        class="avatar"
        mode="aspectFill"
        @error="handleImageError"
      ></image>
      <view class="companion-details">
        <text class="name">{{ order.attendantName }}</text>
        <view class="phone" v-if="order.attendantPhone">
          <image src="/static/contact.png" class="phone-icon"></image>
          <text class="phone-number">{{ order.attendantPhone }}</text>
        </view>
        <view class="rating">
          <text class="star">⭐</text>
          <text class="score">{{ order.attendantScore || 5 }}</text>
        </view>
      </view>
    </view>
    <view class="companion-info" v-else-if="(order.orderStatus === 0 || order.orderStatus === 1) && order.paymentStatus === 1">
      <view class="companion-details">
        <text class="name">待分配陪诊师</text>
        <text class="desc">系统正在为您匹配合适的陪诊师...</text>
      </view>
    </view>
    <view class="companion-info" v-else-if="order.paymentStatus === 0 && order.orderStatus !== 7">
      <view class="companion-details">
        <text class="name">待支付</text>
        <text class="desc">请尽快完成支付以确认订单</text>
      </view>
    </view>
    <view class="companion-info" v-else-if="order.orderStatus === 7">
      <view class="companion-details">
        <text class="name">订单已取消</text>
        <text class="desc">此订单已取消，无法分配陪诊师</text>
      </view>
    </view>

    <!-- 费用信息 -->
    <view class="payment-info">
      <view class="payment-row">
        <text class="label">总费用</text>
        <text class="price">¥{{ formatAmount(order.totalPrice || order.orderAmount) }}</text>
      </view>
      <view class="payment-row" v-if="order.priceCalculation">
        <text class="label">费用说明</text>
        <text class="value">{{ order.priceCalculation }}</text>
      </view>
    </view>

    <!-- 二维码区域 -->
    <view class="qr-section" v-if="showQRCode">
      <view class="qr-title">服务确认二维码</view>
      <view class="qr-container">
        <image :src="order.qrCodeUrl" class="qr-image" mode="aspectFit"></image>
        <br>
        <text class="qr-desc">请陪诊师扫描此二维码确认开始服务</text>
      </view>
    </view>

    <!-- 时长确认区域 -->
    <view class="duration-confirm-section" v-if="showDurationConfirm">
      <view class="confirm-title">请确认服务时长</view>
      <view class="duration-info">
        <text class="info-text">陪诊师已结束服务，实际服务时长为 {{ order.actualDuration }} 小时</text>
        <text class="amount-text" v-if="order.balanceAmount && order.balanceAmount > 0">
          需补差价：¥{{ order.balanceAmount }}
        </text>
      </view>
      <button class="confirm-btn" @click="confirmDuration">确认时长</button>
    </view>

    <!-- 差价支付区域 -->
    <view class="balance-payment-section" v-if="showBalancePayment">
      <view class="payment-title">差价支付</view>
      <view class="payment-amount">
        <text class="amount-label">需支付差价：</text>
        <text class="amount-price">¥{{ order.balanceAmount }}</text>
      </view>
      <button class="pay-btn" @click="payBalance">立即支付</button>
    </view>

    <!-- 服务记录 -->
    <view class="record-section">
      <view class="record-title">服务记录</view>
      <view class="record-list">
        <view
          v-for="(item, index) in serviceSteps"
          :key="index"
          class="step-item"
        >
          <view class="step-dot"></view>
          <view class="step-content">
            <text class="step-title">{{ item.title }}</text>
            <text class="step-desc">{{ item.desc }}</text>
          </view>
          <text class="step-time">{{ item.time }}</text>
        </view>
      </view>
    </view>

    <!-- 底部固定导航栏 -->
    <view class="bottom-nav">
      <view class="nav-item" @click="callCompanion" v-if="order.attendantPhone">
        <image src="/static/contact.png" class="nav-icon"></image>
        <text>联系</text>
      </view>
      <view class="nav-item" @click="consult">
        <image src="/static/advisory.png" class="nav-icon"></image>
        <text>咨询</text>
      </view>
      <view class="nav-item" @click="evaluate" v-if="order.orderStatus === 6">
        <image src="/static/valuation.png" class="nav-icon"></image>
        <text>评价</text>
      </view>
      <view class="nav-item" @click="share">
        <image src="/static/share.png" class="nav-icon"></image>
        <text>分享</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post, config } from '@/utils/api.js';
import { addSocketListener, removeSocketListener } from '@/utils/websocket.js';

// 使用 ref 定义响应式变量
const order = ref({});
let pollTimer = null; // 轮询定时器

// 计算属性
const showQRCode = computed(() => {
  // 状态为2(已接单)且有二维码URL时显示
  return order.value.orderStatus === 2 && order.value.qrCodeUrl;
});

const showDurationConfirm = computed(() => {
  return order.value.orderStatus === 4; // 待确认时长状态
});

const showBalancePayment = computed(() => {
  return order.value.orderStatus === 5 && order.value.balanceAmount && order.value.balanceAmount > 0; // 待支付差价状态
});

// 服务记录 (根据订单状态动态生成)
const serviceSteps = computed(() => {
  if (!order.value || !order.value.orderNo) return [];

  const steps = [];
  const status = order.value.orderStatus;
  const paymentStatus = order.value.paymentStatus;

  steps.push({ title: '订单创建', desc: '您已成功提交订单', time: order.value.createTime || formatDate(order.value.orderDate) });

  if (paymentStatus === 1) {
    steps.push({ title: '已支付', desc: '订单费用已支付', time: order.value.paymentTime || '' });
  }

  if (status >= 2) {
    steps.push({ title: '陪诊师已接单', desc: '陪诊师已接单，准备为您服务', time: '' });
  }

  if (status >= 3) {
    steps.push({ title: '服务开始', desc: '陪诊师已开始服务', time: formatDate(order.value.serviceStartTime) });
  }

  if (status >= 4) {
    steps.push({ title: '服务结束', desc: '陪诊师已结束服务', time: formatDate(order.value.serviceEndTime) });
  }

  if (status >= 5) {
    steps.push({ title: '待确认时长', desc: '请确认实际服务时长', time: '' });
  }

  if (status >= 6) {
    steps.push({ title: '订单完成', desc: '订单已完成', time: '' });
  }

  if (status === 7) {
    steps.push({ title: '订单取消', desc: '订单已被取消', time: '' });
  }

  return steps;
});

// 状态文本映射
const getOrderStatusText = (order) => {
  if (!order) return '未知状态';

  if (order.paymentStatus === 0 && order.orderStatus !== 7) {
    return '待支付';
  }

  const statusMap = {
    0: '待接单',
    1: '待接单',
    2: '待服务', // 修改：将“已接单”改为“待服务”
    3: '服务中',
    4: '待确认时长',
    5: '待支付差价',
    6: '已完成',
    7: '已取消'
  };
  return statusMap[order.orderStatus] || '未知状态';
};

const getStatusClass = (order) => {
  if (!order) return '';

  if (order.paymentStatus === 0 && order.orderStatus !== 7) {
    return 'status-deposit';
  }

  const classMap = {
    0: 'status-waiting',
    1: 'status-waiting',
    2: 'status-accepted',
    3: 'status-service',
    4: 'status-confirm',
    5: 'status-balance',
    6: 'status-completed',
    7: 'status-cancelled'
  };
  return classMap[order.orderStatus] || 'status-default';
};

// 服务类型映射
const getServiceTypeName = (type) => {
  const typeMap = {
    1: '普通陪诊',
    2: '术后护理',
    3: '急诊陪同',
    4: '上门陪诊'
  };
  return typeMap[type] || '未知类型';
};

// 格式化日期
const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleString('zh-CN');
};

// 格式化金额
const formatAmount = (amount) => {
  if (!amount) return '0.00';
  return Number(amount).toFixed(2);
};

// 获取头像URL
const getAvatarUrl = (avatarPath) => {
  if (!avatarPath) return '/static/default-avatar.jpg';
  if (avatarPath.startsWith('http')) return avatarPath;
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : config.baseURL + '/';
  const cleanUrl = avatarPath.startsWith('/') ? avatarPath.substring(1) : avatarPath;
  return baseUrl + cleanUrl;
};

// 图片加载错误处理
const handleImageError = (e) => {
  console.error('头像加载失败:', e);
};

// 返回订单列表
const returnToOrders = () => {
  uni.reLaunch({
    url: '/pages/order/order',
  });
};

// 联系陪诊师
const callCompanion = () => {
  if (order.value.attendantPhone) {
    uni.makePhoneCall({
      phoneNumber: order.value.attendantPhone,
    });
  }
};

// 咨询
const consult = () => {
  uni.showToast({
    title: '暂未开放咨询功能',
  });
};

// 分享
const share = () => {
  uni.showToast({
    title: '暂未开放分享功能',
  });
};

// 评价
const evaluate = () => {
  uni.showToast({
    title: '暂未开放评价功能',
  });
};

// 确认服务时长
const confirmDuration = async () => {
  try {
    const response = await post(`/orders/${order.value.orderId}/confirm-duration`, {
      userId: uni.getStorageSync('userId') // 从缓存获取用户ID
    });
    
    if (response.data === '确认成功') {
      uni.showToast({ title: '确认成功', icon: 'success' });
      await fetchOrderDetail(order.value.orderNo);
    } else {
      uni.showToast({ title: response.data || '确认失败', icon: 'none' });
    }
  } catch (error) {
    console.error('确认时长失败:', error);
    uni.showToast({ title: '确认失败', icon: 'none' });
  }
};

// 支付差价
const payBalance = async () => {
  try {
    const response = await post(`/orders/${order.value.orderId}/pay-balance`, {
      userId: uni.getStorageSync('userId') // 从缓存获取用户ID
    });
    
    if (response.data === '支付成功') {
      uni.showToast({ title: '支付成功', icon: 'success' });
      await fetchOrderDetail(order.value.orderNo);
    } else {
      uni.showToast({ title: response.data || '支付失败', icon: 'none' });
    }
  } catch (error) {
    console.error('支付差价失败:', error);
    uni.showToast({ title: '支付失败', icon: 'none' });
  }
};

/**
 * 获取症状描述文本
 */
const getSymptomDescription = () => {
  const { symptoms } = order.value;
  if (!symptoms) {
    return '无';
  }
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms.filter(s => s && s.trim() && s !== '无' && s !== 'null');
    return validSymptoms.length > 0 ? validSymptoms.join(', ') : '无';
  }
  return symptoms;
};

// 获取订单详情
const fetchOrderDetail = async (orderNo) => {
  try {
    console.log('正在获取订单详情，订单号:', orderNo);
    const response = await get(`/ai/guide/orders/${orderNo}/complete-info`);
    
    console.log('订单详情响应:', response);

    if (response && response.code === 200 && response.data) {
      order.value = response.data;

      // 如果订单状态已更新（>=2），停止轮询
      if (order.value.orderStatus >= 2 && pollTimer) {
          clearInterval(pollTimer);
          pollTimer = null;
      }
    } else {
      uni.showToast({ title: '获取订单详情失败', icon: 'none' });
    }
  } catch (error) {
    console.error('获取订单详情失败:', error);
    uni.showToast({ title: '网络错误', icon: 'none' });
  }
};

// WebSocket 消息处理
const handleSocketMessage = (message) => {
  console.log('收到 WebSocket 消息:', message);
  
  // 检查消息是否与当前订单相关
  const isCurrentOrder = message.orderId === order.value.orderId || 
                        message.orderNo === order.value.orderNo ||
                        (message.data && (message.data.orderId === order.value.orderId || message.data.orderNo === order.value.orderNo));
  
  // 处理各种订单状态变更消息
  if (isCurrentOrder && (message.type === 'ORDER_ACCEPTED' || 
                         message.type === 'SERVICE_STARTED' || 
                         message.type === 'SERVICE_COMPLETED' ||
                         message.type === 'ORDER_STATUS_CHANGED')) {
    
    console.log('收到当前订单状态更新消息，刷新详情');
    // 延迟一小段时间后再刷新，确保数据库已更新
    setTimeout(() => {
      fetchOrderDetail(order.value.orderNo);
    }, 1000);
  }
};

// 页面加载
onLoad(async (options) => {
  const orderNo = options.orderNo || options.orderId;

  console.log('页面加载参数:', options);
  console.log('解析出的订单号:', orderNo);

  if (!orderNo) {
    uni.showToast({ title: '订单号错误', icon: 'none' });
    setTimeout(() => {
        uni.reLaunch({ url: '/pages/order/order' });
    }, 1500);
    return;
  }

  await fetchOrderDetail(orderNo);

  // 添加 WebSocket 监听
  addSocketListener(handleSocketMessage);
  
  // 如果订单处于待接单状态，开启轮询作为 WebSocket 的备份
  if (order.value.orderStatus === 1) {
      pollTimer = setInterval(() => {
          fetchOrderDetail(orderNo);
      }, 5000); // 缩短到5秒
  }

  // 如果订单处于待接单状态，开启轮询作为 WebSocket 的备份
  if (order.value.orderStatus === 1) {
      pollTimer = setInterval(() => {
          fetchOrderDetail(orderNo);
      }, 3000);
  }
});

// 页面卸载
onUnmounted(() => {
    // 移除 WebSocket 监听
    removeSocketListener(handleSocketMessage);

    // 清除轮询定时器
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
});
</script>

<style scoped>
/* 状态标签样式 */
.status-deposit {
  background-color: #FFF3E0;
  color: #F57C00;
}
.status-waiting {
  background-color: #E3F2FD;
  color: #1976D2;
}
.status-accepted {
  background-color: #E8F5E8;
  color: #4CAF50;
}
.status-service {
  background-color: #F3E5F5;
  color: #9C27B0;
}
.status-confirm {
  background-color: #FFF8E1;
  color: #FF8F00;
}
.status-balance {
  background-color: #FFEBEE;
  color: #D32F2F;
}
.status-completed {
  background-color: #E0E0E0;
  color: #616161;
}
.status-cancelled {
  background-color: #FFEBEE;
  color: #D32F2F;
}

/* 邀请陪诊师按钮 */
.invite-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  text-align: center;
}

.invite-btn {
  width: 100%;
  height: 90rpx;
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
  border: none;
  border-radius: 45rpx;
  font-size: 32rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.invite-icon {
  font-size: 36rpx;
}

.invite-text {
  font-size: 32rpx;
}

/* 二维码区域 */
.qr-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}
.qr-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 24rpx;
  text-align: center;
}
.qr-container {
  text-align: center;
}
.qr-image {
  width: 300rpx;
  height: 300rpx;
  margin: 0 auto 20rpx;
}
.qr-desc {
  font-size: 26rpx;
  color: #666;
  text-align: center;
}

/* 时长确认区域 */
.duration-confirm-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}
.confirm-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 24rpx;
  text-align: center;
}
.duration-info {
  margin-bottom: 32rpx;
}
.info-text {
  font-size: 28rpx;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
  text-align: center;
}
.amount-text {
  font-size: 32rpx;
  color: #FF4D4F;
  font-weight: bold;
  display: block;
  text-align: center;
}
.confirm-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  color: white;
  border: none;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
}

/* 差价支付区域 */
.balance-payment-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}
.payment-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 24rpx;
  text-align: center;
}
.payment-amount {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 32rpx;
}
.amount-label {
  font-size: 28rpx;
  color: #666;
  margin-right: 16rpx;
}
.amount-price {
  font-size: 36rpx;
  color: #FF4D4F;
  font-weight: bold;
}
.pay-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #52C41A, #389E0D);
  color: white;
  border: none;
  border-radius: 40rpx;
  font-size: 32rpx;
  font-weight: bold;
}

/* 模拟按钮区域 */
.simulate-section {
  padding: 0 32rpx 20rpx 32rpx;
  margin-bottom: 20rpx;
}
.simulate-btn {
  width: 100%;
  height: 80rpx;
  background-color: #FF7F00;
  color: white;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(255, 127, 0, 0.2);
}

/* 保持原有样式 */
.container {
  padding: 0 32rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 160rpx;
}

.order-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 28rpx;
  color: #666;
  font-weight: bold;
  margin-bottom: 16rpx;
}

.status-tag {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.order-number {
  font-size: 26rpx;
  color: #333;
}

.service-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 20rpx;
}

.label {
  color: #666;
  width: 180rpx;
}

.value {
  color: #333;
  flex: 1;
  text-align: right;
}

.companion-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.companion-details {
  flex: 1;
}

.name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  display: block;
  margin-bottom: 8rpx;
}

.phone {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 8rpx;
}

.phone-icon {
  width: 24rpx;
  height: 24rpx;
}

.phone-number {
  font-size: 24rpx;
  color: #666;
}

.rating {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.star {
  font-size: 20rpx;
}

.score {
  font-size: 24rpx;
  color: #FFB300;
}

.payment-info {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.payment-row {
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  color: #333;
  margin-bottom: 20rpx;
}

.price {
  color: #FF4D4F;
  font-weight: bold;
}

.record-section {
  background: white;
  border-radius: 16rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
}

.record-title {
  font-size: 32rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 32rpx;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
}

.step-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background-color: #1890FF;
  margin-top: 8rpx;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  display: block;
  margin-bottom: 8rpx;
}

.step-desc {
  font-size: 24rpx;
  color: #666;
  line-height: 1.4;
}

.step-time {
  font-size: 24rpx;
  color: #999;
  text-align: right;
  width: 160rpx;
  flex-shrink: 0;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-top: 1rpx solid #eee;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 120rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  z-index: 999;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  color: #1890FF;
}

.nav-icon {
  width: 40rpx;
  height: 40rpx;
}

.return-to-orders-section {
  padding: 40rpx 32rpx 20rpx 32rpx;
}

.return-to-orders-btn {
  width: 100%;
  height: 80rpx;
  background-color: #1890FF;
  color: white;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(24, 144, 255, 0.2);
}
</style>