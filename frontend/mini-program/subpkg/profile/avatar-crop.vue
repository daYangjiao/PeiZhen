<template>
  <view class="page">
    <view class="hero">
      <text class="title">裁剪头像</text>
      <text class="tip">请拖动并缩放图片，裁剪为正方形头像</text>
    </view>

    <view class="crop-shell">
      <view
        class="crop-stage"
        :style="{ width: `${cropBoxSize}px`, height: `${cropBoxSize}px` }"
        @touchstart.stop.prevent="handleTouchStart"
        @touchmove.stop.prevent="handleTouchMove"
        @touchend.stop.prevent="handleTouchEnd"
        @touchcancel.stop.prevent="handleTouchEnd"
      >
        <image
          v-if="sourcePath && imageReady"
          class="crop-image"
          :src="sourcePath"
          mode="scaleToFill"
          :style="imageStyle"
          draggable="false"
        ></image>
        <view class="crop-mask"></view>
        <view class="crop-frame"></view>
      </view>
    </view>

    <view class="hint-row">
      <text class="hint-text">头像将以正方形保存，便于在聊天和订单中保持正常显示</text>
    </view>

    <view class="action-row">
      <button class="ghost-btn" @click="handleCancel">取消</button>
      <button class="primary-btn" :disabled="processing || !imageReady" @click="handleConfirm">
        {{ processing ? '生成中...' : '完成' }}
      </button>
    </view>

    <canvas
      canvas-id="avatarCropCanvas"
      id="avatarCropCanvas"
      class="hidden-canvas"
      :style="{ width: `${canvasOutputSize}px`, height: `${canvasOutputSize}px` }"
    ></canvas>
  </view>
</template>

<script setup>
import { computed, getCurrentInstance, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { consumeAvatarCropTask, rejectAvatarCropTask, resolveAvatarCropTask } from '@/utils/avatar-upload.js'

const instance = getCurrentInstance()
const sourcePath = ref('')
const processing = ref(false)
const imageReady = ref(false)

const imageWidth = ref(0)
const imageHeight = ref(0)
const baseScale = ref(1)
const userScale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)

const cropBoxSize = ref(0)
const canvasOutputSize = 720

const touchMode = ref('')
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragOriginX = ref(0)
const dragOriginY = ref(0)
const pinchStartDistance = ref(0)
const pinchStartScale = ref(1)
const settled = ref(false)

const imageStyle = computed(() => {
  const width = imageWidth.value * baseScale.value * userScale.value
  const height = imageHeight.value * baseScale.value * userScale.value
  return {
    width: `${width}px`,
    height: `${height}px`,
    transform: `translate(${offsetX.value}px, ${offsetY.value}px)`
  }
})

const rpxToPx = (rpx) => {
  const { windowWidth } = uni.getSystemInfoSync()
  return (Number(rpx) * windowWidth) / 750
}

const getDisplaySize = () => ({
  width: imageWidth.value * baseScale.value * userScale.value,
  height: imageHeight.value * baseScale.value * userScale.value
})

const clampOffsets = () => {
  const { width, height } = getDisplaySize()
  const maxOffsetX = Math.max(0, (width - cropBoxSize.value) / 2)
  const maxOffsetY = Math.max(0, (height - cropBoxSize.value) / 2)
  offsetX.value = Math.min(maxOffsetX, Math.max(-maxOffsetX, offsetX.value))
  offsetY.value = Math.min(maxOffsetY, Math.max(-maxOffsetY, offsetY.value))
}

const initializeTransform = () => {
  baseScale.value = Math.max(cropBoxSize.value / imageWidth.value, cropBoxSize.value / imageHeight.value)
  userScale.value = 1
  offsetX.value = 0
  offsetY.value = 0
  clampOffsets()
}

const normalizeDistance = (touches = []) => {
  if (!touches || touches.length < 2) return 0
  const [first, second] = touches
  const dx = second.clientX - first.clientX
  const dy = second.clientY - first.clientY
  return Math.sqrt(dx * dx + dy * dy)
}

const handleTouchStart = (event) => {
  if (processing.value || !imageReady.value) return
  const touches = event.touches || []
  if (touches.length >= 2) {
    touchMode.value = 'pinch'
    pinchStartDistance.value = normalizeDistance(touches)
    pinchStartScale.value = userScale.value
    return
  }

  const touch = touches[0]
  if (!touch) return
  touchMode.value = 'drag'
  dragStartX.value = touch.clientX
  dragStartY.value = touch.clientY
  dragOriginX.value = offsetX.value
  dragOriginY.value = offsetY.value
}

const handleTouchMove = (event) => {
  if (processing.value || !imageReady.value) return
  const touches = event.touches || []
  if (touchMode.value === 'pinch' && touches.length >= 2) {
    const distance = normalizeDistance(touches)
    if (!pinchStartDistance.value) return
    const nextScale = pinchStartScale.value * (distance / pinchStartDistance.value)
    userScale.value = Math.min(4, Math.max(1, nextScale))
    clampOffsets()
    return
  }

  if (touchMode.value !== 'drag') return
  const touch = touches[0]
  if (!touch) return
  offsetX.value = dragOriginX.value + (touch.clientX - dragStartX.value)
  offsetY.value = dragOriginY.value + (touch.clientY - dragStartY.value)
  clampOffsets()
}

const handleTouchEnd = () => {
  touchMode.value = ''
}

const loadImageInfo = async () => {
  const path = consumeAvatarCropTask()
  if (!path) {
    uni.showToast({ title: '未找到待裁剪图片', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 200)
    return
  }

  sourcePath.value = path
  cropBoxSize.value = Math.max(240, uni.getSystemInfoSync().windowWidth - rpxToPx(72))

  try {
    const imageInfo = await new Promise((resolve, reject) => {
      uni.getImageInfo({
        src: path,
        success: resolve,
        fail: reject
      })
    })
    imageWidth.value = imageInfo.width
    imageHeight.value = imageInfo.height
    initializeTransform()
    imageReady.value = true
  } catch (error) {
    uni.showToast({ title: '图片读取失败', icon: 'none' })
    setTimeout(() => handleCancel(), 300)
  }
}

const handleCancel = () => {
  if (!settled.value) {
    settled.value = true
    rejectAvatarCropTask(new Error('cancel'))
  }
  uni.navigateBack()
}

const exportCroppedImage = () =>
  new Promise((resolve, reject) => {
    const ctx = uni.createCanvasContext('avatarCropCanvas', instance?.proxy)
    const displayWidth = imageWidth.value * baseScale.value * userScale.value
    const displayHeight = imageHeight.value * baseScale.value * userScale.value
    const ratio = canvasOutputSize / cropBoxSize.value
    const drawWidth = displayWidth * ratio
    const drawHeight = displayHeight * ratio
    const drawX = ((cropBoxSize.value - displayWidth) / 2 + offsetX.value) * ratio
    const drawY = ((cropBoxSize.value - displayHeight) / 2 + offsetY.value) * ratio

    ctx.setFillStyle('#ffffff')
    ctx.fillRect(0, 0, canvasOutputSize, canvasOutputSize)
    ctx.drawImage(sourcePath.value, drawX, drawY, drawWidth, drawHeight)
    ctx.draw(false, () => {
      uni.canvasToTempFilePath(
        {
          canvasId: 'avatarCropCanvas',
          x: 0,
          y: 0,
          width: canvasOutputSize,
          height: canvasOutputSize,
          destWidth: canvasOutputSize,
          destHeight: canvasOutputSize,
          fileType: 'jpg',
          quality: 1,
          success: (result) => resolve(result.tempFilePath),
          fail: reject
        },
        instance?.proxy
      )
    })
  })

const handleConfirm = async () => {
  if (processing.value || !imageReady.value) return
  processing.value = true
  uni.showLoading({ title: '生成中...' })
  try {
    const tempFilePath = await exportCroppedImage()
    settled.value = true
    resolveAvatarCropTask(tempFilePath)
    uni.navigateBack()
  } catch (error) {
    uni.showToast({ title: '头像裁剪失败', icon: 'none' })
  } finally {
    uni.hideLoading()
    processing.value = false
  }
}

onLoad(() => {
  loadImageInfo()
})

onUnload(() => {
  if (!settled.value) {
    rejectAvatarCropTask(new Error('cancel'))
  }
})
</script>

<style scoped lang="scss">
@import '@/styles/user-ui.scss';

.page {
  @include user-page;
  min-height: 100vh;
  padding: 32rpx 28rpx 44rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.hero {
  padding: 18rpx 6rpx 28rpx;
}

.title {
  display: block;
  font-size: 40rpx;
  line-height: 1.2;
  font-weight: 700;
  color: $user-color-text-main;
  margin-bottom: 10rpx;
}

.tip {
  display: block;
  font-size: 24rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
}

.crop-shell {
  @include user-card(28rpx);
  display: flex;
  align-items: center;
  justify-content: center;
}

.crop-stage {
  position: relative;
  overflow: hidden;
  border-radius: 32rpx;
  background: #0f172a;
}

.crop-image {
  position: absolute;
  left: 50%;
  top: 50%;
  transform-origin: center center;
  will-change: transform;
}

.crop-mask {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, transparent 0 48%, rgba(15, 23, 42, 0.48) 56%);
  pointer-events: none;
}

.crop-frame {
  position: absolute;
  inset: 0;
  border: 4rpx solid rgba(255, 255, 255, 0.92);
  border-radius: 32rpx;
  box-sizing: border-box;
  pointer-events: none;
  box-shadow: inset 0 0 0 999rpx rgba(255, 255, 255, 0.01);
}

.hint-row {
  padding: 22rpx 10rpx 0;
}

.hint-text {
  display: block;
  font-size: 22rpx;
  line-height: 1.6;
  color: $user-color-text-sub;
  text-align: center;
}

.action-row {
  margin-top: auto;
  padding-top: 40rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.ghost-btn {
  @include user-ghost-btn(84rpx);
  font-size: 28rpx;
  font-weight: 600;
}

.primary-btn {
  @include user-primary-btn;
  height: 84rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
  pointer-events: none;
}
</style>
