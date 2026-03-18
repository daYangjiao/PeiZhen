<template>
  <view class="redirect-page">
    <view class="loading-card">
      <image class="loading-icon" src="/static/escort-withdraw.svg" mode="aspectFit"></image>
      <text class="loading-text">正在跳转到钱包明细...</text>
    </view>
  </view>
</template>

<script setup>
import { onLoad } from '@dcloudio/uni-app'

const redirectToWallet = () => {
  uni.redirectTo({
    url: '/subpkg/profile/wallet-detail?tab=withdraw',
    fail: () => {
      uni.navigateTo({ url: '/subpkg/profile/wallet-detail?tab=withdraw' })
    }
  })
}

onLoad(() => {
  redirectToWallet()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.redirect-page {
  @include escort-page;
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.loading-card {
  @include escort-card(32rpx);
  margin-top: 24rpx;
  display: flex;
  align-items: center;
}

.loading-icon {
  width: 56rpx;
  height: 56rpx;
  margin-right: 14rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #4b5563;
}
</style>
