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

    <view v-if="dialog.visible" class="dialog-mask" @click="closeDialog">
      <view class="dialog-panel" @click.stop>
        <text class="dialog-title">{{ dialog.title }}</text>
        <text class="dialog-desc">{{ dialog.content }}</text>
        <view class="dialog-actions" :class="{ single: !dialog.showCancel }">
          <button v-if="dialog.showCancel" class="dialog-btn secondary" @click="closeDialog">取消</button>
          <button class="dialog-btn primary" @click="confirmDialog">{{ dialog.confirmText }}</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const notifyOn = ref(true)
const dialog = ref({
  visible: false,
  title: '',
  content: '',
  confirmText: '确定',
  showCancel: false,
  action: ''
})

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
  dialog.value = {
    visible: true,
    title: '清理本地缓存',
    content: '仅清理本地缓存，不影响服务器数据。',
    confirmText: '确认清理',
    showCancel: true,
    action: 'clear'
  }
}

const aboutApp = () => {
  dialog.value = {
    visible: true,
    title: '关于平台',
    content: '陪诊服务平台 v1.0.0\n为患者与陪诊师提供安全、高效的连接服务。',
    confirmText: '我知道了',
    showCancel: false,
    action: ''
  }
}

const closeDialog = () => {
  dialog.value.visible = false
}

const confirmDialog = () => {
  if (dialog.value.action === 'clear') {
    uni.removeStorageSync('attendantInfo')
    uni.showToast({ title: '已清理', icon: 'success' })
  }
  closeDialog()
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

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 32rpx 28rpx calc(32rpx + env(safe-area-inset-bottom));
  background: rgba(15, 23, 42, 0.36);
  box-sizing: border-box;
}

.dialog-panel {
  width: 100%;
  background: #fff;
  border-radius: 34rpx;
  padding: 34rpx 28rpx 28rpx;
  box-shadow: 0 24rpx 70rpx rgba(15, 23, 42, 0.18);
  box-sizing: border-box;
}

.dialog-title {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: $escort-color-text-main;
}

.dialog-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.65;
  color: $escort-color-text-sub;
  white-space: pre-line;
}

.dialog-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 28rpx;
}

.dialog-actions.single {
  grid-template-columns: 1fr;
}

.dialog-btn {
  height: 88rpx;
  border-radius: 26rpx;
  font-size: 28rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.dialog-btn::after {
  border: none;
}

.dialog-btn.secondary {
  background: #eef4ff;
  color: #4b6388;
}

.dialog-btn.primary {
  background: linear-gradient(135deg, #2f7cff 0%, #63adff 100%);
  color: #fff;
}
</style>
