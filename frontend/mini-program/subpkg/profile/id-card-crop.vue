<template>
  <view class="page">
    <view class="hero">
      <text class="title">{{ sideLabel }}框选</text>
      <text class="tip">拖动或双指缩放，让身份证四边完整落入蓝色框内</text>
    </view>

    <view class="crop-shell">
      <view
        class="crop-stage"
        :style="{ width: `${cropBoxWidth}px`, height: `${cropBoxHeight}px` }"
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
        <view class="id-frame">
          <view class="corner tl"></view>
          <view class="corner tr"></view>
          <view class="corner bl"></view>
          <view class="corner br"></view>
          <text class="frame-label">身份证区域</text>
        </view>
      </view>
    </view>

    <view class="check-card">
      <view class="check-item">
        <text class="check-dot"></text>
        <text>姓名、号码、有效期保持清晰无遮挡</text>
      </view>
      <view class="check-item">
        <text class="check-dot"></text>
        <text>不要只拍局部，四个圆角需要完整进入框内</text>
      </view>
    </view>

    <view class="action-row">
      <button class="ghost-btn" @click="handleCancel">取消</button>
      <button class="primary-btn" :disabled="processing || !imageReady" @click="handleConfirm">
        {{ processing ? '生成中...' : '完成框选' }}
      </button>
    </view>

    <canvas
      canvas-id="idCardCropCanvas"
      id="idCardCropCanvas"
      class="hidden-canvas"
      :style="{ width: `${canvasOutputWidth}px`, height: `${canvasOutputHeight}px` }"
    ></canvas>
  </view>
</template>

<script setup>
import { computed, getCurrentInstance, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { consumeIdCardFrameTask, rejectIdCardFrameTask, resolveIdCardFrameTask } from '@/utils/id-card-upload.js'

const instance = getCurrentInstance()
const ID_CARD_RATIO = 856 / 540
const canvasOutputWidth = 1280
const canvasOutputHeight = Math.round(canvasOutputWidth / ID_CARD_RATIO)

const sourcePath = ref('')
const side = ref('front')
const processing = ref(false)
const imageReady = ref(false)

const imageWidth = ref(0)
const imageHeight = ref(0)
const baseScale = ref(1)
const userScale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)

const cropBoxWidth = ref(0)
const cropBoxHeight = ref(0)

const touchMode = ref('')
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragOriginX = ref(0)
const dragOriginY = ref(0)
const pinchStartDistance = ref(0)
const pinchStartScale = ref(1)
const settled = ref(false)

const sideLabel = computed(() => side.value === 'back' ? '身份证背面' : '身份证正面')

const imageStyle = computed(() => {
  const width = imageWidth.value * baseScale.value * userScale.value
  const height = imageHeight.value * baseScale.value * userScale.value
  return {
    left: `${(cropBoxWidth.value - width) / 2 + offsetX.value}px`,
    top: `${(cropBoxHeight.value - height) / 2 + offsetY.value}px`,
    width: `${width}px`,
    height: `${height}px`,
  }
})

const rpxToPx = (rpx) => {
  const { windowWidth } = uni.getSystemInfoSync()
  return (Number(rpx) * windowWidth) / 750
}

const resolveCropBox = () => {
  const systemInfo = uni.getSystemInfoSync()
  const safeBottom = systemInfo.safeAreaInsets?.bottom || 0
  const horizontalPadding = rpxToPx(48)
  const shellPadding = rpxToPx(28)
  const reservedHeight = rpxToPx(86) + rpxToPx(124) + rpxToPx(128) + safeBottom
  const maxWidth = systemInfo.windowWidth - horizontalPadding - shellPadding
  const maxHeight = systemInfo.windowHeight - reservedHeight
  let width = maxWidth
  let height = width / ID_CARD_RATIO

  if (height > maxHeight) {
    height = maxHeight
    width = height * ID_CARD_RATIO
  }

  cropBoxWidth.value = Math.max(rpxToPx(300), width)
  cropBoxHeight.value = cropBoxWidth.value / ID_CARD_RATIO
}

const getDisplaySize = () => ({
  width: imageWidth.value * baseScale.value * userScale.value,
  height: imageHeight.value * baseScale.value * userScale.value,
})

const clampOffsets = () => {
  const { width, height } = getDisplaySize()
  const maxOffsetX = Math.max(0, (width - cropBoxWidth.value) / 2)
  const maxOffsetY = Math.max(0, (height - cropBoxHeight.value) / 2)
  offsetX.value = Math.min(maxOffsetX, Math.max(-maxOffsetX, offsetX.value))
  offsetY.value = Math.min(maxOffsetY, Math.max(-maxOffsetY, offsetY.value))
}

const initializeTransform = () => {
  baseScale.value = Math.max(cropBoxWidth.value / imageWidth.value, cropBoxHeight.value / imageHeight.value)
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
    userScale.value = Math.min(5, Math.max(1, pinchStartScale.value * (distance / pinchStartDistance.value)))
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
  const task = consumeIdCardFrameTask()
  if (!task.filePath) {
    uni.showToast({ title: '未找到待框选图片', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 200)
    return
  }

  sourcePath.value = task.filePath
  side.value = task.side || 'front'
  resolveCropBox()

  try {
    const imageInfo = await new Promise((resolve, reject) => {
      uni.getImageInfo({
        src: sourcePath.value,
        success: resolve,
        fail: reject,
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
    rejectIdCardFrameTask(new Error('cancel'))
  }
  uni.navigateBack()
}

const exportFramedImage = () =>
  new Promise((resolve, reject) => {
    const ctx = uni.createCanvasContext('idCardCropCanvas', instance?.proxy)
    const displayWidth = imageWidth.value * baseScale.value * userScale.value
    const displayHeight = imageHeight.value * baseScale.value * userScale.value
    const ratio = canvasOutputWidth / cropBoxWidth.value
    const drawWidth = displayWidth * ratio
    const drawHeight = displayHeight * ratio
    const drawX = ((cropBoxWidth.value - displayWidth) / 2 + offsetX.value) * ratio
    const drawY = ((cropBoxHeight.value - displayHeight) / 2 + offsetY.value) * ratio

    ctx.setFillStyle('#ffffff')
    ctx.fillRect(0, 0, canvasOutputWidth, canvasOutputHeight)
    ctx.drawImage(sourcePath.value, drawX, drawY, drawWidth, drawHeight)
    ctx.draw(false, () => {
      uni.canvasToTempFilePath(
        {
          canvasId: 'idCardCropCanvas',
          x: 0,
          y: 0,
          width: canvasOutputWidth,
          height: canvasOutputHeight,
          destWidth: canvasOutputWidth,
          destHeight: canvasOutputHeight,
          fileType: 'jpg',
          quality: 1,
          success: (result) => resolve(result.tempFilePath),
          fail: reject,
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
    const tempFilePath = await exportFramedImage()
    settled.value = true
    resolveIdCardFrameTask(tempFilePath)
    uni.navigateBack()
  } catch (error) {
    uni.showToast({ title: '身份证框选失败', icon: 'none' })
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
    rejectIdCardFrameTask(new Error('cancel'))
  }
})
</script>

<style scoped lang="scss">
@import '@/styles/escort-ui.scss';

.page {
  @include escort-page;
  min-height: 100vh;
  padding: 18rpx 24rpx calc(22rpx + env(safe-area-inset-bottom));
  padding: 18rpx 24rpx calc(22rpx + constant(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.hero {
  padding: 4rpx 6rpx 14rpx;
}

.title {
  display: block;
  color: #172033;
  font-size: 32rpx;
  font-weight: 800;
  text-align: center;
}

.tip {
  display: block;
  margin-top: 8rpx;
  color: #6b7890;
  font-size: 23rpx;
  line-height: 1.4;
  text-align: center;
}

.crop-shell {
  @include escort-card(24rpx);
  padding: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.crop-stage {
  position: relative;
  overflow: hidden;
  border-radius: 30rpx;
  background: #0f172a;
}

.crop-image {
  position: absolute;
}

.crop-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.22);
  pointer-events: none;
}

.id-frame {
  position: absolute;
  inset: 0;
  border: 4rpx solid rgba(55, 132, 255, 0.96);
  border-radius: 30rpx;
  box-sizing: border-box;
  pointer-events: none;
  box-shadow: inset 0 0 0 999rpx rgba(255, 255, 255, 0.02), 0 0 0 2rpx rgba(255, 255, 255, 0.92);
}

.corner {
  position: absolute;
  width: 48rpx;
  height: 48rpx;
  border-color: #ffffff;
  border-style: solid;
}

.corner.tl {
  left: 16rpx;
  top: 16rpx;
  border-width: 6rpx 0 0 6rpx;
}

.corner.tr {
  right: 16rpx;
  top: 16rpx;
  border-width: 6rpx 6rpx 0 0;
}

.corner.bl {
  left: 16rpx;
  bottom: 16rpx;
  border-width: 0 0 6rpx 6rpx;
}

.corner.br {
  right: 16rpx;
  bottom: 16rpx;
  border-width: 0 6rpx 6rpx 0;
}

.frame-label {
  position: absolute;
  left: 24rpx;
  top: 24rpx;
  height: 44rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.92);
  color: #1d65e6;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 44rpx;
}

.check-card {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 24rpx;
  border: 1rpx solid #dce9fb;
  background: rgba(255, 255, 255, 0.9);
  display: grid;
  gap: 10rpx;
}

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 10rpx;
  color: #5e6c82;
  font-size: 22rpx;
  line-height: 1.45;
}

.check-dot {
  width: 10rpx;
  height: 10rpx;
  margin-top: 10rpx;
  border-radius: 50%;
  background: $escort-color-primary;
  flex: 0 0 auto;
}

.action-row {
  padding-top: 18rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  flex-shrink: 0;
}

.ghost-btn,
.primary-btn {
  width: 100%;
  height: 88rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1;
  font-size: 28rpx;
  font-weight: 700;

  &::after {
    border: none;
  }
}

.ghost-btn {
  border: 1rpx solid #d8e6f5;
  background: #ffffff;
  color: #52627a;
}

.primary-btn {
  background: $escort-color-primary;
  color: #ffffff;
  box-shadow: $escort-shadow-primary;
}

.hidden-canvas {
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
  pointer-events: none;
}
</style>
