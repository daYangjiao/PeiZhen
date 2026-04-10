<template>
  <view class="escort-bottom-bar">
    <view class="bar-inner">
      <view class="bar-item" @click="go('/pages/role-escort/hall')">
        <view class="icon-wrap">
          <image class="icon" :src="active === 'hall' ? tabIcons.hall.active : tabIcons.hall.default" mode="aspectFit" />
        </view>
        <text class="text" :class="{ active: active === 'hall' }">接单厅</text>
      </view>
      <view class="bar-item" @click="go('/pages/role-escort/order')">
        <view class="icon-wrap">
          <image class="icon" :src="active === 'order' ? tabIcons.order.active : tabIcons.order.default" mode="aspectFit" />
        </view>
        <text class="text" :class="{ active: active === 'order' }">订单</text>
      </view>
      <view class="bar-item" @click="go('/pages/role-escort/message')">
        <view class="icon-wrap">
          <image class="icon" :src="active === 'message' ? tabIcons.message.active : tabIcons.message.default" mode="aspectFit" />
          <view class="message-badge" v-if="messageUnread > 0">
            <text>{{ messageUnread > 99 ? '99+' : messageUnread }}</text>
          </view>
        </view>
        <text class="text" :class="{ active: active === 'message' }">消息</text>
      </view>
      <view class="bar-item" @click="go('/pages/role-escort/profile')">
        <view class="icon-wrap">
          <image class="icon" :src="active === 'profile' ? tabIcons.profile.active : tabIcons.profile.default" mode="aspectFit" />
        </view>
        <text class="text" :class="{ active: active === 'profile' }">我的</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useMessageStore } from '@/stores/message.js'
import { escortTabIcons } from '@/utils/assets.js'

defineProps({
  active: { type: String, default: 'hall' }
})

const messageStore = useMessageStore()
const messageUnread = computed(() => messageStore.totalUnreadCount)
const tabIcons = escortTabIcons

const refreshUnreadBadge = () => {
  messageStore.scheduleRefreshUnreadCounts(120)
}

onMounted(() => {
  refreshUnreadBadge()
  uni.$on('chat:message', refreshUnreadBadge)
  uni.$on('session:changed', refreshUnreadBadge)
})

onUnmounted(() => {
  uni.$off('chat:message', refreshUnreadBadge)
  uni.$off('session:changed', refreshUnreadBadge)
})

const go = (url) => {
  // 陪诊师端不在微信 TabBar 内，统一使用 reLaunch 保证底部栏一致
  uni.reLaunch({ url })
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
.escort-bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(50px + env(safe-area-inset-bottom));
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: border-box;
  z-index: 999;
}

.bar-inner {
  height: 50px;
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 2px;
}

.icon-wrap {
  width: 24px;
  height: 24px;
  position: relative;
}

.icon {
  width: 24px;
  height: 24px;
}

.message-badge {
  position: absolute;
  min-width: 28rpx;
  height: 28rpx;
  line-height: 28rpx;
  padding: 0 8rpx;
  box-sizing: border-box;
  border-radius: 999rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  text-align: center;
  top: -10rpx;
  right: -14rpx;
  border: 2rpx solid #fff;
}

.text {
  font-size: 10px;
  color: #999999;
  line-height: 1.1;
}

.text.active {
  color: $escort-color-primary;
}
</style>
