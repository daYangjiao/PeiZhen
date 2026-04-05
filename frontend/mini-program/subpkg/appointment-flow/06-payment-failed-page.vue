<template>
  <view class="payment-failed-page">
    <view class="card failed-card">
      <view class="icon failed">
        <text class="icon-check">×</text>
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
    }
  },

  onLoad(options) {
    if (options.orderNo) {
      this.orderNo = decodeURIComponent(options.orderNo)
    }
    if (options.scene) {
      this.scene = options.scene
    }
  },

  methods: {
    retryPayment() {
      if (!this.orderNo) {
        uni.showToast({
          title: '缺少订单号',
          icon: 'none'
        })
        return
      }

      if (this.scene === 'balance') {
        uni.redirectTo({
          url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(this.orderNo)}`
        })
      } else {
        uni.redirectTo({
          url: `/subpkg/appointment-flow/04-order-confirm-page?orderNo=${encodeURIComponent(this.orderNo)}`
        })
      }
    },

    goToOrderDetail() {
      if (this.orderNo) {
        uni.redirectTo({
          url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(this.orderNo)}`
        })
      } else {
        uni.showToast({
          title: '无法跳转：缺少订单号',
          icon: 'none'
        })
      }
    },

    goHome() {
      uni.reLaunch({
        url: '/pages/role-user/home'
      })
    }
  }
}
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
  @include user-card(40rpx 28rpx);
  width: 100%;
}

.failed-card {
  text-align: center;
}

.icon {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 28rpx;
}

.icon.failed {
  background: linear-gradient(135deg, #ff6a6a 0%, #ef4444 100%);
  box-shadow: 0 18rpx 36rpx rgba(239, 68, 68, 0.2);
}

.icon-check {
  font-size: 56rpx;
  line-height: 1;
  color: #ffffff;
  font-weight: 700;
}

.main-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: $user-color-text-main;
  margin-bottom: 18rpx;
}

.desc {
  display: block;
  font-size: 24rpx;
  line-height: 1.7;
  color: $user-color-text-sub;
  margin-bottom: 40rpx;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.retry-btn,
.view-order-btn,
.home-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.retry-btn::after,
.view-order-btn::after,
.home-btn::after {
  border: none;
}

.retry-btn {
  @include user-primary-btn;
  border: none;
}

.view-order-btn {
  @include user-ghost-btn(88rpx);
}

.home-btn {
  background: #f6faff;
  color: $user-color-text-sub;
  border: 1rpx solid rgba(220, 232, 248, 0.92);
}
</style>
