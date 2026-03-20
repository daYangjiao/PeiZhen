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
              <text class="banner-title">专业医疗陪诊服务</text>
              <text class="banner-subtitle">让您的医疗就诊更加便捷</text>
              <view class="banner-btn" @click="navigateToAppointmentForm">
                <text class="btn-text">立即预约陪诊</text>
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
        <text class="section-title">推荐陪诊员</text>
      </view>
      <scroll-view class="companion-scroll" scroll-x="true" show-scrollbar="true">
        <view class="companion-list">
          <view
            class="companion-item"
            v-for="(companion, index) in companions"
            :key="index"
            @click="navigateToCompanion(companion)"
          >
            <image class="companion-avatar" :src="getFullAvatarUrl(companion.avatar)"></image>
            <view class="companion-info">
              <text class="companion-name">{{ companion.name }}</text>
              <text class="companion-specialty">{{ companion.professionalField }}</text>
              <text class="companion-experience">{{ companion.experienceYears }}年经验</text>
              <view class="rating-section">
                <text class="rating">★ {{ companion.score }}</text>
                <text class="service-count">已服务 {{ companion.serviceCount || 0 }} 人次</text>
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
        @click.stop="navigateToAIaks"
      >
        <image class="floating-icon" :src="assistantEntryIcon" mode="aspectFit" />
      </movable-view>
    </movable-area>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getRecommendedAttendants } from '@/api/attendant.js'
import { canUseRemoteImageUrl, config, getBackendImageUrl, getLocalFirstImageUrl } from '@/utils/api.js'
import { ren1, wujiaoxin, xin, yvyue2 } from '@/utils/assets.js'

const searchKeyword = ref('')
const bannerImage = getLocalFirstImageUrl('banner.jpg', '/static/banner.jpg')
const assistantEntryIcon = getLocalFirstImageUrl('mynewlogo.png', '/static/mynewlogo.png')
const categories = ref([
  { name: '门诊陪诊', icon: getLocalFirstImageUrl('category1.jpg', '/static/category1.jpg') },
  { name: '住院陪护', icon: getLocalFirstImageUrl('category2.jpg', '/static/category2.jpg') },
  { name: '专家会诊', icon: getLocalFirstImageUrl('category3.jpg', '/static/category3.jpg') },
  { name: '检查陪同', icon: getLocalFirstImageUrl('category4.jpg', '/static/category4.jpg') }
])
const services = ref([
  { name: '预约服务', icon: yvyue2 },
  { name: '匹配陪诊员', icon: ren1 },
  { name: '专业陪诊', icon: xin },
  { name: '评价反馈', icon: wujiaoxin }
])
const companions = ref([])

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

const navigateToAIaks = () => {
  if (suppressClick.value) return
  uni.navigateTo({ url: '/subpkg/ai/AIaks' })
}

const navigateToAppointmentForm = () => {
  // 与底部“预约”Tab 保持一致，先进入 AItriage 页面，再由该页重定向到子包表单
  uni.switchTab({ url: '/pages/AItriage/01_AppointmentSelection' })
}

const navigateToCategory = () => {
  uni.showToast({ title: '分类功能待接入', icon: 'none' })
}

const navigateToCompanion = () => {
  uni.showToast({ title: '陪诊师详情功能暂未开放', icon: 'none' })
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    uni.showToast({ title: `搜索"${searchKeyword.value}"`, icon: 'none' })
  } else {
    uni.showToast({ title: '请输入搜索内容', icon: 'none' })
  }
}

const getFullAvatarUrl = (relativePath) => {
  const defaultAvatar = '/static/default-avatar.jpg'
  if (!relativePath) return getLocalFirstImageUrl('default-avatar.jpg', defaultAvatar)
  if (relativePath.startsWith('http')) {
    return canUseRemoteImageUrl(relativePath) ? relativePath : defaultAvatar
  }
  const baseUrl = config.assetBaseURL.endsWith('/') ? config.assetBaseURL : config.assetBaseURL + '/'
  const avatarPath = relativePath.startsWith('/') ? relativePath.substring(1) : relativePath
  const fullUrl = baseUrl + avatarPath
  return canUseRemoteImageUrl(fullUrl) ? fullUrl : defaultAvatar
}

const fetchAttendants = async () => {
  try {
    const res = await getRecommendedAttendants()
    if (res.code === 200 && res.data) {
      companions.value = res.data
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
  color: #66a6ff;
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
.companion-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-bottom: 15rpx;
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
  color: #66a6ff;
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
