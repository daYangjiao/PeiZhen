<template>
  <view v-if="visible" class="qualification-modal-mask" @click="close">
    <view class="qualification-modal" :class="`state-${gate.state}`" @click.stop>
      <view class="modal-handle"></view>
      <view class="modal-head">
        <view class="modal-mark">{{ markText }}</view>
        <view class="modal-title-wrap">
          <view class="modal-kicker">接单门禁</view>
          <view class="modal-title">{{ gate.title || '资质状态提醒' }}</view>
        </view>
      </view>
      <view class="modal-message">{{ gate.message || '请先完善资质后再继续操作。' }}</view>
      <view class="modal-actions">
        <button class="modal-btn secondary" @click="close">稍后</button>
        <button class="modal-btn primary" @click="confirm">{{ primaryText }}</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'

const visible = ref(false)
const redirectOnConfirm = ref(true)
const gate = reactive({
  state: 'incomplete',
  title: '',
  message: ''
})

const markText = computed(() => {
  if (gate.state === 'blocked') return '停'
  if (gate.state === 'expired') return '期'
  if (gate.state === 'rejected') return '驳'
  return '审'
})

const primaryText = computed(() => (gate.state === 'blocked' ? '我知道了' : '去资质管理'))

const open = (payload = {}) => {
  const nextGate = payload.gate || payload
  gate.state = nextGate.state || 'incomplete'
  gate.title = nextGate.title || '资质状态提醒'
  gate.message = nextGate.message || '请先完善资质后再继续操作。'
  redirectOnConfirm.value = payload.redirectOnConfirm !== false
  visible.value = true
}

const close = () => {
  visible.value = false
}

const confirm = () => {
  const shouldRedirect = redirectOnConfirm.value && gate.state !== 'blocked'
  close()
  if (shouldRedirect) {
    uni.navigateTo({ url: '/subpkg/profile/qualification' })
  }
}

onMounted(() => {
  uni.$on('escort-qualification-gate:show', open)
})

onUnmounted(() => {
  uni.$off('escort-qualification-gate:show', open)
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.qualification-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2200;
  background: rgba(17, 24, 39, 0.44);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 28rpx 28rpx calc(32rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.qualification-modal {
  width: 100%;
  max-width: 680rpx;
  max-height: 86vh;
  padding: 18rpx 34rpx 32rpx;
  border-radius: 38rpx;
  background: #ffffff;
  border: 1rpx solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 30rpx 90rpx rgba(25, 66, 128, 0.22);
  box-sizing: border-box;
  animation: qualificationModalIn 180ms ease-out;
}

.modal-handle {
  width: 76rpx;
  height: 8rpx;
  margin: 0 auto 28rpx;
  border-radius: 999rpx;
  background: #d7e5f5;
}

.modal-head {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.modal-mark {
  width: 84rpx;
  height: 84rpx;
  flex: 0 0 84rpx;
  border-radius: 32rpx;
  background: #eef6ff;
  color: $escort-color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
  font-weight: 800;
}

.modal-title-wrap {
  min-width: 0;
  flex: 1;
}

.modal-kicker {
  font-size: 23rpx;
  color: #6b7a90;
  margin-bottom: 6rpx;
  font-weight: 700;
}

.state-rejected .modal-mark,
.state-expired .modal-mark,
.state-incomplete .modal-mark {
  background: #fff3f0;
  color: #ef4444;
}

.state-blocked .modal-mark {
  background: #f3f4f6;
  color: #4b5563;
}

.modal-title {
  text-align: left;
  font-size: 36rpx;
  line-height: 1.3;
  color: #172033;
  font-weight: 800;
}

.modal-message {
  margin-top: 24rpx;
  padding: 22rpx 24rpx;
  border-radius: 26rpx;
  background: #f7faff;
  color: #53627a;
  font-size: 27rpx;
  line-height: 1.65;
  text-align: left;
}

.modal-actions {
  margin-top: 30rpx;
  display: grid;
  grid-template-columns: 1fr 1.25fr;
  gap: 18rpx;
}

.modal-btn {
  height: 84rpx;
  line-height: normal;
  border: none;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
  padding: 0 18rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  white-space: nowrap;
}

.modal-btn::after {
  border: none;
}

.modal-btn.secondary {
  background: #f2f6fb;
  color: #53627a;
}

.modal-btn.primary {
  background: linear-gradient(135deg, #1777ff, #0f9ed8);
  color: #ffffff;
  box-shadow: 0 14rpx 30rpx rgba(23, 119, 255, 0.22);
}

@keyframes qualificationModalIn {
  from {
    opacity: 0;
    transform: translateY(60rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
