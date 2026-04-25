<template>
  <view class="page">
    <view class="hero-card slide-up delay-1">
      <view class="hero-left">
        <image class="hero-icon" src="/static/xin.png" mode="aspectFit"></image>
        <view>
          <text class="hero-title">我的评价</text>
          <text class="hero-desc">及时回复评价，持续提升服务口碑</text>
        </view>
      </view>
      <view class="hero-btn" @click="loadReviews"><text>刷新</text></view>
    </view>

    <view class="summary-grid slide-up delay-2">
      <view class="summary-card">
        <text class="summary-label">平均评分</text>
        <text class="summary-value">{{ avgRating }}</text>
      </view>
      <view class="summary-card">
        <text class="summary-label">评价总数</text>
        <text class="summary-value">{{ totalCount }}</text>
      </view>
      <view class="summary-card">
        <text class="summary-label">待回复</text>
        <text class="summary-value">{{ pendingCount }}</text>
      </view>
    </view>

    <view class="filter-wrap slide-up delay-2">
      <view class="filter-item" :class="{ active: filterTab === 'all' }" @click="filterTab = 'all'">
        <text>全部</text>
      </view>
      <view class="filter-item" :class="{ active: filterTab === 'pending' }" @click="filterTab = 'pending'">
        <text>待回复 {{ pendingCount }}</text>
      </view>
      <view class="filter-item" :class="{ active: filterTab === 'replied' }" @click="filterTab = 'replied'">
        <text>已回复 {{ repliedCount }}</text>
      </view>
    </view>

    <view v-if="loading" class="state-card"><text>加载中...</text></view>
    <view v-else-if="filteredReviews.length === 0" class="state-card"><text>暂无评价</text></view>

    <view v-else class="list-wrap">
      <view class="review-card slide-up delay-3" v-for="item in filteredReviews" :key="item.orderId">
        <view class="card-head">
          <view class="head-left">
            <text class="service">{{ item.serviceTypeName || item.serviceContent || '陪诊服务' }}</text>
            <text class="order-no">订单 {{ item.orderNo || '--' }}</text>
          </view>
          <view class="head-right">
            <text class="date">{{ item.serviceDate || '--' }}</text>
          </view>
        </view>

        <view class="stars-row">
          <text class="star" v-for="n in 5" :key="n" :class="n <= item.rating ? 'on' : 'off'">★</text>
        </view>

        <view class="tag-wrap" v-if="item.tags && item.tags.length">
          <text class="tag-chip" v-for="(tag, idx) in item.tags" :key="idx">{{ tag }}</text>
        </view>

        <view class="content-box">
          <text class="content" v-if="item.content">{{ item.content }}</text>
          <text class="content muted" v-else>用户未填写文字评价</text>
        </view>

        <view class="reply-box replied" v-if="item.attendantReply">
          <text class="reply-title">我的回复</text>
          <text class="reply-text">{{ item.attendantReply }}</text>
        </view>

        <view class="reply-box" v-else>
          <textarea
            class="textarea"
            v-model="draftReply[item.orderId]"
            placeholder="请输入回复内容（200字内）"
            maxlength="200"
          />
          <view class="reply-foot">
            <text class="count">{{ (draftReply[item.orderId] || '').length }}/200</text>
            <view class="reply-btn" @click="submitReply(item.orderId)">
              <text>{{ submittingOrderId === item.orderId ? '提交中...' : '提交回复' }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { get, post } from '@/utils/api.js'
import { formatRatingScore } from '@/utils/rating.js'

const loading = ref(false)
const submittingOrderId = ref(null)
const reviews = ref([])
const draftReply = reactive({})
const filterTab = ref('all')

const totalCount = computed(() => reviews.value.length)
const pendingCount = computed(() => reviews.value.filter((item) => !item.attendantReply).length)
const repliedCount = computed(() => reviews.value.filter((item) => !!item.attendantReply).length)

const avgRating = computed(() => {
  if (!reviews.value.length) return formatRatingScore(null, 0)
  const sum = reviews.value.reduce((acc, item) => acc + Number(item.rating || 0), 0)
  return formatRatingScore(sum / reviews.value.length, reviews.value.length)
})

const filteredReviews = computed(() => {
  if (filterTab.value === 'pending') {
    return reviews.value.filter((item) => !item.attendantReply)
  }
  if (filterTab.value === 'replied') {
    return reviews.value.filter((item) => !!item.attendantReply)
  }
  return reviews.value
})

const normalizeTags = (tagsValue) => {
  if (!tagsValue) return []
  return String(tagsValue)
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

const loadReviews = async () => {
  const userInfo = uni.getStorageSync('userInfo')
  if (!userInfo || !userInfo.id) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const orderRes = await get('/attendant/orders', {
      attendantId: userInfo.id,
      orderStatus: 6,
      page: 0,
      size: 120
    })

    const orders = orderRes.code === 200 && orderRes.data && Array.isArray(orderRes.data.content)
      ? orderRes.data.content
      : []

    if (orders.length === 0) {
      reviews.value = []
      return
    }

    const evalResList = await Promise.all(
      orders.map((order) =>
        get('/attendant/orders/' + order.orderId + '/evaluation')
          .then((res) => ({ order, evaluation: res.code === 200 ? res.data : null }))
          .catch(() => ({ order, evaluation: null }))
      )
    )

    reviews.value = evalResList
      .filter((item) => item.evaluation)
      .map((item) => ({
        ...item.order,
        rating: item.evaluation.rating,
        content: item.evaluation.content,
        tags: normalizeTags(item.evaluation.tags),
        attendantReply: item.evaluation.attendantReply || ''
      }))
  } catch (error) {
    reviews.value = []
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const submitReply = async (orderId) => {
  const text = (draftReply[orderId] || '').trim()
  if (!text) {
    uni.showToast({ title: '请输入回复内容', icon: 'none' })
    return
  }
  if (submittingOrderId.value) return

  submittingOrderId.value = orderId
  try {
    const res = await post('/attendant/orders/' + orderId + '/evaluation/reply', { reply: text })
    if (res.code === 200) {
      const target = reviews.value.find((item) => item.orderId === orderId)
      if (target) target.attendantReply = text
      draftReply[orderId] = ''
      uni.showToast({ title: '回复成功', icon: 'success' })
    }
  } catch (error) {
    uni.showToast({ title: '回复失败', icon: 'none' })
  } finally {
    submittingOrderId.value = null
  }
}

onMounted(() => {
  loadReviews()
})
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.page {
  @include escort-page;
  min-height: 100vh;
  padding: 24rpx;
  box-sizing: border-box;
}

.hero-card {
  @include escort-card(24rpx);
  border: 1rpx solid #e5eefb;
  background: linear-gradient(135deg, #ffffff 0%, #f3f8ff 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.hero-left {
  display: flex;
  align-items: center;
}

.hero-icon {
  width: 56rpx;
  height: 56rpx;
  margin-right: 14rpx;
}

.hero-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

.hero-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.hero-btn {
  min-width: 96rpx;
  height: 54rpx;
  border-radius: 28rpx;
  border: 1rpx solid $escort-color-primary;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: $escort-color-primary;
    font-size: 24rpx;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-bottom: 14rpx;
}

.summary-card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 18rpx 14rpx;
  box-shadow: $escort-shadow-card;
}

.summary-label {
  display: block;
  font-size: 22rpx;
  color: #6b7280;
}

.summary-value {
  display: block;
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: $escort-color-primary;
}

.filter-wrap {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 10rpx;
  box-shadow: $escort-shadow-card;
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 14rpx;
}

.filter-item {
  flex: 1;
  height: 54rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: 24rpx;
    color: #111827;
  }
}

.filter-item.active {
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);

  text {
    color: #fff;
  }
}

.state-card {
  height: 220rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: $escort-shadow-card;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: #9ca3af;
    font-size: 24rpx;
  }
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.review-card {
  background: #ffffff;
  border-radius: 16rpx;
  padding: 22rpx;
  box-shadow: $escort-shadow-card;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.head-left {
  flex: 1;
  min-width: 0;
  margin-right: 12rpx;
}

.service {
  display: block;
  font-size: 28rpx;
  color: #111827;
  font-weight: 700;
}

.order-no {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #6b7280;
}

.head-right {
  min-width: 120rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.date {
  font-size: 22rpx;
  color: #9ca3af;
}

.stars-row {
  margin-top: 12rpx;
}

.star {
  font-size: 28rpx;
  margin-right: 4rpx;
}

.star.on {
  color: #f59e0b;
}

.star.off {
  color: #d1d5db;
}

.tag-wrap {
  margin-top: 10rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.tag-chip {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #edf5ff;
  color: $escort-color-primary;
  font-size: 22rpx;
}

.content-box {
  margin-top: 10rpx;
  padding: 14rpx;
  border-radius: 12rpx;
  background: #f8fafc;
}

.content {
  font-size: 24rpx;
  color: #374151;
  line-height: 1.6;
}

.content.muted {
  color: #9ca3af;
}

.reply-box {
  margin-top: 12rpx;
  padding: 14rpx;
  border-radius: 12rpx;
  background: #f8fafc;
}

.reply-box.replied {
  background: #eef6ff;
}

.reply-title {
  display: block;
  font-size: 22rpx;
  color: $escort-color-primary;
  margin-bottom: 6rpx;
}

.reply-text {
  font-size: 24rpx;
  color: #1f2937;
  line-height: 1.6;
}

.textarea {
  width: 100%;
  min-height: 88rpx;
  max-height: 140rpx;
  border-radius: 10rpx;
  background: #fff;
  padding: 10rpx 12rpx;
  box-sizing: border-box;
  font-size: 24rpx;
}

.reply-foot {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.count {
  color: #9ca3af;
  font-size: 22rpx;
}

.reply-btn {
  width: 176rpx;
  height: 64rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    color: #fff;
    font-size: 24rpx;
  }
}

.slide-up {
  opacity: 0;
  transform: translateY(20rpx);
  animation: slideUp 0.42s ease forwards;
}

.delay-1 {
  animation-delay: 0.02s;
}

.delay-2 {
  animation-delay: 0.08s;
}

.delay-3 {
  animation-delay: 0.14s;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
