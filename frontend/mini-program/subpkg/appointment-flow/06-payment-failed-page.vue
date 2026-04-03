<template>
  <view class="payment-failed-page">
    <view class="failed-hero-card">
      <view class="failed-hero-icon">
        <text class="failed-hero-icon-text">!</text>
      </view>
      <text class="failed-hero-title">支付未完成</text>
      <text class="failed-hero-desc">订单尚未支付</text>
    </view>

    <view class="card failed-status-card">
      <text class="status-card-label">订单编号</text>
      <text class="status-card-value">{{ orderNo || '—' }}</text>
    </view>

    <view class="failed-action-card">
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
  padding: 28rpx 24rpx calc(172rpx + env(safe-area-inset-bottom));
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
  line-height: 1.4;
  display: block;
}

.failed-status-card {
  padding: 24rpx;
  margin-bottom: 24rpx;
}

.status-card-label {
  display: block;
  font-size: 22rpx;
  color: $user-color-text-sub;
  margin-bottom: 12rpx;
}

.status-card-value {
  display: block;
  font-size: 26rpx;
  line-height: 1.6;
  color: $user-color-text-main;
  font-weight: 700;
  word-break: break-all;
}

.failed-action-card {
  position: sticky;
  bottom: 0;
  z-index: 20;
  padding: 20rpx 0 calc(8rpx + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(244, 248, 255, 0) 0%, rgba(244, 248, 255, 0.96) 28%, rgba(244, 248, 255, 1) 100%);
  display: flex;
  flex-direction: column;
  gap: 18rpx;
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
