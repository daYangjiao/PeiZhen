<template>
  <view class="payment-failed-page">
    <view class="failed-hero-card">
      <view class="failed-hero-icon">
        <text class="failed-hero-icon-text">!</text>
      </view>
      <text class="failed-hero-title">支付未完成</text>
      <text class="failed-hero-desc">这笔订单还没有完成支付。你可以立即重新支付，或者先进入订单详情稍后继续处理。</text>
    </view>

    <view class="card failed-reason-card">
      <text class="section-eyebrow">处理建议</text>
      <text class="section-title">你现在可以这样做</text>
      <view class="reason-list">
        <view class="reason-item">
          <view class="reason-dot"></view>
          <text class="reason-text">重新发起支付，继续完成当前订单锁单流程。</text>
        </view>
        <view class="reason-item">
          <view class="reason-dot"></view>
          <text class="reason-text">如果暂时不支付，可进入订单详情稍后继续处理。</text>
        </view>
        <view class="reason-item">
          <view class="reason-dot"></view>
          <text class="reason-text">若本次失败发生在补付差价阶段，重新支付会回到对应订单继续补付。</text>
        </view>
      </view>
    </view>

    <view class="card failed-action-card">
      <button class="retry-btn" @click="retryPayment">重新支付</button>
      <button class="view-order-btn" @click="goToOrderDetail">查看订单详情</button>
      <button class="home-btn" @click="goHome">返回首页</button>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      orderNo: '',
      scene: 'normal'
    };
  },

  onLoad(options) {
    if (options.orderNo) {
      this.orderNo = decodeURIComponent(options.orderNo);
    }
    if (options.scene) {
      this.scene = options.scene;
    }
  },

  methods: {
    // 重新支付
    retryPayment() {
      if (!this.orderNo) {
        uni.showToast({
          title: '缺少订单号',
          icon: 'none'
        });
        return;
      }

      // 补付失败：回到订单详情页继续补付；首付款失败：回订单确认页
      if (this.scene === 'balance') {
        uni.redirectTo({
          url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(this.orderNo)}`
        });
      } else {
        uni.redirectTo({
          url: `/subpkg/appointment-flow/04-order-confirm-page?orderNo=${encodeURIComponent(this.orderNo)}`
        });
      }
    },

    // 跳转到订单详情页
    goToOrderDetail() {
      if (this.orderNo) {
        uni.redirectTo({
          url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(this.orderNo)}`
        });
      } else {
        uni.showToast({
          title: '无法跳转：缺少订单号',
          icon: 'none'
        });
      }
    },

    // 返回首页
    goHome() {
      uni.reLaunch({
        url: '/pages/role-user/home'
      });
    }
  }
};
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.payment-failed-page {  
  @include user-page;
  padding: 28rpx 24rpx calc(40rpx + env(safe-area-inset-bottom));
  font-size: 28rpx;
  min-height: 100vh;
  box-sizing: border-box;
}

.failed-hero-card,
.card {  
  @include user-card(28rpx);
}

.failed-hero-card {   
  text-align: center;
  padding: 40rpx 30rpx 34rpx;
  margin-bottom: 24rpx;
  background: linear-gradient(180deg, #ffffff 0%, #fff6f6 100%);
}

.failed-hero-icon {  
  width: 112rpx;
  height: 112rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #ff7a7a 0%, #ef4444 100%);
  color: white;
  font-size: 60rpx;
  margin: 0 auto 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 18rpx 36rpx rgba(239, 68, 68, 0.22);
}

.failed-hero-icon-text {
  font-size: 58rpx;
  line-height: 1;
  color: #ffffff;
  font-weight: 700;
}

.failed-hero-title {  
  font-size: 42rpx;
  font-weight: 700;
  color: $user-color-text-main;
  display: block;
  margin-bottom: 14rpx;
}

.failed-hero-desc {  
  color: $user-color-text-sub;
  font-size: 24rpx;
  line-height: 1.75;
  display: block;
}

.section-eyebrow {
  display: block;
  font-size: 22rpx;
  line-height: 1;
  color: #ef4444;
  font-weight: 700;
  letter-spacing: 2rpx;
  margin-bottom: 12rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  line-height: 1.2;
  color: $user-color-text-main;
  font-weight: 700;
  margin-bottom: 22rpx;
}

.failed-reason-card,
.failed-action-card {
  margin-bottom: 24rpx;
}

.reason-list,
.failed-action-card {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.reason-item {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.reason-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #ef4444;
  margin-top: 10rpx;
  flex-shrink: 0;
}

.reason-text {
  font-size: 24rpx;
  line-height: 1.72;
  color: $user-color-text-main;
}

.retry-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #007aff 0%, #2563eb 100%);
  color: white;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  border: none;
  box-shadow: 0 16rpx 30rpx rgba(37, 99, 235, 0.24);
}

.view-order-btn {    
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background-color: white;
  color: #007AFF;
  border: 1rpx solid rgba(0, 122, 255, 0.22);
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.home-btn {
  width: 100%;
  height: 72rpx;
  line-height: 72rpx;
  background-color: transparent;
  color: $user-color-text-sub;
  border: none;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 600;
}
</style>
