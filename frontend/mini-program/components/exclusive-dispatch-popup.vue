<template>
  <view v-if="store.visible && store.popup" class="popup-overlay" @click="handleLater">
    <view class="popup-shell" @click.stop>
      <view class="popup-hero">
        <view class="hero-icon-wrap">
          <image class="hero-icon" :src="orderActive" mode="aspectFit" />
        </view>
        <view class="hero-copy">
          <text class="hero-title">收到专属派单</text>
          <text class="hero-subtitle">{{ popup.serviceType }}</text>
        </view>
        <view class="countdown-chip">
          <text class="countdown-label">剩余</text>
          <text class="countdown-value">{{ popup.countdownText }}</text>
        </view>
      </view>

      <view class="popup-body">
        <view class="summary-card">
          <view class="summary-row">
            <text class="summary-label">医院</text>
            <text class="summary-value">{{ popup.hospital }}</text>
          </view>
          <view class="summary-row">
            <text class="summary-label">预约时间</text>
            <text class="summary-value">{{ popup.appointmentTime }}</text>
          </view>
          <view class="summary-row">
            <text class="summary-label">订单号</text>
            <text class="summary-value">{{ popup.orderNo || '--' }}</text>
          </view>
        </view>

        <view class="patient-card">
          <view class="patient-top">
            <view>
              <text class="patient-label">患者信息</text>
              <text class="patient-name">{{ popup.patientName }}</text>
            </view>
            <view class="income-block">
              <text class="income-label">预计收入</text>
              <text class="income-value">¥{{ popup.incomeText }}</text>
            </view>
          </view>
          <view class="patient-tags">
            <text class="patient-tag">{{ popup.patientAge }}岁</text>
            <text class="patient-tag">{{ popup.patientSex }}</text>
          </view>
        </view>

        <view class="popup-hint">
          <text>请尽快确认接单，超时后订单将自动释放到公共派单池。</text>
        </view>
      </view>

      <view class="reject-confirm" v-if="showRejectConfirm">
        <text class="reject-title">确认暂不接这单？</text>
        <text class="reject-desc">拒绝后订单会回到公共接单大厅，由其他陪诊师接单。</text>
        <view class="reject-actions">
          <button class="reject-btn ghost" @click="showRejectConfirm = false">再看看</button>
          <button class="reject-btn danger" :disabled="store.actionLoading" @click="handleReject">
            {{ store.actionLoading ? '处理中...' : '确认拒绝' }}
          </button>
        </view>
      </view>

      <view class="popup-actions">
        <button class="action-btn primary" :disabled="store.actionLoading" @click="handleAccept">
          {{ store.actionLoading ? '接单中...' : '立即接单' }}
        </button>
        <button class="action-btn secondary" @click="handleViewDetail">查看详情</button>
        <button class="action-btn danger" :disabled="store.actionLoading" @click="showRejectConfirm = true">拒绝派单</button>
      </view>

      <view class="later-wrap">
        <text class="later-link" @click="handleLater">稍后处理</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

import { orderActive } from '@/utils/assets.js'
import { useExclusiveDispatchStore } from '@/stores/exclusive-dispatch.js'

const store = useExclusiveDispatchStore()
const popup = computed(() => store.popup || {})
const showRejectConfirm = ref(false)

onMounted(() => {
  store.ensureInitialized()
  store.refreshPendingExclusiveOrders()
})

const handleAccept = async () => {
  showRejectConfirm.value = false
  await store.acceptExclusiveOrder()
}

const handleViewDetail = async () => {
  showRejectConfirm.value = false
  await store.viewOrderDetail()
}

const handleReject = async () => {
  const result = await store.rejectExclusiveOrder()
  if (result?.ok || result?.terminal) {
    showRejectConfirm.value = false
  }
}

const handleLater = () => {
  showRejectConfirm.value = false
  store.closePopup({ ignore: true })
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(18, 50, 90, 0.34);
  backdrop-filter: blur(8rpx);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
  box-sizing: border-box;
}

.popup-shell {
  width: 100%;
  max-width: 640rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 24rpx 60rpx rgba(18, 56, 109, 0.18);
}

.popup-hero {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 26rpx 24rpx;
  background: linear-gradient(135deg, #007aff, #2563eb);
  color: #ffffff;
}

.hero-icon-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero-icon {
  width: 36rpx;
  height: 36rpx;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #ffffff;
}

.hero-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.88);
}

.countdown-chip {
  min-width: 116rpx;
  padding: 12rpx 16rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.16);
  text-align: center;
  flex-shrink: 0;
}

.countdown-label {
  display: block;
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.82);
}

.countdown-value {
  display: block;
  margin-top: 4rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #ffffff;
}

.popup-body {
  padding: 24rpx;
}

.summary-card,
.patient-card {
  background: #f8fbff;
  border: 1rpx solid #dfebfb;
  border-radius: 18rpx;
  padding: 20rpx 22rpx;
}

.patient-card {
  margin-top: 18rpx;
}

.summary-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.summary-row + .summary-row {
  margin-top: 14rpx;
}

.summary-label {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #6a7f97;
}

.summary-value {
  flex: 1;
  text-align: right;
  font-size: 25rpx;
  line-height: 1.45;
  color: #18324f;
  word-break: break-all;
}

.patient-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.patient-label {
  display: block;
  font-size: 22rpx;
  color: #6a7f97;
}

.patient-name {
  display: block;
  margin-top: 8rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #16324f;
}

.income-block {
  text-align: right;
  flex-shrink: 0;
}

.income-label {
  display: block;
  font-size: 22rpx;
  color: #6a7f97;
}

.income-value {
  display: block;
  margin-top: 8rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #2563eb;
}

.patient-tags {
  display: flex;
  gap: 12rpx;
  margin-top: 16rpx;
  flex-wrap: wrap;
}

.patient-tag {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #eaf3ff;
  color: #2563eb;
  font-size: 22rpx;
  line-height: 1;
}

.popup-hint {
  margin-top: 18rpx;
  padding: 18rpx 20rpx;
  border-radius: 16rpx;
  background: #fff7e8;
  color: #9a6a12;
  font-size: 23rpx;
  line-height: 1.6;
}

.popup-actions {
  padding: 0 24rpx;
}

.reject-confirm {
  margin: 0 24rpx 20rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #fff5f5;
  border: 1rpx solid #ffd6d6;
}

.reject-title {
  display: block;
  font-size: 27rpx;
  font-weight: 700;
  color: #9f1d1d;
}

.reject-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.55;
  color: #8b5a5a;
}

.reject-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 18rpx;
}

.reject-btn {
  height: 72rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  font-weight: 700;
  border: none;
}

.reject-btn::after {
  border: none;
}

.reject-btn.ghost {
  background: #ffffff;
  color: #31536f;
  border: 1rpx solid #e1ebf5;
}

.reject-btn.danger {
  background: #ffe4e4;
  color: #d94848;
}

.action-btn {
  width: 100%;
  height: 88rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
  border: none;
}

.action-btn::after {
  border: none;
}

.action-btn.primary {
  @include escort-primary-btn;
}

.action-btn.secondary {
  margin-top: 16rpx;
  background: #f2f7ff;
  color: #2563eb;
  border: 1rpx solid #cdddf8;
}

.action-btn.danger {
  margin-top: 16rpx;
  background: #fff5f5;
  color: #d94848;
  border: 1rpx solid #ffd0d0;
}

.action-btn[disabled] {
  opacity: 0.7;
}

.later-wrap {
  padding: 20rpx 24rpx 24rpx;
  text-align: center;
}

.later-link {
  font-size: 24rpx;
  color: #6b7f96;
}
</style>
