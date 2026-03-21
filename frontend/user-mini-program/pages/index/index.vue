<template>
  <view class="container">
    <!-- 顶部区域 -->
    <view class="header">
      <!-- 搜索框 -->
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

    <!-- 轮播图 -->
    <view class="banner-section">
      <swiper class="banner-swiper" indicator-dots="true" autoplay="true" interval="3000" duration="500">
        <swiper-item>
          <view class="banner-item">
            <image class="banner-bg" :src="getBackendImageUrl('banner.jpg')"></image>
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

    <!-- 服务分类 -->
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

    <!-- 服务流程 -->
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

    <!-- 推荐陪诊员 -->
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

    <!-- ========== 新增：可拖动悬浮按钮 ========== -->
    <view
      class="floating-btn"
      :style="{ left: btnLeft + 'px', top: btnTop + 'px' }"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @touchend="isDragging = false"
      @mousedown="handleMouseDown"
      @mousemove="handleMouseMove"
      @mouseup="isDragging = false"
      @mouseleave="isDragging = false"
      @click.stop="navigateToAIaks"
    >
      <image class="floating-icon" src="/static/brand-logo.png" mode="aspectFit" />
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecommendedAttendants } from '../../api/attendant.js'
import { config, getBackendImageUrl } from '../../utils/api.js'

// 响应式数据
const searchKeyword = ref('')
const categories = ref([
  { name: '普通陪诊', icon: '/static/service-general.png' },
  { name: '术后护理', icon: '/static/service-postop.png' },
  { name: '急诊陪同', icon: '/static/service-emergency.png' },
  { name: '上门陪诊', icon: '/static/service-home.png' }
])
const services = ref([
  { name: '预约服务', icon: '/static/yvyue_2.png' },
  { name: '匹配陪诊员', icon: '/static/ren_1.png' },
  { name: '专业陪诊', icon: '/static/xin.png' },
  { name: '评价反馈', icon: '/static/wujiaoxin.png' }
])
const companions = ref([])

// ========== 悬浮按钮位置状态 ==========
const btnLeft = ref(0)
const btnTop = ref(0)
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const startLeft = ref(0)
const startTop = ref(0)

// ========== 拖拽逻辑 ==========
const handleTouchStart = (e) => {
  const touch = e.touches[0]
  startX.value = touch.clientX
  startY.value = touch.clientY
  startLeft.value = btnLeft.value
  startTop.value = btnTop.value
  isDragging.value = true
}

const handleTouchMove = (e) => {
  if (!isDragging.value) return
  const touch = e.touches[0]
  const deltaX = touch.clientX - startX.value
  const deltaY = touch.clientY - startY.value
  updatePosition(startLeft.value + deltaX, startTop.value + deltaY)
}

const updatePosition = (x, y) => {
  const sysInfo = uni.getSystemInfoSync()
  const windowWidth = sysInfo.windowWidth
  const windowHeight = sysInfo.windowHeight
  const btnSizePx = 60 * (windowWidth / 750)

  x = Math.max(0, Math.min(windowWidth - btnSizePx, x))
  y = Math.max(0, Math.min(windowHeight - btnSizePx, y))

  btnLeft.value = x
  btnTop.value = y
}

const setInitialPosition = () => {
  const sysInfo = uni.getSystemInfoSync()
  const windowWidth = sysInfo.windowWidth
  const windowHeight = sysInfo.windowHeight
  const btnSizePx = 60 * (windowWidth / 750)

  btnLeft.value = windowWidth - btnSizePx
  btnTop.value = windowHeight - btnSizePx
}

// ========== 跳转方法 ==========
const navigateToAIaks = () => {
  uni.navigateTo({ url: '/pages/AIaks/AIaks' })
}

const navigateToAppointmentForm = () => {
  // 正确：用switchTab跳转到tabbar页面
  uni.switchTab({
    url: '/pages/AItriage/01_AppointmentSelection', // tabbar页面路径
    success: () => {
      console.log('跳转到tabbar页面成功');
    },
    fail: (err) => {
      console.error('跳转失败', err);
    }
  });
}



const navigateToCompanion = (companion) => {
  uni.showToast({ title: '陪诊师详情功能暂未开放', icon: 'none' })
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    uni.showToast({ title: `搜索"${searchKeyword.value}"`, icon: 'none' })
  } else {
    uni.showToast({ title: '请输入搜索内容', icon: 'none' })
  }
}

// 获取完整头像URL
const getFullAvatarUrl = (relativePath) => {
  if (!relativePath) return getBackendImageUrl('default-avatar.jpg');
  if (relativePath.startsWith('http')) return relativePath;
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : config.baseURL + '/';
  const avatarPath = relativePath.startsWith('/') ? relativePath.substring(1) : relativePath;
  return baseUrl + avatarPath;
}

// 获取陪诊师列表
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

// 页面挂载后执行
onMounted(() => {
  setInitialPosition()
  fetchAttendants()
})
</script>

<style>
.container {
  min-height: 100vh;
  background-color: #f5f5f5;
  position: relative;
}

.floating-btn {
  position: fixed;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
  z-index: 9999;
  cursor: grab;
}

.floating-icon {
  width: 40px;
  height: 40px;
}

.header {
  background: linear-gradient(135deg, #ffffff 0%, #ffffff 100%);
  padding: 0 30rpx 30rpx;
}
.search-box {
  background-color: #f5f5f5;
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
  height: 300rpx;
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
  color: #4a90e2;
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
  color: #4a90e2;
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
