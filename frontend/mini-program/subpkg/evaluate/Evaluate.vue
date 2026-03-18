<template>
  <view class="evaluate-page">
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="error" class="error">{{ error }}</view>

    <view v-else class="content">
      <view class="card">
        <view class="card-title">订单信息</view>
        <view class="row">
          <text class="label">订单编号</text>
          <text class="value">{{ orderNo }}</text>
        </view>
        <view class="row">
          <text class="label">服务类型</text>
          <text class="value">{{ orderInfo.serviceTypeName || '陪诊服务' }}</text>
        </view>
        <view class="row">
          <text class="label">就诊时间</text>
          <text class="value">{{ orderInfo.serviceDate }} {{ orderInfo.serviceTime }}</text>
        </view>
        <view class="row">
          <text class="label">就诊医院</text>
          <text class="value">{{ orderInfo.hospital || '-' }}</text>
        </view>
        <view class="row">
          <text class="label">陪诊师</text>
          <text class="value">{{ orderInfo.attendantName || '待分配' }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title">整体服务评分</view>
        <view class="stars">
          <text
            v-for="i in 5"
            :key="i"
            class="star"
            :class="{ active: rating >= i }"
            @click="setRating(i)"
          >★</text>
        </view>
        <text class="tip">请为本次陪诊服务打分（必填）</text>
      </view>

      <view class="card">
        <view class="card-title">服务亮点</view>
        <view class="tags">
          <view
            v-for="tag in tags"
            :key="tag"
            class="tag"
            :class="{ selected: selectedTags.includes(tag) }"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title">评价内容</view>
        <view class="textarea-wrapper">
          <textarea
            v-model="content"
            :maxlength="200"
            class="textarea"
            placeholder="可以说说陪诊师的服务态度、专业性、沟通等方面感受…"
          />
          <text class="char-count">{{ content.length }}/200</text>
        </view>
      </view>

      <view class="bottom-space"></view>
    </view>

    <view class="bottom-submit-bar">
      <button class="submit-btn" @click="submit">提交评价</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post } from '@/utils/api.js'

const orderNo = ref('')
const orderId = ref(null)
const loading = ref(true)
const error = ref('')
const orderInfo = ref({})

const rating = ref(0)
const content = ref('')
const tags = ref(['服务专业', '沟通耐心', '时间准时', '人很亲切', '路线熟悉'])
const selectedTags = ref([])

onLoad((options) => {
  if (options && options.orderNo) {
    orderNo.value = decodeURIComponent(options.orderNo)
  }
  if (!orderNo.value) {
    error.value = '缺少订单编号'
    loading.value = false
    return
  }
  loadOrderInfo()
})

const loadOrderInfo = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await get(`/ai/guide/orders/${orderNo.value}/complete-info`)
    if (res && res.code === 200 && res.data) {
      orderInfo.value = res.data
      orderId.value = res.data.orderId
      await loadEvaluation()
    } else {
      error.value = '获取订单信息失败'
    }
  } catch (e) {
    console.error('加载订单信息失败', e)
    error.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

const loadEvaluation = async () => {
  if (!orderId.value) return
  try {
    const res = await get(`/api/orders/${orderId.value}/evaluation`)
    if (res && res.code === 200 && res.data) {
      const eva = res.data
      if (eva.rating) rating.value = eva.rating
      if (eva.tags) selectedTags.value = eva.tags.split(',').filter(Boolean)
      if (eva.content) content.value = eva.content
    }
  } catch (e) {
    console.error('加载评价信息失败', e)
  }
}

const setRating = (val) => {
  rating.value = val
}

const toggleTag = (tag) => {
  const list = selectedTags.value
  const idx = list.indexOf(tag)
  if (idx >= 0) list.splice(idx, 1)
  else list.push(tag)
}

const submit = () => {
  if (rating.value === 0) {
    uni.showToast({ title: '请先打个星级评分', icon: 'none' })
    return
  }
  if (!orderId.value) {
    uni.showToast({ title: '订单信息有误', icon: 'none' })
    return
  }
  const payload = {
    rating: rating.value,
    tags: selectedTags.value.join(','),
    content: content.value
  }
  post(`/api/orders/${orderId.value}/evaluation`, payload)
    .then(res => {
      if (res && res.code === 200) {
        if (orderNo.value) {
          uni.setStorageSync(`order_evaluated_${orderNo.value}`, '1')
        }
        uni.showToast({ title: '评价已提交', icon: 'success' })
        setTimeout(() => {
          if (orderNo.value) {
            uni.redirectTo({
              url: `/subpkg/order/order-detail?orderNo=${encodeURIComponent(orderNo.value)}`
            })
          } else {
            uni.navigateBack()
          }
        }, 800)
      } else {
        uni.showToast({ title: (res && res.message) || '提交失败', icon: 'none' })
      }
    })
    .catch(e => {
      console.error('提交评价失败', e)
      uni.showToast({ title: '提交失败，请稍后重试', icon: 'none' })
    })
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.evaluate-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20rpx 32rpx 140rpx;
  box-sizing: border-box;
}
.loading,
.error {
  text-align: center;
  margin: 100rpx auto;
  color: #666;
}
.error {
  color: #ff4d4f;
}
.content {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.06);
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16rpx;
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8rpx 0;
}
.label {
  font-size: 26rpx;
  color: #6b7280;
}
.value {
  font-size: 26rpx;
  color: #111827;
  max-width: 60%;
  text-align: right;
}
.stars {
  display: flex;
  gap: 8rpx;
  margin: 8rpx 0 4rpx;
}
.star {
  font-size: 40rpx;
  color: #d1d5db;
}
.star.active {
  color: #facc15;
}
.tip {
  font-size: 24rpx;
  color: #9ca3af;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.tag {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  border: 1rpx solid #149DE4;
  font-size: 24rpx;
  color: #149DE4;
}
.tag.selected {
  background-color: #149DE4;
  color: #fff;
}
.textarea-wrapper {
  position: relative;
}
.textarea {
  width: 100%;
  height: 150rpx;
  font-size: 26rpx;
  line-height: 1.5;
  padding: 20rpx;
  border: 1rpx solid #ddd;
  border-radius: 12rpx;
  background: #f9fafb;
  box-sizing: border-box;
}
.char-count {
  font-size: 22rpx;
  color: #9ca3af;
  text-align: right;
  margin-top: 8rpx;
}
.bottom-space {
  height: 40rpx;
}
.bottom-submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 32rpx 32rpx;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 -4rpx 16rpx rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
}
.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  background: linear-gradient(135deg, #66A6FF, #4F95F0);
  color: #fff;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}
</style>

