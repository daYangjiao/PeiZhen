<template>
  <view class="public-page">
    <view class="hero-card">
      <image class="hero-logo" :src="brandLogo" mode="aspectFit"></image>
      <text class="hero-title">愈安陪诊</text>
      <text class="hero-subtitle">就医流程参考与陪诊服务信息展示</text>
      <text class="hero-desc">围绕陪诊相关场景提供服务介绍、流程说明、术后护理建议与常见问题整理，便于个人了解与参考。</text>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="panel-title">服务分类介绍</text>
        <text class="panel-note">围绕四类常见陪诊场景整理基础说明</text>
      </view>
      <view class="category-grid">
        <view class="category-card" v-for="(item, index) in categories" :key="index">
          <image class="category-icon" :src="item.icon" mode="aspectFit"></image>
          <text class="category-name">{{ item.name }}</text>
          <text class="category-desc">{{ getCategoryDescription(item.name) }}</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="panel-title">就医流程参考</text>
        <text class="panel-note">从准备、沟通到随访的常见流程梳理</text>
      </view>
      <view class="process-list">
        <view class="process-item" v-for="(item, index) in processList" :key="index">
          <view class="process-index">{{ index + 1 }}</view>
          <view class="process-body">
            <text class="process-title">{{ item.title }}</text>
            <text class="process-text">{{ item.desc }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <text class="panel-title">陪诊经验人物卡</text>
        <text class="panel-note">展示不同经验方向的陪诊服务形象与擅长领域</text>
      </view>
      <view class="companion-list">
        <view class="companion-card" v-for="(companion, index) in companions" :key="index">
          <image class="companion-avatar" :src="companion.displayAvatar"></image>
          <view class="companion-body">
            <text class="companion-name">{{ companion.name }}</text>
            <text class="companion-meta">{{ companion.professionalField }}</text>
            <text class="companion-meta">{{ companion.experienceYears }}年经验 · 评分 {{ companion.score }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="panel note-panel">
      <text class="panel-title">说明</text>
      <text class="note-text">当前网站以服务介绍、就医流程参考、健康管理信息与陪诊经验展示为主，相关内容仅供了解与参考。</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecommendedAttendants } from '@/api/attendant.js'
import { appointmentServiceLogos, brandLogo } from '@/utils/assets.js'
import { getLocalFirstImageUrl } from '@/utils/api.js'
import { resolveAvatarUrl } from '@/utils/media.js'
import { formatRatingScore } from '@/utils/rating.js'

const categories = ref([
  { name: '普通陪诊', icon: appointmentServiceLogos[1] },
  { name: '术后护理', icon: appointmentServiceLogos[2] },
  { name: '急诊陪同', icon: appointmentServiceLogos[3] },
  { name: '上门陪诊', icon: appointmentServiceLogos[4] }
])

const processList = [
  { title: '就诊前准备', desc: '提前确认医院、科室、证件资料和陪同需求，减少现场等待与遗漏。' },
  { title: '到院流程参考', desc: '了解挂号、报到、检查、缴费和取药等常见就医环节，提升就诊效率。' },
  { title: '重点事项记录', desc: '整理医生建议、术后注意事项、复查节点和用药提醒，便于后续查看。' },
  { title: '术后与随访建议', desc: '根据常见场景提供术后护理、复诊随访和居家观察等参考信息。' }
]

const companions = ref([])
const defaultCompanionAvatar = getLocalFirstImageUrl('default-avatar.jpg', '/static/default-avatar.jpg')

const getCategoryDescription = (name) => {
  const descriptionMap = {
    '普通陪诊': '适用于门诊就医、院内流程陪同与基础就诊协助场景。',
    '术后护理': '侧重术后恢复阶段的照护提醒、复诊准备与日常观察。',
    '急诊陪同': '围绕紧急就医场景整理急诊流程、准备事项与沟通建议。',
    '上门陪诊': '适合行动不便或特殊陪同需求场景的上门陪同说明。'
  }
  return descriptionMap[name] || '提供常见就医陪同场景的参考说明。'
}

const decorateCompanion = (companion = {}) => ({
  ...companion,
  score: formatRatingScore(companion.score),
  displayAvatar: resolveAvatarUrl(companion.avatar, defaultCompanionAvatar)
})

const fetchAttendants = async () => {
  try {
    const res = await getRecommendedAttendants()
    if (res.code === 200 && res.data) {
      companions.value = (res.data || []).slice(0, 4).map(decorateCompanion)
    }
  } catch (error) {
    console.error('获取展示人物卡失败:', error)
  }
}

onMounted(() => {
  fetchAttendants()
})
</script>

<style lang="scss" scoped>
.public-page {
  min-height: 100vh;
  background: #f5f8fc;
  padding: 28rpx;
}
.hero-card {
  background: linear-gradient(150deg, #eaf4ff 0%, #f8fbff 62%, #ffffff 100%);
  border-radius: 28rpx;
  padding: 44rpx 36rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  box-shadow: 0 18rpx 48rpx rgba(0, 122, 255, 0.08);
  margin-bottom: 24rpx;
}
.hero-logo {
  width: 132rpx;
  height: 132rpx;
  border-radius: 28rpx;
  margin-bottom: 24rpx;
}
.hero-title {
  font-size: 44rpx;
  line-height: 1.2;
  font-weight: 700;
  color: #123a63;
  margin-bottom: 12rpx;
}
.hero-subtitle {
  font-size: 28rpx;
  line-height: 1.6;
  font-weight: 600;
  color: #007aff;
  margin-bottom: 14rpx;
}
.hero-desc {
  font-size: 24rpx;
  line-height: 1.8;
  color: #5d7388;
}
.panel {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx 26rpx;
  margin-bottom: 22rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 61, 104, 0.05);
}
.panel-head {
  margin-bottom: 22rpx;
}
.panel-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 8rpx;
}
.panel-note {
  display: block;
  font-size: 22rpx;
  color: #7b8ea3;
  line-height: 1.6;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}
.category-card {
  background: #f7fbff;
  border: 1rpx solid #e2eefc;
  border-radius: 20rpx;
  padding: 24rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.category-icon {
  width: 84rpx;
  height: 84rpx;
  border-radius: 22rpx;
  margin-bottom: 16rpx;
}
.category-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #123a63;
  margin-bottom: 10rpx;
}
.category-desc {
  font-size: 22rpx;
  line-height: 1.7;
  color: #667b90;
}
.process-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}
.process-item {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
}
.process-index {
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
.process-body {
  flex: 1;
}
.process-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #16324f;
  margin-bottom: 8rpx;
}
.process-text {
  display: block;
  font-size: 22rpx;
  line-height: 1.7;
  color: #6a7f94;
}
.companion-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.companion-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  border: 1rpx solid #edf3fa;
  border-radius: 20rpx;
  padding: 18rpx;
}
.companion-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.companion-body {
  display: flex;
  flex-direction: column;
}
.companion-name {
  font-size: 28rpx;
  font-weight: 700;
  color: #16324f;
  margin-bottom: 8rpx;
}
.companion-meta {
  font-size: 22rpx;
  color: #6f8194;
  line-height: 1.6;
}
.note-panel {
  margin-bottom: 0;
}
.note-text {
  display: block;
  font-size: 22rpx;
  line-height: 1.8;
  color: #647789;
}
</style>
