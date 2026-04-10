<template>
  <view class="test-page">
    <view class="header">
      <text class="title">订单确认页面数据源测试</text>
    </view>
    
    <view class="content">
      <!-- 获取最新预约信息 -->
      <view class="section">
        <button class="btn-primary" @click="getLatestAppointment">获取最新预约信息</button>
        <view v-if="latestAppointment" class="result">
          <text>预约编号: {{ latestAppointment.appointmentNo }}</text>
          <text>医院名称: {{ latestAppointment.hospitalName }}</text>
          <text>服务日期: {{ latestAppointment.serviceDate }}</text>
          <text>服务类型: {{ latestAppointment.serviceTypeNumber }}</text>
          <text>患者姓名: {{ latestAppointment.patientName }}</text>
        </view>
      </view>
      
      <!-- 测试预约详情接口 -->
      <view class="section">
        <button class="btn-primary" @click="testAppointmentDetail" :disabled="!latestAppointment">测试预约详情接口</button>
        <view v-if="appointmentDetail" class="result">
          <text>医院: {{ appointmentDetail.hospital }}</text>
          <text>患者: {{ appointmentDetail.patientName }}</text>
          <text>日期: {{ appointmentDetail.serviceDate }}</text>
          <text>时间: {{ appointmentDetail.serviceTime }}</text>
          <text>服务类型编号: {{ appointmentDetail.serviceTypeNumber }}</text>
          <text>总价: ¥{{ appointmentDetail.totalPrice }}</text>
        </view>
      </view>
      
      <!-- 测试订单详情接口 -->
      <view class="section">
        <button class="btn-primary" @click="testOrderDetail">测试订单详情接口</button>
        <view v-if="orderDetail" class="result">
          <text>医院: {{ orderDetail.hospital }}</text>
          <text>患者: {{ orderDetail.patientName }}</text>
          <text>日期: {{ orderDetail.serviceDate }}</text>
          <text>时间: {{ orderDetail.serviceTime }}</text>
          <text>服务类型编号: {{ orderDetail.serviceTypeNumber }}</text>
          <text>总价: ¥{{ orderDetail.totalPrice }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { get, post } from '@/utils/api.js'

const latestAppointment = ref(null)
const appointmentDetail = ref(null)
const orderDetail = ref(null)

// 获取最新预约信息
const getLatestAppointment = async () => {
  try {
    const response = await get('/ai/guide/test/latest-appointment')
    console.log('获取最新预约信息:', response)
    if (response && response.success) {
      latestAppointment.value = response
      uni.showToast({
        title: '获取成功',
        icon: 'success'
      })
    } else {
      uni.showToast({
        title: response?.message || '获取失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('获取最新预约信息失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  }
}

// 测试预约详情接口
const testAppointmentDetail = async () => {
  if (!latestAppointment.value) return
  
  try {
    const response = await get(`/ai/guide/appointments/${latestAppointment.value.appointmentNo}`)
    console.log('预约详情接口响应:', response)
    if (response && response.code === 200) {
      appointmentDetail.value = response.data
      uni.showToast({
        title: '预约详情获取成功',
        icon: 'success'
      })
    } else {
      uni.showToast({
        title: '获取失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('测试预约详情接口失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  }
}

// 测试订单详情接口
const testOrderDetail = async () => {
  try {
    // 使用测试订单号
    const testOrderNo = 'ORD_TEST_1770738242941'
    const response = await get(`/ai/guide/orders/${testOrderNo}`)
    console.log('订单详情接口响应:', response)
    if (response && response.code === 200) {
      orderDetail.value = response.data
      uni.showToast({
        title: '订单详情获取成功',
        icon: 'success'
      })
    } else {
      uni.showToast({
        title: '获取失败',
        icon: 'none'
      })
    }
  } catch (error) {
    console.error('测试订单详情接口失败:', error)
    uni.showToast({
      title: '网络错误',
      icon: 'none'
    })
  }
}
</script>

<style scoped>
.test-page {
  padding: 20rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.header {
  text-align: center;
  padding: 40rpx 0;
  background: linear-gradient(135deg, #4A90E2, #357ABD);
  margin-bottom: 40rpx;
  border-radius: 16rpx;
}

.title {
  color: white;
  font-size: 36rpx;
  font-weight: bold;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.section {
  background: white;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}

.btn-primary {
  width: 100%;
  height: 80rpx;
  background-color: #007aff;
  color: white;
  border-radius: 12rpx;
  font-size: 32rpx;
  border: none;
  margin-bottom: 20rpx;
}

.btn-primary[disabled] {
  background-color: #cccccc;
  opacity: 0.6;
}

.result {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  padding: 20rpx;
  background-color: #f8f9fa;
  border-radius: 12rpx;
  margin-top: 20rpx;
}

.result text {
  font-size: 28rpx;
  color: #333;
  padding: 8rpx 0;
}
</style>