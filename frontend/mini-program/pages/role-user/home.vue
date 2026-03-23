<template>
  <view class="container">
    <view v-if="publicSafeMode" class="public-site">
      <view class="public-hero">
        <image class="public-logo" :src="assistantEntryIcon" mode="aspectFit"></image>
        <text class="public-title">愈安陪诊</text>
        <text class="public-subtitle">就医流程参考与陪诊服务信息展示</text>
        <text class="public-desc">为有就医陪同需求的人群提供服务介绍、流程说明、术后护理参考与常见问题整理。</text>
      </view>

      <view class="public-panel">
        <view class="public-section-head">
          <text class="public-section-title">服务分类介绍</text>
          <text class="public-section-note">围绕四类常见陪诊场景整理基础说明</text>
        </view>
        <view class="public-category-grid">
          <view class="public-category-card" v-for="(item, index) in categories" :key="`safe-cat-${index}`">
            <image class="public-category-icon" :src="item.icon" mode="aspectFit"></image>
            <text class="public-category-name">{{ item.name }}</text>
            <text class="public-category-desc">{{ getCategoryDescription(item.name) }}</text>
          </view>
        </view>
      </view>

      <view class="public-panel">
        <view class="public-section-head">
          <text class="public-section-title">就医流程参考</text>
          <text class="public-section-note">从准备、沟通到随访的常见流程梳理</text>
        </view>
        <view class="public-process-list">
          <view class="public-process-item" v-for="(item, index) in publicProcessList" :key="`safe-process-${index}`">
            <view class="public-process-index">{{ index + 1 }}</view>
            <view class="public-process-body">
              <text class="public-process-title">{{ item.title }}</text>
              <text class="public-process-text">{{ item.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="public-panel">
        <view class="public-section-head">
          <text class="public-section-title">陪诊经验人物卡</text>
          <text class="public-section-note">展示不同经验方向的陪诊服务形象与擅长领域</text>
        </view>
        <view class="public-companion-list">
          <view class="public-companion-card" v-for="(companion, index) in companions" :key="`safe-companion-${index}`">
            <image class="public-companion-avatar" :src="getFullAvatarUrl(companion.avatar)"></image>
            <view class="public-companion-body">
              <text class="public-companion-name">{{ companion.name }}</text>
              <text class="public-companion-meta">{{ companion.professionalField }}</text>
              <text class="public-companion-meta">{{ companion.experienceYears }}年经验 · 评分 {{ companion.score }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="public-panel public-note-panel">
        <text class="public-section-title">说明</text>
        <text class="public-note-text">当前网站以服务介绍、就医流程参考、健康管理信息与陪诊经验展示为主，相关内容仅供了解与参考。</text>
      </view>
    </view>

    <template v-else>
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
        <text class="section-title">{{ publicSafeMode ? '陪诊经验人物卡' : '推荐陪诊员' }}</text>
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
                <text class="service-count">{{ publicSafeMode ? '人物示例展示' : `已服务 ${companion.serviceCount || 0} 人次` }}</text>
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
    </template>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getRecommendedAttendants } from '@/api/attendant.js'
import { canUseRemoteImageUrl, config, getBackendImageUrl, getLocalFirstImageUrl } from '@/utils/api.js'
import { appointmentServiceLogos, brandLogo, ren1, wujiaoxin, xin, yvyue2 } from '@/utils/assets.js'
import { PUBLIC_SAFE_NOTICE, isPublicSafeMode, showPublicSafeNotice } from '@/utils/site-mode.js'

const searchKeyword = ref('')
const publicSafeMode = isPublicSafeMode()
const bannerImage = getLocalFirstImageUrl('banner.jpg', '/static/banner.jpg')
const assistantEntryIcon = brandLogo
const categories = ref([
  { name: '普通陪诊', icon: appointmentServiceLogos[1] },
  { name: '术后护理', icon: appointmentServiceLogos[2] },
  { name: '急诊陪同', icon: appointmentServiceLogos[3] },
  { name: '上门陪诊', icon: appointmentServiceLogos[4] }
])
const services = ref([
  { name: '预约服务', icon: yvyue2 },
  { name: '匹配陪诊员', icon: ren1 },
  { name: '专业陪诊', icon: xin },
  { name: '评价反馈', icon: wujiaoxin }
])
const companions = ref([])
const publicProcessList = [
  { title: '就诊前准备', desc: '提前确认医院、科室、证件资料和陪同需求，减少现场等待与遗漏。' },
  { title: '到院流程参考', desc: '了解挂号、报到、检查、缴费和取药等常见就医环节，提升就诊效率。' },
  { title: '重点事项记录', desc: '整理医生建议、术后注意事项、复查节点和用药提醒，便于后续查看。' },
  { title: '术后与随访建议', desc: '根据常见场景提供术后护理、复诊随访和居家观察等参考信息。' }
]

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
  if (publicSafeMode) {
    uni.navigateTo({ url: '/subpkg/ai/AIaks' })
    return
  }
  if (suppressClick.value) return
  uni.navigateTo({ url: '/subpkg/ai/AIaks' })
}

const navigateToAppointmentForm = () => {
  if (publicSafeMode) {
    showPublicSafeNotice('当前网站主要提供服务介绍与流程参考')
    return
  }
  // 与底部“预约”Tab 保持一致，先进入 AItriage 页面，再由该页重定向到子包表单
  uni.switchTab({ url: '/pages/AItriage/01_AppointmentSelection' })
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
  uni.showToast({ title: '分类功能待接入', icon: 'none' })
}

const navigateToCompanion = () => {
  if (publicSafeMode) {
    showPublicSafeNotice('当前页面展示陪诊经验人物信息')
    return
  }
  uni.showToast({ title: '陪诊师详情功能暂未开放', icon: 'none' })
}

const getCategoryDescription = (name) => {
  const descriptionMap = {
    '普通陪诊': '适用于门诊就医、院内流程陪同与基础就诊协助场景。',
    '术后护理': '侧重术后恢复阶段的照护提醒、复诊准备与日常观察。',
    '急诊陪同': '围绕紧急就医场景整理急诊流程、准备事项与沟通建议。',
    '上门陪诊': '适合行动不便或特殊陪同需求场景的上门陪同说明。'
  }
  return descriptionMap[name] || '提供常见就医陪同场景的参考说明。'
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
.public-site {
  padding: 28rpx;
}
.public-hero {
  background: linear-gradient(150deg, #eaf4ff 0%, #f8fbff 62%, #ffffff 100%);
  border-radius: 28rpx;
  padding: 44rpx 36rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  box-shadow: 0 18rpx 48rpx rgba(0, 122, 255, 0.08);
  margin-bottom: 24rpx;
}
.public-logo {
  width: 132rpx;
  height: 132rpx;
  border-radius: 28rpx;
  margin-bottom: 24rpx;
}
.public-title {
  font-size: 44rpx;
  line-height: 1.2;
  font-weight: 700;
  color: #123a63;
  margin-bottom: 12rpx;
}
.public-subtitle {
  font-size: 28rpx;
  line-height: 1.6;
  font-weight: 600;
  color: #007aff;
  margin-bottom: 14rpx;
}
.public-desc {
  font-size: 24rpx;
  line-height: 1.8;
  color: #5d7388;
}
.public-panel {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx 26rpx;
  margin-bottom: 22rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 61, 104, 0.05);
}
.public-section-head {
  margin-bottom: 22rpx;
}
.public-section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 8rpx;
}
.public-section-note {
  display: block;
  font-size: 22rpx;
  color: #7b8ea3;
  line-height: 1.6;
}
.public-category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}
.public-category-card {
  background: #f7fbff;
  border: 1rpx solid #e2eefc;
  border-radius: 20rpx;
  padding: 24rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.public-category-icon {
  width: 84rpx;
  height: 84rpx;
  border-radius: 22rpx;
  margin-bottom: 16rpx;
}
.public-category-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #123a63;
  margin-bottom: 10rpx;
}
.public-category-desc {
  font-size: 22rpx;
  line-height: 1.7;
  color: #667b90;
}
.public-process-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}
.public-process-item {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
}
.public-process-index {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #007aff;
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 6rpx;
}
.public-process-body {
  flex: 1;
}
.public-process-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #16324f;
  margin-bottom: 8rpx;
}
.public-process-text {
  display: block;
  font-size: 22rpx;
  line-height: 1.7;
  color: #6a7f94;
}
.public-companion-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.public-companion-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  border: 1rpx solid #edf3fa;
  border-radius: 20rpx;
  padding: 18rpx;
}
.public-companion-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.public-companion-body {
  display: flex;
  flex-direction: column;
}
.public-companion-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 8rpx;
}
.public-companion-meta {
  font-size: 22rpx;
  color: #6f8194;
  line-height: 1.6;
}
.public-note-panel {
  margin-bottom: 0;
}
.public-note-text {
  display: block;
  font-size: 22rpx;
  line-height: 1.8;
  color: #647789;
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
