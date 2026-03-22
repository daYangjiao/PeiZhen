<template>
  <view class="page">
    <view class="card row">
      <text class="label">消息提醒</text>
      <switch :checked="notifyOn" color="#007AFF" @change="onNotifyChange" />
    </view>

    <view class="card row" @click="clearLocalCache">
      <text class="label">清理本地缓存</text>
      <text class="arrow">›</text>
    </view>

    <view class="card row" @click="aboutApp">
      <text class="label">关于平台</text>
      <text class="arrow">›</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const notifyOn = ref(true)

onMounted(() => {
  const saved = uni.getStorageSync('escort_notify_on')
  notifyOn.value = saved === '' || saved === undefined ? true : !!saved
})

const onNotifyChange = (e) => {
  notifyOn.value = !!e.detail.value
  uni.setStorageSync('escort_notify_on', notifyOn.value)
  uni.showToast({ title: notifyOn.value ? '已开启提醒' : '已关闭提醒', icon: 'none' })
}

const clearLocalCache = () => {
  uni.showModal({
    title: '确认清理',
    content: '仅清理本地缓存，不影响服务器数据。',
    success: (res) => {
      if (!res.confirm) return
      uni.removeStorageSync('attendantInfo')
      uni.showToast({ title: '已清理', icon: 'success' })
    }
  })
}

const aboutApp = () => {
  uni.showModal({
    title: '关于平台',
    content: '陪诊服务平台 v1.0.0\\n为患者与陪诊师提供安全、高效的连接服务。',
    showCancel: false
  })
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

.card {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 0 24rpx;
  margin-bottom: 14rpx;
  box-shadow: $escort-shadow-card;
}

.row {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.label {
  font-size: 29rpx;
  color: #1f2937;
}

.arrow {
  color: #c0c4cc;
  font-size: 34rpx;
}
</style>
