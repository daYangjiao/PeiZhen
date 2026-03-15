<template>
  <view class="payment-failed-page">
    <!-- 支付失败卡片 -->
    <view class="card failed-card">
      <view class="icon">
        <text class="icon-check">×</text>
      </view>
      <text class="main-title">支付失败!</text>
      <text class="desc">您的订单支付失败，请确认订单后再试，或更换其它支付方式。</text>
      
      <!-- 按钮组 -->
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
          url: `/subpkg/appointment-flow/04_OrderConfirmPage?orderNo=${encodeURIComponent(this.orderNo)}`
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

<style scoped>
.payment-failed-page {  
  background-color: #f5f5f5;
  padding: 40rpx;
  font-size: 28rpx;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {  
  background-color: white;
  border-radius: 16rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.08);
  width: 100%;
  box-sizing: border-box;
}

.failed-card {   
  text-align: center;
}

.icon {  
  width: 100rpx;
  height: 100rpx;
  line-height: 100rpx;
  border-radius: 50%;
  background-color: #ff4d4f;
  color: white;
  font-size: 60rpx;
  margin: 0 auto 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-title {  
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.desc {  
  color: #666;
  font-size: 28rpx;
  line-height: 1.6;
  margin-bottom: 60rpx;
  display: block;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.retry-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background-color: #007AFF;
  color: white;
  border-radius: 40rpx;
  font-size: 30rpx;
  border: none;
}

.view-order-btn {    
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background-color: white;
  color: #007AFF;
  border: 2rpx solid #007AFF;
  border-radius: 40rpx;
  font-size: 30rpx;
}

.home-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  background-color: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 40rpx;
  font-size: 30rpx;
}
</style>