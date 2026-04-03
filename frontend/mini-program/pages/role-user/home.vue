<template>
  <view class="container">
    <view class="header">
      <view class="search-box">
        <image class="search-icon" src="/static/sous.png"></image>
        <input
          class="search-input"
          placeholder="搜索医院、科室或疾病"
          v-model="searchKeyword"
          @confirm="handleSearch"
          confirm-type="search"
        />
      </view>
    </view>

    <view class="banner-section">
      <swiper class="banner-swiper" indicator-dots="true" autoplay="true" interval="3000" duration="500">
        <swiper-item>
          <view class="banner-item">
            <image class="banner-bg" :src="bannerImage"></image>
            <view class="banner-content">
              <text class="banner-title">{{ publicSafeMode ? '个人就医流程与陪诊经验分享' : '专业医疗陪诊服务' }}</text>
              <text class="banner-subtitle">{{ publicSafeMode ? '提供就医流程参考、服务介绍与健康管理信息' : '让您的医疗就诊更加便捷' }}</text>
              <view class="banner-btn" @click="navigateToAppointmentForm">
                <text class="btn-text">{{ publicSafeMode ? '查看服务说明' : '立即预约陪诊' }}</text>
              </view>
            </view>
          </view>
        </swiper-item>
      </swiper>
    </view>

    <view class="category-section">
      <view class="category-grid">
        <view
          class="category-item"
          v-for="(item, index) in categories"
          :key="index"
          @click="navigateToCategory(item)"
        >
          <image class="category-icon" :src="item.icon"></image>
          <text class="category-text">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <view class="service-section">
      <view class="section-title">
        <text class="title-text">服务流程</text>
      </view>
      <view class="service-grid">
        <view class="service-item" v-for="(item, index) in services" :key="index">
          <image class="service-icon" :src="item.icon"></image>
          <text class="service-text">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <view class="companion-section">
      <view class="section-header">
        <text class="section-title" @click="toggleAvatarDiagnostics">{{ publicSafeMode ? '陪诊经验人物卡' : '推荐陪诊员' }}</text>
      </view>
      <scroll-view class="companion-scroll" scroll-x="true" show-scrollbar="true">
        <view class="companion-list">
          <view
            class="companion-item"
            v-for="(companion, index) in companions"
            :key="index"
            @click="navigateToCompanion(companion)"
          >
            <view class="companion-avatar-wrap">
            <image
              class="companion-avatar"
              :src="companion.displayAvatar"
              mode="aspectFill"
              @load="handleCompanionAvatarLoad(companion, $event)"
              @error="handleCompanionAvatarError(companion, $event)"
            ></image>
            <view
              v-if="showAvatarDiagnostics"
              class="companion-avatar-bg-check"
              :style="getCompanionAvatarStyle(companion)"
            ></view>
          </view>
            <view class="companion-info">
              <text class="companion-name">{{ companion.name }}</text>
              <text class="companion-specialty">{{ companion.professionalField }}</text>
              <text class="companion-experience">{{ companion.experienceYears }}年经验</text>
              <view class="rating-section">
                <text class="rating">★ {{ companion.score }}</text>
                <text class="service-count">{{ publicSafeMode ? '人物示例展示' : `已服务 ${companion.serviceCount || 0} 人次` }}</text>
              </view>
              <view v-if="showAvatarDiagnostics" class="avatar-diagnostic-card">
                <text class="avatar-diagnostic-line">raw: {{ companion.avatarDiagnostic?.rawAvatar || '-' }}</text>
                <text class="avatar-diagnostic-line">resolved: {{ companion.avatarDiagnostic?.resolvedAvatar || '-' }}</text>
                <text class="avatar-diagnostic-line">image: {{ companion.avatarDiagnostic?.imageStatus || '-' }}</text>
                <text class="avatar-diagnostic-line">detail: {{ companion.avatarDiagnostic?.imageDetail || '-' }}</text>
                <text class="avatar-diagnostic-line">download: {{ companion.avatarDiagnostic?.downloadStatus || '-' }}</text>
                <text class="avatar-diagnostic-line">temp: {{ companion.avatarDiagnostic?.downloadTempPath || '-' }}</text>
                <text class="avatar-diagnostic-line">imageInfo: {{ companion.avatarDiagnostic?.imageInfoStatus || '-' }}</text>
                <view class="avatar-diagnostic-btn" @click.stop="runCompanionAvatarDiagnostics(companion)">
                  <text class="avatar-diagnostic-btn-text">重新检测</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <movable-area class="floating-area">
      <movable-view
        class="floating-btn"
        direction="all"
        :x="btnLeft"
        :y="btnTop"
        :inertia="false"
        :damping="36"
        :friction="2"
        @touchstart="handleTouchStart"
        @touchend="handleTouchEnd"
        @touchcancel="handleTouchCancel"
        @change="handleFloatingChange"
        @click.stop="navigateToAiAsk"
      >
        <image class="floating-icon" :src="assistantEntryIcon" mode="aspectFit" />
      </movable-view>
    </movable-area>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getRecommendedAttendants } from '@/api/attendant.js'
import { getLocalFirstImageUrl } from '@/utils/api.js'
import { appointmentServiceLogos, brandLogo, ren1, wujiaoxin, xin, yvyue2 } from '@/utils/assets.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { PUBLIC_SAFE_LANDING_URL, PUBLIC_SAFE_NOTICE, isPublicSafeMode, showPublicSafeNotice } from '@/utils/site-mode.js'

const searchKeyword = ref('')
const publicSafeMode = isPublicSafeMode()
const bannerImage = getLocalFirstImageUrl('banner.jpg', '/static/banner.jpg')
const assistantEntryIcon = brandLogo
const categories = ref([
  { name: '普通陪诊', icon: appointmentServiceLogos[1], serviceTypeNumber: 1 },
  { name: '术后护理', icon: appointmentServiceLogos[2], serviceTypeNumber: 2 },
  { name: '急诊陪同', icon: appointmentServiceLogos[3], serviceTypeNumber: 3 },
  { name: '上门陪诊', icon: appointmentServiceLogos[4], serviceTypeNumber: 4 }
])
const services = ref([
  { name: '预约服务', icon: yvyue2 },
  { name: '匹配陪诊员', icon: ren1 },
  { name: '专业陪诊', icon: xin },
  { name: '评价反馈', icon: wujiaoxin }
])
const companions = ref([])
const defaultCompanionAvatar = getLocalFirstImageUrl('default-avatar.jpg', '/static/default-avatar.jpg')
const showAvatarDiagnostics = ref(false)
const avatarDiagnosticTapCount = ref(0)
let avatarDiagnosticTimer = null
const btnLeft = ref(0)
const btnTop = ref(0)
const areaWidth = ref(0)
const areaHeight = ref(0)
const safeTopInset = ref(0)
const safeBottomInset = ref(0)
const isTouching = ref(false)
const hasDragged = ref(false)
const suppressClick = ref(false)
const startLeft = ref(0)
const startTop = ref(0)
const DRAG_THRESHOLD = 8
const FLOAT_BTN_SIZE = 60
const EDGE_MARGIN = 8
const TAB_BAR_RESERVED = 56
const SUPPRESS_TIMEOUT = 1200
let suppressTimer = null
const handleWindowResize = () => {
  updatePositionAfterViewportChange()
}

const clearSuppressState = () => {
  suppressClick.value = false
  if (suppressTimer) {
    clearTimeout(suppressTimer)
    suppressTimer = null
  }
}

const setSuppressState = () => {
  suppressClick.value = true
  if (suppressTimer) {
    clearTimeout(suppressTimer)
  }
  suppressTimer = setTimeout(() => {
    suppressClick.value = false
    suppressTimer = null
  }, SUPPRESS_TIMEOUT)
}

const refreshViewportMetrics = () => {
  const sysInfo = uni.getSystemInfoSync()
  areaWidth.value = sysInfo.windowWidth
  areaHeight.value = sysInfo.windowHeight
  safeTopInset.value = sysInfo.safeAreaInsets?.top || 0
  safeBottomInset.value = sysInfo.safeAreaInsets?.bottom || 0
}

const normalizePosition = (x, y) => {
  const minX = EDGE_MARGIN
  const maxX = Math.max(minX, areaWidth.value - FLOAT_BTN_SIZE - EDGE_MARGIN)
  const minY = safeTopInset.value + EDGE_MARGIN
  const bottomReserved = safeBottomInset.value + TAB_BAR_RESERVED + EDGE_MARGIN
  const maxY = Math.max(minY, areaHeight.value - FLOAT_BTN_SIZE - bottomReserved)
  const nextX = Math.max(minX, Math.min(maxX, Number(x) || 0))
  const nextY = Math.max(minY, Math.min(maxY, Number(y) || 0))
  return { x: nextX, y: nextY }
}

const handleTouchStart = (e) => {
  if (!e.touches || !e.touches.length) return
  clearSuppressState()
  isTouching.value = true
  startLeft.value = btnLeft.value
  startTop.value = btnTop.value
  hasDragged.value = false
}

const handleFloatingChange = (e) => {
  const detail = e.detail || {}
  const { x, y } = normalizePosition(detail.x, detail.y)
  btnLeft.value = x
  btnTop.value = y

  if (isTouching.value && !hasDragged.value) {
    const movedX = Math.abs(x - startLeft.value)
    const movedY = Math.abs(y - startTop.value)
    if (movedX > DRAG_THRESHOLD || movedY > DRAG_THRESHOLD) {
      hasDragged.value = true
    }
  }
}

const finishDrag = () => {
  if (!isTouching.value) return
  isTouching.value = false
  if (hasDragged.value) {
    setSuppressState()
  }
  hasDragged.value = false
}

const handleTouchEnd = () => {
  finishDrag()
}

const handleTouchCancel = () => {
  finishDrag()
}

const setInitialPosition = () => {
  refreshViewportMetrics()
  const { x, y } = normalizePosition(
    areaWidth.value - FLOAT_BTN_SIZE - EDGE_MARGIN,
    areaHeight.value - FLOAT_BTN_SIZE - safeBottomInset.value - TAB_BAR_RESERVED - EDGE_MARGIN
  )
  btnLeft.value = x
  btnTop.value = y
}

const updatePositionAfterViewportChange = () => {
  refreshViewportMetrics()
  const { x, y } = normalizePosition(btnLeft.value, btnTop.value)
  btnLeft.value = x
  btnTop.value = y
}

const navigateToAiAsk = () => {
  if (publicSafeMode) {
    uni.reLaunch({ url: PUBLIC_SAFE_LANDING_URL })
    return
  }
  if (suppressClick.value) return
  uni.navigateTo({ url: '/subpkg/ai/ai-ask' })
}

const navigateToAppointmentForm = () => {
  if (publicSafeMode) {
    showPublicSafeNotice('当前网站主要提供服务介绍与流程参考')
    return
  }
  // 与底部“预约”Tab 保持一致，先进入 ai-triage 页面，再由该页重定向到子包表单
  uni.switchTab({ url: '/pages/ai-triage/01-appointment-selection' })
}

const navigateToCategory = (item) => {
  if (publicSafeMode) {
    uni.showModal({
      title: item.name,
      content: '当前页面主要展示服务介绍与流程参考信息。',
      showCancel: false
    })
    return
  }
  uni.navigateTo({
    url: `/subpkg/appointment-flow/02-appointment-form?serviceTypeNumber=${encodeURIComponent(item.serviceTypeNumber)}&serviceTypeName=${encodeURIComponent(item.name)}`
  })
}

const navigateToCompanion = () => {
  if (publicSafeMode) {
    showPublicSafeNotice('当前页面展示陪诊经验人物信息')
    return
  }
  uni.showToast({ title: '陪诊师详情功能暂未开放', icon: 'none' })
}

const toggleAvatarDiagnostics = () => {
  avatarDiagnosticTapCount.value += 1
  if (avatarDiagnosticTimer) clearTimeout(avatarDiagnosticTimer)
  avatarDiagnosticTimer = setTimeout(() => {
    avatarDiagnosticTapCount.value = 0
    avatarDiagnosticTimer = null
  }, 1200)
  if (avatarDiagnosticTapCount.value >= 5) {
    showAvatarDiagnostics.value = !showAvatarDiagnostics.value
    avatarDiagnosticTapCount.value = 0
    if (avatarDiagnosticTimer) {
      clearTimeout(avatarDiagnosticTimer)
      avatarDiagnosticTimer = null
    }
    uni.showToast({ title: showAvatarDiagnostics.value ? '头像诊断已开启' : '头像诊断已关闭', icon: 'none' })
  }
}

const updateCompanionDiagnostic = (companion, patch = {}) => {
  const target = companions.value.find((item) => item.id === companion.id || item.attendantId === companion.attendantId || item.name === companion.name)
  if (!target) return
  target.avatarDiagnostic = {
    ...(target.avatarDiagnostic || {}),
    ...patch
  }
}

const handleCompanionAvatarLoad = (companion, event) => {
  updateCompanionDiagnostic(companion, {
    imageStatus: 'loaded',
    imageDetail: JSON.stringify(event?.detail || {})
  })
}

const handleCompanionAvatarError = (companion, event) => {
  updateCompanionDiagnostic(companion, {
    imageStatus: 'error',
    imageDetail: JSON.stringify(event?.detail || {})
  })
}

const runCompanionAvatarDiagnostics = async (companion) => {
  const url = companion.displayAvatar || ''
  if (!url) {
    updateCompanionDiagnostic(companion, { downloadStatus: 'no-url' })
    return
  }
  if (typeof plus === 'undefined' && typeof uni.downloadFile !== 'function') {
    updateCompanionDiagnostic(companion, { downloadStatus: 'unsupported-runtime' })
    return
  }
  updateCompanionDiagnostic(companion, { downloadStatus: 'checking' })
  try {
    const [downloadRes, imageInfoRes] = await Promise.allSettled([
      new Promise((resolve, reject) => {
        uni.downloadFile({
          url,
          success: resolve,
          fail: reject
        })
      }),
      new Promise((resolve, reject) => {
        uni.getImageInfo({
          src: url,
          success: resolve,
          fail: reject
        })
      })
    ])

    const patch = {}
    if (downloadRes.status === 'fulfilled') {
      patch.downloadStatus = `download:${downloadRes.value.statusCode}`
      patch.downloadTempPath = downloadRes.value.tempFilePath || ''
    } else {
      patch.downloadStatus = 'download:fail'
      patch.downloadTempPath = JSON.stringify(downloadRes.reason || {})
    }

    if (imageInfoRes.status === 'fulfilled') {
      patch.imageInfoStatus = `imageInfo:${imageInfoRes.value.width}x${imageInfoRes.value.height}`
    } else {
      patch.imageInfoStatus = `imageInfo:fail ${JSON.stringify(imageInfoRes.reason || {})}`
    }

    updateCompanionDiagnostic(companion, patch)
  } catch (error) {
    updateCompanionDiagnostic(companion, {
      downloadStatus: `exception:${JSON.stringify(error || {})}`
    })
  }
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    uni.showToast({ title: `搜索"${searchKeyword.value}"`, icon: 'none' })
  } else {
    uni.showToast({ title: '请输入搜索内容', icon: 'none' })
  }
}

const decorateCompanion = async (companion = {}) => {
  const normalized = {
    ...companion,
    displayAvatar: resolveAvatarUrl(companion.avatar, defaultCompanionAvatar),
    avatarDiagnostic: {
      rawAvatar: companion.avatar || '',
      resolvedAvatar: resolveAvatarUrl(companion.avatar, defaultCompanionAvatar),
      imageStatus: 'pending',
      imageDetail: '',
      downloadStatus: 'pending',
      downloadTempPath: '',
      imageInfoStatus: 'pending'
    }
  }
  if (typeof uni !== 'undefined' && showAvatarDiagnostics.value) {
    runCompanionAvatarDiagnostics(normalized)
  }
  return normalized
}

const getCompanionAvatarStyle = (companion = {}) => {
  const avatarUrl = companion.displayAvatar || defaultCompanionAvatar
  return {
    backgroundImage: `url("${avatarUrl}")`
  }
}

const fetchAttendants = async () => {
  try {
    const res = await getRecommendedAttendants()
    if (res.code === 200 && res.data) {
      companions.value = await Promise.all((res.data || []).map(decorateCompanion))
    } else {
      uni.showToast({ title: '获取陪诊师列表失败', icon: 'none' })
    }
  } catch (error) {
    console.error('获取陪诊师列表出错:', error)
  }
}

onMounted(() => {
  setInitialPosition()
  fetchAttendants()
})
onLoad(() => {
  if (publicSafeMode) {
    uni.reLaunch({ url: PUBLIC_SAFE_LANDING_URL })
  }
})
onShow(() => {
  // 当次拖动：每次进入首页重置入口位置，不持久化。
  setInitialPosition()
  clearSuppressState()
})

onUnmounted(() => {
  clearSuppressState()
  if (typeof uni.offWindowResize === 'function') {
    uni.offWindowResize(handleWindowResize)
  }
})

if (typeof uni.onWindowResize === 'function') {
  uni.onWindowResize(handleWindowResize)
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.container {
  min-height: 100vh;
  background-color: #f5f7fa;
  position: relative;
}
.floating-area {
  position: fixed;
  left: 0;
  top: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  pointer-events: none;
}
.floating-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  pointer-events: auto;
}
.floating-icon {
  width: 40px;
  height: 40px;
}
.header {
  background: #ffffff;
  padding: 0 30rpx 30rpx;
}
.search-box {
  background-color: #f5f7fa;
  border-radius: 50rpx;
  padding: 20rpx 30rpx;
  display: flex;
  align-items: center;
}
.search-icon {
  width: 32rpx;
  height: 32rpx;
  margin-right: 20rpx;
}
.search-input {
  color: #333;
  font-size: 28rpx;
  flex: 1;
}
.banner-section {
  margin: 30rpx;
}
.banner-swiper {
  height: 360rpx;
  border-radius: 20rpx;
  overflow: hidden;
}
.banner-item {
  position: relative;
  height: 100%;
}
.banner-bg {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.banner-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to right, rgba(74, 144, 226, 0.9) 0%, rgba(74, 144, 226, 0.6) 50%, rgba(74, 144, 226, 0.2) 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 40rpx;
  color: white;
}
.banner-title {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
}
.banner-subtitle {
  font-size: 24rpx;
  margin-bottom: 30rpx;
  opacity: 0.9;
}
.banner-btn {
  background-color: white;
  border-radius: 30rpx;
  padding: 15rpx 30rpx;
}
.btn-text {
  color: #007AFF;
  font-size: 24rpx;
  font-weight: bold;
}
.category-section {
  margin: 30rpx;
  background-color: white;
  border-radius: 20rpx;
  padding: 40rpx 20rpx;
}
.category-grid {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
}
.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 25%;
  margin-bottom: 20rpx;
}
.category-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  margin-bottom: 15rpx;
}
.category-text {
  font-size: 24rpx;
  color: #333;
  text-align: center;
}
.service-section {
  margin: 30rpx;
  background-color: white;
  border-radius: 20rpx;
  padding: 40rpx 20rpx;
}
.section-title {
  margin-bottom: 30rpx;
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}
.service-grid {
  display: flex;
  justify-content: space-around;
}
.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 25%;
}
.service-icon {
  width: 60rpx;
  height: 60rpx;
  margin-bottom: 15rpx;
}
.service-text {
  font-size: 22rpx;
  color: #666;
  text-align: center;
}
.companion-section {
  margin: 30rpx;
  background-color: white;
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
}
.section-header {
  margin-bottom: 30rpx;
}
.companion-scroll {
  width: 100%;
  white-space: nowrap;
}
.companion-list {
  display: flex;
  flex-direction: row;
  padding: 0 10rpx;
}
.companion-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 280rpx;
  flex-shrink: 0;
  padding: 20rpx;
  margin-right: 20rpx;
  border: 2rpx solid #f0f0f0;
  border-radius: 15rpx;
}

.companion-avatar-wrap {
  position: relative;
  width: 100rpx;
  height: 100rpx;
  margin-bottom: 15rpx;
}
.companion-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background-color: #f4f6fb;
}
.companion-avatar-bg-check {
  position: absolute;
  right: -12rpx;
  bottom: -12rpx;
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  border: 2rpx solid #fff;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-color: #eef3ff;
}
.avatar-diagnostic-card {
  width: 100%;
  margin-top: 12rpx;
  padding: 12rpx;
  border-radius: 12rpx;
  background: #f7faff;
  border: 1px solid #dbe7ff;
}
.avatar-diagnostic-line {
  display: block;
  width: 100%;
  font-size: 18rpx;
  line-height: 1.5;
  color: #54637a;
  word-break: break-all;
  text-align: left;
}
.avatar-diagnostic-btn {
  margin-top: 10rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #e9f2ff;
  align-self: flex-start;
}
.avatar-diagnostic-btn-text {
  font-size: 20rpx;
  color: #007aff;
}
.companion-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.companion-name {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 8rpx;
}
.companion-specialty {
  font-size: 22rpx;
  color: #007AFF;
  margin-bottom: 5rpx;
}
.companion-experience {
  font-size: 20rpx;
  color: #666;
  margin-bottom: 8rpx;
}
.rating-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.rating {
  font-size: 20rpx;
  color: #ff9500;
  margin-bottom: 3rpx;
}
.service-count {
  font-size: 18rpx;
  color: #999;
}
</style>
