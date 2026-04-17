<template>
	<view class="container">
		<view class="ai-entry-card" @click="goToAiAppointment">
			<view class="ai-entry-copy">
				<text class="ai-entry-eyebrow">AI 帮我找</text>
				<text class="ai-entry-title">智能匹配最懂您的陪诊师</text>
				<text class="ai-entry-desc">如果您已经知道大致需求，可以先交给 AI 帮您筛人，再决定是否下单。</text>
			</view>
			<view class="ai-entry-action">
				<text class="ai-entry-btn">立即体验</text>
			</view>
		</view>

		<!-- 顶部标题 -->
		<view class="header">
			<text class="title">选择服务类型</text>
		</view>

		<!-- 服务类型选择 -->
		<view class="service-types">
			<view class="service-row">
				<view class="service-item" :class="{ 'selected': selectedService === 'general' }" @click="selectService('general')">
					<image class="service-icon" :src="serviceIcons.general" mode="aspectFit"></image>
					<text class="service-title">普通陪诊</text>
					<text class="service-desc">专业陪诊师 贴心服务</text>
				</view>
				<view class="service-item" :class="{ 'selected': selectedService === 'postop' }" @click="selectService('postop')">
					<image class="service-icon" :src="serviceIcons.postop" mode="aspectFit"></image>
					<text class="service-title">术后护理</text>
					<text class="service-desc">专业术后照顾 安心恢复</text>
				</view>
			</view>
			<view class="service-row">
				<view class="service-item" :class="{ 'selected': selectedService === 'emergency' }" @click="selectService('emergency')">
					<image class="service-icon" :src="serviceIcons.emergency" mode="aspectFit"></image>
					<text class="service-title">急诊陪同</text>
					<text class="service-desc">紧急情况 快速响应</text>
				</view>
				<view class="service-item" :class="{ 'selected': selectedService === 'home' }" @click="selectService('home')">
					<image class="service-icon" :src="serviceIcons.home" mode="aspectFit"></image>
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

		<!-- 底部按钮 -->
		<view class="bottom-button">
			<button class="next-btn" @click="goToNext">下一步</button>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { appointmentDetailIcons, appointmentServiceLogos } from '@/utils/assets.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'

const serviceIcons = {
	general: appointmentServiceLogos[1],
	postop: appointmentServiceLogos[2],
	emergency: appointmentServiceLogos[3],
	home: appointmentServiceLogos[4]
}

// 响应式数据
const selectedService = ref(null) // 存储选中的服务类型 key

// 服务详情数据
const serviceDetails = ref({
	general: {
		title: '普通陪诊服务详情',
		details: [
			{ icon: appointmentDetailIcons.time, text: '2小时起约，可根据需要延长' },
			{ icon: appointmentDetailIcons.ren1, text: '¥50/起步价，1小时可延长活动' },
			{ icon: appointmentDetailIcons.help, text: '医院内陪同，包括挂号、缴费、取药等' }
		]
	},
	postop: {
		title: '术后护理服务详情',
		details: [
			{ icon: appointmentDetailIcons.time, text: '24小时专业护理，术后恢复指导' },
			{ icon: appointmentDetailIcons.ren1, text: '¥80/起步价，专业护理人员' },
			{ icon: appointmentDetailIcons.help, text: '术后康复指导、伤口护理、用药提醒' }
		]
	},
	emergency: {
		title: '急诊陪同服务详情',
		details: [
			{ icon: appointmentDetailIcons.time, text: '24小时随时响应，紧急情况优先' },
			{ icon: appointmentDetailIcons.ren1, text: '¥100/起步价，急诊专业陪护' },
			{ icon: appointmentDetailIcons.help, text: '急诊科陪同、协助医生沟通、家属联系' }
		]
	},
	home: {
		title: '上门陪诊服务详情',
		details: [
			{ icon: appointmentDetailIcons.time, text: '预约上门，专业陪诊师到家服务' },
			{ icon: appointmentDetailIcons.ren1, text: '¥120/起步价，包含交通费用' },
			{ icon: appointmentDetailIcons.help, text: '上门接送、全程陪同、专业护理' }
		]
	}
})

// 生命周期钩子
onMounted(() => {
	// 页面加载完成后的逻辑
	restoreUserSelections()
})

onShow(() => {
	if (redirectPublicSafeToHome()) return
	// 页面显示时恢复用户选择状态
	restoreUserSelections()
})

// 保存用户选择到本地存储 (只保存服务类型)
const saveUserSelections = () => {
	// 将服务类型 key 映射为数字 (1-4)
	let serviceTypeNumber = 0;
	switch(selectedService.value) {
		case 'general': serviceTypeNumber = 1; break;
		case 'postop': serviceTypeNumber = 2; break;
		case 'emergency': serviceTypeNumber = 3; break;
		case 'home': serviceTypeNumber = 4; break;
		default: serviceTypeNumber = 0;
	}

	const selections = {
		appointment_type: serviceTypeNumber // 保存为数字 1,2,3,4
	}
	uni.setStorageSync('appointmentSelections', selections)
}

// 从本地存储恢复用户选择 (只恢复服务类型)
const restoreUserSelections = () => {
	try {
		const selections = uni.getStorageSync('appointmentSelections')
		if (selections && selections.appointment_type) {
			// 将数字映射回服务类型 key
			switch(selections.appointment_type) {
				case 1: selectedService.value = 'general'; break;
				case 2: selectedService.value = 'postop'; break;
				case 3: selectedService.value = 'emergency'; break;
				case 4: selectedService.value = 'home'; break;
				default: selectedService.value = null;
			}
		}
	} catch (e) {
		console.log('恢复用户选择失败:', e)
	}
}

// 选择服务类型
const selectService = (type) => {
	if (selectedService.value === type) {
		selectedService.value = null // 点击已选中项取消选择
	} else {
		selectedService.value = type // 选择新的服务类型
	}
	// 保存用户选择
	saveUserSelections()
}

// 跳转到下一步（预约表单页面）
const goToNext = () => {
	if (!selectedService.value) {
		uni.showToast({
			title: '请先选择服务类型',
			icon: 'none'
		})
		return
	}

	// 将服务类型数字和名称传递给下一个页面
	let serviceTypeNumber = 0;
	let serviceTypeName = '';
	switch(selectedService.value) {
		case 'general': serviceTypeNumber = 1; serviceTypeName = '普通陪诊'; break;
		case 'postop': serviceTypeNumber = 2; serviceTypeName = '术后护理'; break;
		case 'emergency': serviceTypeNumber = 3; serviceTypeName = '急诊陪同'; break;
		case 'home': serviceTypeNumber = 4; serviceTypeName = '上门陪诊'; break;
	}

	// 跳转到预约表单页面，并传递服务类型数据
	uni.navigateTo({
		url: '/subpkg/appointment-flow/02-appointment-form?serviceTypeNumber=' + serviceTypeNumber + '&serviceTypeName=' + encodeURIComponent(serviceTypeName)
	})
}

const goToAiAppointment = () => {
	uni.navigateTo({
		url: '/subpkg/ai-appointment/01-ai-appointment'
	})
}
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.container {
	min-height: 100vh;
	background-color: #f5f7fa;
	padding: 20rpx;
	padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
	overflow-x: clip;
}

.ai-entry-card {
	margin: 8rpx 8rpx 28rpx;
	padding: 28rpx;
	border-radius: 26rpx;
	background: linear-gradient(135deg, #edf5ff 0%, #ffffff 100%);
	box-shadow: 0 16rpx 36rpx rgba(66, 109, 190, 0.10);
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 24rpx;
}

.ai-entry-copy {
	flex: 1;
}

.ai-entry-eyebrow {
	display: block;
	font-size: 22rpx;
	font-weight: 700;
	color: #4b6fb7;
	letter-spacing: 2rpx;
}

.ai-entry-title {
	display: block;
	margin-top: 12rpx;
	font-size: 34rpx;
	font-weight: 700;
	color: #20324f;
}

.ai-entry-desc {
	display: block;
	margin-top: 12rpx;
	font-size: 24rpx;
	line-height: 1.6;
	color: #687998;
}

.ai-entry-action {
	flex-shrink: 0;
}

.ai-entry-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	padding: 18rpx 24rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, #3f84ff 0%, #6cb0ff 100%);
	color: #ffffff;
	font-size: 24rpx;
	font-weight: 700;
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

/* 服务类型选择 */
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
	border: 2rpx solid #007AFF;
	box-shadow: 0 6rpx 16rpx rgba(74, 144, 226, 0.2);
	transform: translateY(-2rpx);
}

.service-item.selected .service-title {
	color: #007AFF;
	font-weight: 600;
}

.service-item.selected .service-desc {
	color: #666;
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

/* 服务详情 */
.service-details {
	background-color: white;
	border-radius: 20rpx;
	padding: 40rpx;
	margin-bottom: 40rpx;
	box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
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

/* 底部按钮 */
.bottom-button {
	position: fixed;
	bottom: calc(60rpx + env(safe-area-inset-bottom));
	left: 0;
	right: 0;
	padding: 24rpx 30rpx calc(24rpx + env(safe-area-inset-bottom));
	background-color: white;
	border-top: 1rpx solid #eee;
	box-sizing: border-box;
	z-index: 20;
}

.next-btn {
	width: 100%;
	height: 88rpx;
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
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
