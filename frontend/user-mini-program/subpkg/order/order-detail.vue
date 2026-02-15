<template>
	<view class="order-detail-page" :style="{ paddingTop: statusBarHeight + 'px' }">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="back-btn" @click="goBack">
					<text class="back-icon">‹</text>
				</view>
				<text class="navbar-title">订单详情</text>
				<view class="menu-btn">
					<text class="menu-icon">⋯</text>
				</view>
				<view class="record-btn">
					<view class="record-icon"></view>
				</view>
			</view>
		</view>

		<!-- 订单内容 -->
		<scroll-view class="content" scroll-y>
			<!-- 医院信息 -->
			<view class="hospital-section">
				<view class="hospital-header">
					<text class="hospital-name">{{ orderDetail.hospitalName }}</text>
					<view class="status-badge">
						<text class="status-text">进行中</text>
					</view>
				</view>
				<view class="time-info">
					<image class="time-icon" src="/static/time.png" mode="aspectFit"></image>
					<text class="time-text">{{ orderDetail.serviceDate }} {{ orderDetail.startTime }} - {{ orderDetail.endTime }}</text>
				</view>
			</view>

			<!-- 陪诊员信息 -->
			<view class="companion-section">
				<view class="companion-info">
					<image class="companion-avatar" :src="orderDetail.doctorAvatar" mode="aspectFill"></image>
					<text class="companion-name">{{ orderDetail.doctorName }}</text>
					<view class="contact-btn" @click="contactCompanion">
						<image class="contact-icon" src="/static/wechat-icon.png" mode="aspectFit"></image>
					</view>
				</view>
			</view>

			<!-- 预约信息 -->
			<view class="booking-section">
				<view class="info-item">
					<text class="label">预约人：</text>
					<text class="value">{{ orderDetail.patientName }}</text>
				</view>
				<view class="info-item">
					<text class="label">手机号：</text>
					<text class="value">{{ orderDetail.patientPhone }}</text>
				</view>
				<view class="info-item">
					<text class="label">服务类型：</text>
					<text class="value">{{ orderDetail.serviceType }}</text>
				</view>
				<view class="info-item">
					<text class="label">服务详情：</text>
					<text class="value">{{ orderDetail.serviceDetails }}</text>
				</view>
			</view>

			<!-- 服务流程 -->
			<view class="process-section">
				<view class="process-item" :class="{ active: currentStep >= 1 }">
					<view class="step-number">1</view>
					<text class="step-text">等待陪诊员接单</text>
				</view>
				<view class="process-item" :class="{ active: currentStep >= 2 }">
					<view class="step-number">2</view>
					<text class="step-text">陪诊员已接单</text>
				</view>
				<view class="process-item" :class="{ active: currentStep >= 3 }">
					<view class="step-number">3</view>
					<text class="step-text">正在陪诊</text>
				</view>
				<view class="process-item" :class="{ active: currentStep >= 4 }">
					<view class="step-number">4</view>
					<text class="step-text">服务完成</text>
				</view>
			</view>
		</scroll-view>

		<!-- 底部按钮 -->
		<view class="bottom-actions">
			<view class="cancel-btn" @click="cancelOrder">
				<text class="btn-text">取消订单</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useOrderStore } from '@/stores/order.js'

// 使用订单状态管理
const orderStore = useOrderStore()

// 系统信息
const statusBarHeight = ref(0)

// 订单详情
const orderDetail = ref({})
const currentStep = ref(1)

// 页面加载时获取订单详情
onLoad((options) => {
	if (options.orderId) {
		// 根据订单ID获取订单详情
		const order = orderStore.getOrderById(options.orderId)
		if (order) {
			orderDetail.value = order
			// 根据订单状态设置当前步骤
			switch (order.status) {
				case 'pending':
					currentStep.value = 1
					break
				case 'ongoing':
					currentStep.value = 2
					break
				case 'completed':
					currentStep.value = 4
					break
				default:
					currentStep.value = 1
			}
		}
	}
})

// 生命周期
onMounted(() => {
	// 获取系统信息
	const systemInfo = uni.getSystemInfoSync()
	statusBarHeight.value = systemInfo.statusBarHeight || 0
})

// 返回上一页
const goBack = () => {
	uni.navigateBack()
}

// 联系陪诊员
const contactCompanion = () => {
	uni.showToast({
		title: '联系陪诊员',
		icon: 'none'
	})
}

// 取消订单
const cancelOrder = () => {
	uni.showModal({
		title: '确认取消',
		content: '确定要取消这个订单吗？',
		success: (res) => {
			if (res.confirm) {
				// 调用订单store的取消订单方法
				orderStore.cancelOrder(orderDetail.value.id)
				uni.showToast({
					title: '订单已取消',
					icon: 'success'
				})
				// 返回订单列表页面
				setTimeout(() => {
					uni.navigateBack()
				}, 1500)
			}
		}
	})
}
</script>

<style scoped>
.order-detail-page {
	min-height: 100vh;
	background-color: #f8f9fa;
	display: flex;
	flex-direction: column;
}

/* 自定义导航栏 */
.custom-navbar {
	height: 88rpx;
	background-color: white;
	display: flex;
	align-items: center;
	justify-content: center;
	position: relative;
	border-bottom: 1rpx solid #f0f0f0;
}

.navbar-content {
	display: flex;
	align-items: center;
	justify-content: space-between;
	width: 100%;
	padding: 0 32rpx;
}

.back-btn {
	width: 60rpx;
	height: 60rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-icon {
	font-size: 48rpx;
	color: #333;
	font-weight: bold;
}

.navbar-title {
	font-size: 36rpx;
	font-weight: 600;
	color: #333;
	position: absolute;
	left: 50%;
	transform: translateX(-50%);
}

.menu-btn, .record-btn {
	width: 60rpx;
	height: 60rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.menu-icon {
	font-size: 32rpx;
	color: #333;
}

.record-icon {
	width: 32rpx;
	height: 32rpx;
	background-color: #333;
	border-radius: 50%;
}

/* 内容区域 */
.content {
	flex: 1;
	padding: 24rpx;
}

/* 医院信息 */
.hospital-section {
	background-color: white;
	border-radius: 16rpx;
	padding: 32rpx;
	margin-bottom: 24rpx;
}

.hospital-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 24rpx;
}

.hospital-name {
	font-size: 36rpx;
	font-weight: 600;
	color: #333;
}

.status-badge {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	border-radius: 24rpx;
	padding: 8rpx 16rpx;
}

.status-text {
	font-size: 24rpx;
	color: white;
	font-weight: 500;
}

.time-info {
	display: flex;
	align-items: center;
}

.time-icon {
	width: 32rpx;
	height: 32rpx;
	margin-right: 16rpx;
}

.time-text {
	font-size: 28rpx;
	color: #666;
}

/* 陪诊员信息 */
.companion-section {
	background-color: white;
	border-radius: 16rpx;
	padding: 32rpx;
	margin-bottom: 24rpx;
}

.companion-info {
	display: flex;
	align-items: center;
}

.companion-avatar {
	width: 80rpx;
	height: 80rpx;
	border-radius: 50%;
	margin-right: 24rpx;
}

.companion-name {
	font-size: 32rpx;
	font-weight: 500;
	color: #333;
	flex: 1;
}

.contact-btn {
	width: 60rpx;
	height: 60rpx;
	background-color: #f0f0f0;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.contact-icon {
	width: 32rpx;
	height: 32rpx;
}

/* 预约信息 */
.booking-section {
	background-color: white;
	border-radius: 16rpx;
	padding: 32rpx;
	margin-bottom: 24rpx;
}

.info-item {
	display: flex;
	margin-bottom: 16rpx;
}

.info-item:last-child {
	margin-bottom: 0;
}

.label {
	font-size: 28rpx;
	color: #666;
	width: 160rpx;
}

.value {
	font-size: 28rpx;
	color: #333;
	flex: 1;
}

/* 服务流程 */
.process-section {
	background-color: white;
	border-radius: 16rpx;
	padding: 32rpx;
	margin-bottom: 0;
}

.process-item {
	display: flex;
	align-items: center;
	margin-bottom: 32rpx;
	position: relative;
}

.process-item:last-child {
	margin-bottom: 0;
}

.process-item:not(:last-child)::after {
	content: '';
	position: absolute;
	left: 24rpx;
	top: 48rpx;
	width: 2rpx;
	height: 32rpx;
	background-color: #e0e0e0;
}

.process-item.active .step-number {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	color: white;
}

.process-item.active .step-text {
	color: #333;
	font-weight: 500;
}

.process-item.active:not(:last-child)::after {
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.step-number {
	width: 48rpx;
	height: 48rpx;
	border-radius: 50%;
	background-color: #e0e0e0;
	color: #999;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 24rpx;
	font-weight: 500;
	margin-right: 24rpx;
}

.step-text {
	font-size: 28rpx;
	color: #999;
}

/* 底部按钮 */
.bottom-actions {
	padding: 16rpx 24rpx 24rpx 24rpx;
	background-color: white;
	border-top: 1rpx solid #f0f0f0;
}

.cancel-btn {
	width: 100%;
	height: 88rpx;
	background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
	border-radius: 44rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.btn-text {
	font-size: 32rpx;
	color: white;
	font-weight: 600;
}
</style>