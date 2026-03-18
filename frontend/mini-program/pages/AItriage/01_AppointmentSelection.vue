<template>
	<view class="container">
		<!-- 顶部标题 -->
		<view class="header">
			<text class="title">选择服务类型</text>
		</view>

		<!-- 服务类型选择 -->
		<view class="service-types">
			<view class="service-row">
				<view class="service-item" :class="{ 'selected': selectedService === 'general' }" @click="selectService('general')">
					<image class="service-icon" src="/static/logo_1.png" mode="aspectFit"></image>
					<text class="service-title">普通陪诊</text>
					<text class="service-desc">专业陪诊师 贴心服务</text>
				</view>
				<view class="service-item" :class="{ 'selected': selectedService === 'postop' }" @click="selectService('postop')">
					<image class="service-icon" src="/static/logo_2.png" mode="aspectFit"></image>
					<text class="service-title">术后护理</text>
					<text class="service-desc">专业术后照顾 安心恢复</text>
				</view>
			</view>
			<view class="service-row">
				<view class="service-item" :class="{ 'selected': selectedService === 'emergency' }" @click="selectService('emergency')">
					<image class="service-icon" src="/static/logo_3.jpg" mode="aspectFit"></image>
					<text class="service-title">急诊陪同</text>
					<text class="service-desc">紧急情况 快速响应</text>
				</view>
				<view class="service-item" :class="{ 'selected': selectedService === 'home' }" @click="selectService('home')">
					<image class="service-icon" src="/static/logo_4.jpg" mode="aspectFit"></image>
					<text class="service-title">上门陪诊</text>
					<text class="service-desc">专门专业陪诊师 贴心陪伴</text>
				</view>
			</view>
		</view>

		<!-- 服务详情 -->
		<view class="service-details">
			<template v-if="selectedService">
				<text class="details-title">{{ serviceDetails[selectedService].title }}</text>
				<view class="detail-item" v-for="(detail, index) in serviceDetails[selectedService].details" :key="index">
					<image class="detail-icon" :src="detail.icon" mode="aspectFit"></image>
					<text class="detail-text">{{ detail.text }}</text>
				</view>
			</template>
			<template v-else>
				<text class="details-title">请选择服务类型</text>
				<view class="detail-item">
					<text class="detail-text">请先选择上方的服务类型以查看详细信息</text>
				</view>
			</template>
		</view>

		<!-- 底部按钮（注意：这是 Tab 页，不能用 fixed 遮住 TabBar） -->
		<view class="bottom-button">
			<button class="next-btn" @click="goToNext">下一步</button>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'

const selectedService = ref(null)

const serviceDetails = ref({
	general: {
		title: '普通陪诊服务详情',
		details: [
			{ icon: '/static/time.png', text: '2小时起约，可根据需要延长' },
			{ icon: '/static/ren_1.png', text: '¥50/起步价，1小时可延长活动' },
			{ icon: '/static/help.png', text: '医院内陪同，包括挂号、缴费、取药等' }
		]
	},
	postop: {
		title: '术后护理服务详情',
		details: [
			{ icon: '/static/time.png', text: '24小时专业护理，术后恢复指导' },
			{ icon: '/static/ren_1.png', text: '¥80/起步价，专业护理人员' },
			{ icon: '/static/help.png', text: '术后康复指导、伤口护理、用药提醒' }
		]
	},
	emergency: {
		title: '急诊陪同服务详情',
		details: [
			{ icon: '/static/time.png', text: '24小时随时响应，紧急情况优先' },
			{ icon: '/static/ren_1.png', text: '¥100/起步价，急诊专业陪护' },
			{ icon: '/static/help.png', text: '急诊科陪同、协助医生沟通、家属联系' }
		]
	},
	home: {
		title: '上门陪诊服务详情',
		details: [
			{ icon: '/static/time.png', text: '预约上门，专业陪诊师到家服务' },
			{ icon: '/static/ren_1.png', text: '¥120/起步价，包含交通费用' },
			{ icon: '/static/help.png', text: '上门接送、全程陪同、专业护理' }
		]
	}
})

const saveUserSelections = () => {
	let serviceTypeNumber = 0
	switch (selectedService.value) {
		case 'general': serviceTypeNumber = 1; break
		case 'postop': serviceTypeNumber = 2; break
		case 'emergency': serviceTypeNumber = 3; break
		case 'home': serviceTypeNumber = 4; break
		default: serviceTypeNumber = 0
	}
	uni.setStorageSync('appointmentSelections', { appointment_type: serviceTypeNumber })
}

const restoreUserSelections = () => {
	try {
		const selections = uni.getStorageSync('appointmentSelections')
		if (selections && selections.appointment_type) {
			switch (selections.appointment_type) {
				case 1: selectedService.value = 'general'; break
				case 2: selectedService.value = 'postop'; break
				case 3: selectedService.value = 'emergency'; break
				case 4: selectedService.value = 'home'; break
				default: selectedService.value = null
			}
		}
	} catch {}
}

const selectService = (type) => {
	selectedService.value = selectedService.value === type ? null : type
	saveUserSelections()
}

const goToNext = () => {
	if (!selectedService.value) {
		uni.showToast({ title: '请先选择服务类型', icon: 'none' })
		return
	}
	let serviceTypeNumber = 0
	let serviceTypeName = ''
	switch (selectedService.value) {
		case 'general': serviceTypeNumber = 1; serviceTypeName = '普通陪诊'; break
		case 'postop': serviceTypeNumber = 2; serviceTypeName = '术后护理'; break
		case 'emergency': serviceTypeNumber = 3; serviceTypeName = '急诊陪同'; break
		case 'home': serviceTypeNumber = 4; serviceTypeName = '上门陪诊'; break
	}
	uni.navigateTo({
		url: '/subpkg/appointment-flow/02_AppointmentForm?serviceTypeNumber=' + serviceTypeNumber + '&serviceTypeName=' + encodeURIComponent(serviceTypeName)
	})
}

onMounted(() => restoreUserSelections())
onShow(() => restoreUserSelections())
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.container {
	min-height: 100vh;
	background-color: #f5f7fa;
	padding: 20rpx;
	padding-bottom: 220rpx; /* 适当抬高“下一步”按钮，刚好浮在底部预约 Tab 上方 */
}

.header {
	text-align: center;
	padding: 40rpx 0;
}
.title {
	font-size: 36rpx;
	font-weight: bold;
	color: #333;
}

.service-types {
	margin-bottom: 40rpx;
	padding: 20rpx;
}
.service-row {
	display: flex;
	justify-content: space-between;
	margin-bottom: 20rpx;
	gap: 20rpx;
}
.service-item {
	flex: 1;
	height: 200rpx;
	background-color: white;
	border-radius: 16rpx;
	padding: 30rpx 20rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	text-align: center;
	transition: all 0.3s ease;
	border: 2rpx solid transparent;
}
.service-item.selected {
	background: linear-gradient(135deg, #f0f8ff 0%, #e6f3ff 100%);
	border: 2rpx solid #66a6ff;
	box-shadow: 0 6rpx 16rpx rgba(74, 144, 226, 0.2);
	transform: translateY(-2rpx);
}
.service-icon {
	width: 80rpx;
	height: 80rpx;
	margin-bottom: 20rpx;
}
.service-title {
	font-size: 28rpx;
	font-weight: bold;
	color: #333;
	margin-bottom: 10rpx;
}
.service-desc {
	font-size: 24rpx;
	color: #666;
	line-height: 1.4;
}

.service-details {
	background-color: white;
	border-radius: 20rpx;
	padding: 40rpx;
	margin-bottom: 40rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}
.details-title {
	font-size: 32rpx;
	font-weight: bold;
	color: #333;
	margin-bottom: 30rpx;
	display: block;
}
.detail-item {
	display: flex;
	align-items: center;
	margin-bottom: 20rpx;
}
.detail-icon {
	width: 40rpx;
	height: 40rpx;
	margin-right: 20rpx;
}
.detail-text {
	font-size: 28rpx;
	color: #666;
	line-height: 1.5;
}

.bottom-button {
	padding: 30rpx 0 0;
}
.next-btn {
	width: 100%;
	height: 88rpx;
	background: linear-gradient(135deg, #66A6FF 0%, #4F95F0 100%);
	color: white;
	font-size: 32rpx;
	font-weight: bold;
	border-radius: 44rpx;
	border: none;
	display: flex;
	align-items: center;
	justify-content: center;
}
.next-btn:active {
	opacity: 0.8;
}
</style>

