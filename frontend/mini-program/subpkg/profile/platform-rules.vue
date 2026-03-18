<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/escort-rules.svg" mode="aspectFit"></image>
        <view>
          <text class="hero-title">平台规则</text>
          <text class="hero-desc">定价与定金规则清晰透明，保障双方权益</text>
        </view>
      </view>
    </view>

    <view class="rule-card slide-up delay-2">
      <text class="card-title">定价规则</text>
      <text class="rule-line">普通陪诊：50元/2小时，超时30元/小时</text>
      <text class="rule-line">术后护理：白天45元/小时，夜间60元/小时</text>
      <text class="rule-line">急诊陪同：加急费白天100元，夜间150元+时长费</text>
      <text class="rule-line no-gap">上门陪诊：里程费3公里免费，超出5元/公里</text>
    </view>

    <view class="rule-card slide-up delay-3">
      <text class="card-title">定金扣除规则</text>
      <text class="rule-line">服务开始前2小时以上取消：全额退款</text>
      <text class="rule-line">1-2小时取消：扣除20%定金</text>
      <text class="rule-line">30-60分钟取消：扣除30%定金</text>
      <text class="rule-line">30分钟内取消：扣除50%定金</text>
      <text class="rule-line no-gap">服务开始后取消：扣除100%定金</text>
    </view>
  </view>
</template>

<script setup>
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding-bottom: 24rpx;
}

.hero-card {
  @include escort-card(26rpx);
  margin: 24rpx 24rpx 0;
  border: 1rpx solid #e5eefb;
  background: linear-gradient(135deg, #ffffff 0%, #f3f8ff 100%);
}

.hero-left {
  display: flex;
  align-items: center;
}

.hero-icon {
  width: 56rpx;
  height: 56rpx;
  margin-right: 14rpx;
}

.hero-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.rule-card {
  @include escort-card(32rpx);
  margin: 24rpx;
}

.card-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 20rpx;
}

.rule-line {
  display: block;
  color: #666666;
  font-size: 28rpx;
  line-height: 1.8;
  margin-bottom: 12rpx;
}

.no-gap {
  margin-bottom: 0;
}

.slide-up {
  opacity: 0;
  transform: translateY(20rpx);
  animation: slideUp 0.42s ease forwards;
}

.delay-1 {
  animation-delay: 0.02s;
}

.delay-2 {
  animation-delay: 0.08s;
}

.delay-3 {
  animation-delay: 0.14s;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
