<template>
  <view class="escort-bottom-bar">
    <view class="bar-item" :class="{ active: active === 'hall' }" @click="go('/pages/role-escort/hall')">
      <image class="icon" :src="active === 'hall' ? '/static/shouye_active.png' : '/static/shouye.png'" mode="aspectFit" />
      <text class="text">接单厅</text>
    </view>
    <view class="bar-item" :class="{ active: active === 'order' }" @click="go('/pages/role-escort/order')">
      <image class="icon" :src="active === 'order' ? '/static/order_active.png' : '/static/order.png'" mode="aspectFit" />
      <text class="text">订单</text>
    </view>
    <view class="bar-item" :class="{ active: active === 'message' }" @click="go('/pages/role-escort/message')">
      <image class="icon" :src="active === 'message' ? '/static/xiaoxi_2_active.png' : '/static/xiaoxi_2.png'" mode="aspectFit" />
      <text class="text">消息</text>
    </view>
    <view class="bar-item" :class="{ active: active === 'profile' }" @click="go('/pages/role-escort/profile')">
      <image class="icon" :src="active === 'profile' ? '/static/wode_2_active.png' : '/static/wode_2.png'" mode="aspectFit" />
      <text class="text">我的</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  active: { type: String, default: 'hall' }
})

const go = (url) => {
  // 陪诊师端不在微信 TabBar 内，统一使用 reLaunch 保证底部栏一致
  uni.reLaunch({ url })
}
</script>

<style scoped>
.escort-bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 120rpx;
  background: #ffffff;
  border-top: 1rpx solid #eef2f7;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: border-box;
  z-index: 999;
}
.bar-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  color: #8a94a6;
}
.bar-item.active {
  color: #4A90E2;
}
.icon {
  width: 48rpx;
  height: 48rpx;
}
.text {
  font-size: 22rpx;
}
</style>

