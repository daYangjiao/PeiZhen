<template>
  <view class="evaluate-page">
    <view v-if="loading" class="state-box">
      <text class="state-text">加载订单信息中...</text>
    </view>

    <view v-else-if="error" class="state-box error-box">
      <text class="state-text error-text">{{ error }}</text>
    </view>

    <view v-else class="content">
      <view class="card order-card">
        <view class="card-head">
          <text class="card-title">订单信息</text>
          <text class="order-no">{{ orderNo }}</text>
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
        <view class="card-head">
          <text class="card-title">整体服务评分</text>
          <text class="score-text" v-if="rating > 0">{{ rating }} 分</text>
        </view>
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
        <view class="card-head">
          <text class="card-title">服务亮点</text>
          <text class="tip-inline">可多选</text>
        </view>
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
        <view class="card-head">
          <text class="card-title">评价内容</text>
        </view>
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

    <view v-if="!loading && !error" class="bottom-submit-bar">
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
  background: linear-gradient(180deg, #edf4ff 0%, #f6f9ff 220rpx, #f5f7fa 100%);
  padding: 20rpx 24rpx 170rpx;
  box-sizing: border-box;
}

.state-box {
  margin-top: 180rpx;
  border-radius: 20rpx;
  padding: 40rpx 24rpx;
  text-align: center;
  background: #fff;
  border: 1rpx solid #e3ecfa;
}

.state-text {
  color: #6b7280;
  font-size: 28rpx;
}

.error-box {
  border-color: #f7d4d4;
  background: #fff8f8;
}

.error-text {
  color: #dc2626;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.card {
  background: #fff;
  border-radius: 22rpx;
  border: 1rpx solid #e3ecfa;
  padding: 22rpx;
  box-shadow: 0 10rpx 22rpx rgba(15, 23, 42, 0.06);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-bottom: 14rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f3f68;
}

.order-no {
  font-size: 22rpx;
  color: #6f83a5;
  max-width: 360rpx;
  text-align: right;
  word-break: break-all;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  padding: 11rpx 0;
  border-bottom: 1rpx solid #ecf1f9;
}

.row:last-child {
  border-bottom: none;
}

.label {
  font-size: 25rpx;
  color: #71839e;
  flex-shrink: 0;
}

.value {
  font-size: 25rpx;
  color: #1f2937;
  text-align: right;
  word-break: break-all;
}

.score-text {
  font-size: 26rpx;
  color: #2d73cf;
  font-weight: 600;
}

.stars {
  display: flex;
  gap: 8rpx;
  margin-top: 2rpx;
}

.star {
  font-size: 48rpx;
  color: #d5deeb;
  line-height: 1;
  min-width: 56rpx;
  text-align: center;
}

.star.active {
  color: #f5b93b;
}

.tip {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  color: #94a3b8;
}

.tip-inline {
  font-size: 22rpx;
  color: #94a3b8;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid #c9ddfb;
  background: #f4f9ff;
  font-size: 24rpx;
  color: #2d73cf;
}

.tag.selected {
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: #fff;
  border-color: transparent;
}

.textarea-wrapper {
  position: relative;
}

.textarea {
  width: 100%;
  min-height: 230rpx;
  font-size: 26rpx;
  line-height: 1.6;
  padding: 18rpx;
  border: 1rpx solid #d7e5fa;
  border-radius: 16rpx;
  background: #f8fbff;
  box-sizing: border-box;
}

.char-count {
  font-size: 22rpx;
  color: #9aa9be;
  text-align: right;
  margin-top: 8rpx;
  display: block;
}

.bottom-space {
  height: 24rpx;
}

.bottom-submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9;
  padding: 14rpx 24rpx calc(14rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.95);
  border-top: 1rpx solid #dbe7f8;
  box-shadow: 0 -6rpx 20rpx rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
}

.submit-btn {
  width: 100%;
  min-height: 90rpx;
  line-height: 90rpx;
  border-radius: 45rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  background: linear-gradient(135deg, #007AFF, #2563EB);
  color: #fff;
}
</style>
