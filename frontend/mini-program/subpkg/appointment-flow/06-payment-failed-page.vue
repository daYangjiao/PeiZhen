<template>
  <view class="payment-failed-page">
    <view class="card failed-card">
      <view class="icon-wrap failed">
        <text class="icon-symbol">×</text>
      </view>
      <text class="main-title">支付失败!</text>
      <text class="desc">您的订单支付失败，请确认订单后再试，或更换其它支付方式。</text>

      <view class="btn-group">
        <button class="retry-btn" @click="retryPayment">重新支付</button>
        <button class="view-order-btn" @click="goToOrderDetail">查看订单详情</button>
        <button class="home-btn" @click="goHome">返回首页</button>
      </view>
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
    retryPayment() {
      if (!this.orderNo) {
        uni.showToast({
          title: '缺少订单号',
          icon: 'none'
        });
        return;
      }

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
  min-height: 100vh;
  padding: 40rpx 24rpx calc(44rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  align-items: center;
}

.card {
  @include user-card(36rpx 28rpx);
  width: 100%;
}

.failed-card {
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #fff7f7 100%);
}

.icon-wrap {
  width: 108rpx;
  height: 108rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24rpx;
}

.icon-wrap.failed {
  background: linear-gradient(135deg, #ff6a6a 0%, #ef4444 100%);
  box-shadow: 0 18rpx 36rpx rgba(239, 68, 68, 0.2);
}

.icon-symbol {
  font-size: 56rpx;
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
  margin-bottom: 16rpx;
}

.desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: $user-color-text-sub;
  margin-bottom: 34rpx;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.retry-btn {
  width: 100%;
  @include user-primary-btn;
  border: none;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
}

.view-order-btn {
  width: 100%;
  @include user-ghost-btn(88rpx);
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
}

.home-btn {
  width: 100%;
  height: 72rpx;
  line-height: 72rpx;
  background: transparent;
  border: none;
  color: $user-color-text-sub;
  font-size: 24rpx;
  font-weight: 600;
  border-radius: 999rpx;
}
</style>
