<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/escort-service-center.svg" mode="aspectFit"></image>
        <view>
          <text class="hero-title">客服中心</text>
          <text class="hero-desc">服务问题可快速联系平台处理</text>
        </view>
      </view>
    </view>

    <view class="card slide-up delay-2">
      <text class="title">联系信息</text>
      <view class="info-row">
        <text class="label">服务时间</text>
        <text class="value">09:00 - 21:00</text>
      </view>
      <view class="info-row">
        <text class="label">客服热线</text>
        <text class="value">400-800-2026</text>
      </view>
      <view class="info-row">
        <text class="label">客服邮箱</text>
        <text class="value">support@companion.local</text>
      </view>
    </view>

    <view class="btn primary slide-up delay-3" @click="callService">
      <text>拨打客服电话</text>
    </view>

    <view class="btn slide-up delay-3" @click="goMessage">
      <text>前往消息中心</text>
    </view>
  </view>
</template>

<script setup>
import { makePhoneCallWithGuard } from '@/subpkg/common/runtime.js'

const callService = () => {
  makePhoneCallWithGuard('4008002026')
}

const goMessage = () => {
  uni.reLaunch({ url: '/pages/role-escort/message' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.page {
  @include escort-page;
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.hero-card {
  @include escort-card(26rpx);
  margin-bottom: 18rpx;
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

.card {
  @include escort-card(24rpx);
  margin-bottom: 18rpx;
}

.title {
  display: block;
  font-size: 32rpx;
  color: #111827;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.info-row {
  min-height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #eef2f7;
}

.info-row:last-child {
  border-bottom: none;
}

.label {
  font-size: 25rpx;
  color: #6b7280;
}

.value {
  font-size: 25rpx;
  color: #1f2937;
}

.btn {
  height: $escort-btn-height;
  border-radius: 14rpx;
  background: #fff;
  border: 1rpx solid #e7edf5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14rpx;

  text {
    color: #1f2937;
    font-size: 28rpx;
    font-weight: 600;
  }
}

.primary {
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  border: none;
  box-shadow: $escort-shadow-primary;

  text {
    color: #fff;
  }
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
