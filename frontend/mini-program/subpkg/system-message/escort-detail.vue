<template>
  <view class="page">
    <view v-if="loading" class="state-card">
      <text class="state-text">加载中...</text>
    </view>

    <view v-else-if="loadError" class="state-card">
      <text class="state-title">消息暂时无法打开</text>
      <text class="state-text">{{ loadError }}</text>
      <view class="state-btn" @click="goBack">
        <text>返回列表</text>
      </view>
    </view>

    <template v-else>
      <view class="detail-card">
        <view class="detail-header">
          <text class="detail-title">系统通知</text>
          <text class="detail-time">{{ formatTime(detail.createTime) }}</text>
        </view>
        <text class="detail-content">{{ detail.content || '暂无消息内容' }}</text>
      </view>

      <view class="detail-card">
        <view class="section-header">
          <text class="section-title">关联订单</text>
          <text class="section-tag" :class="{ disabled: !detail.orderAvailable }">
            {{ detail.orderAvailable ? '可查看' : '不可查看' }}
          </text>
        </view>

        <view v-if="detail.orderAvailable && detail.order" class="order-summary">
          <view class="summary-row">
            <text class="label">订单号</text>
            <text class="value">{{ detail.order.orderNo || '--' }}</text>
          </view>
          <view class="summary-row">
            <text class="label">状态</text>
            <text class="value">{{ detail.order.orderStatusText || '--' }}</text>
          </view>
          <view class="summary-row">
            <text class="label">医院</text>
            <text class="value">{{ detail.order.hospital || '--' }}</text>
          </view>
          <view class="summary-row">
            <text class="label">就诊人</text>
            <text class="value">{{ detail.order.patientName || '--' }}</text>
          </view>
          <view class="summary-row">
            <text class="label">服务时间</text>
            <text class="value">{{ formatServiceTime(detail.order) }}</text>
          </view>

          <view class="primary-btn" @click="openOrderDetail">
            <text>查看订单详情</text>
          </view>
        </view>

        <view v-else class="order-empty">
          <text>{{ detail.orderUnavailableReason || '该消息未关联可查看订单' }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get } from '@/utils/api.js'

const loading = ref(true)
const loadError = ref('')
const detail = ref({})

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const formatServiceTime = (order = {}) => {
  const date = order.serviceDate || ''
  const slot = order.serviceTimeSlot || ''
  return [date, slot].filter(Boolean).join(' ')
}

const goBack = () => {
  uni.navigateBack()
}

const openOrderDetail = async () => {
  const orderId = Number(detail.value.order?.orderId || detail.value.orderId || 0)
  if (!detail.value.orderAvailable || !orderId) {
    uni.showToast({ title: detail.value.orderUnavailableReason || '订单暂时无法查看', icon: 'none' })
    return
  }
  try {
    await get(`/attendant/orders/${orderId}`)
    uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${orderId}` })
  } catch (error) {
    uni.showToast({ title: error?.message || '订单暂时无法查看', icon: 'none' })
  }
}

const loadDetail = async (messageId) => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await get(`/api/chat/system/${messageId}`)
    if (res.code === 200 && res.data) {
      detail.value = res.data
      if (res.data.markedRead) {
        uni.$emit('system-message:read', { messageId })
      }
      return
    }
    loadError.value = res.message || '消息不存在'
  } catch (error) {
    loadError.value = error?.message || '消息不存在或已删除'
    if (Number(error?.code || 0) === 401) {
      loadError.value = '无权限查看这条系统消息'
    }
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  const messageId = Number(options?.messageId || 0)
  if (!messageId) {
    loadError.value = '缺少消息ID'
    loading.value = false
    return
  }
  loadDetail(messageId)
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

.detail-card,
.state-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 12rpx 28rpx rgba(31, 41, 55, 0.06);
  margin-bottom: 20rpx;
}

.detail-header,
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.detail-title,
.section-title,
.state-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.detail-time {
  font-size: 22rpx;
  color: #94a3b8;
}

.detail-content,
.state-text,
.order-empty {
  font-size: 27rpx;
  line-height: 1.8;
  color: #475569;
  white-space: pre-wrap;
}

.section-tag {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
  font-size: 22rpx;
  font-weight: 600;
}

.section-tag.disabled {
  background: rgba(148, 163, 184, 0.14);
  color: #64748b;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}

.summary-row:last-child {
  border-bottom: none;
}

.label {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #6b7280;
}

.value {
  flex: 1;
  text-align: right;
  font-size: 26rpx;
  color: #111827;
  line-height: 1.6;
}

.primary-btn,
.state-btn {
  margin-top: 28rpx;
  height: 84rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #1677ff, #0f5fe5);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
