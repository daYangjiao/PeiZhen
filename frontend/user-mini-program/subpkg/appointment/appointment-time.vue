<template>
	<view class="appointment-time-container">
		<!-- 状态栏占位 -->
		<view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
		
		<!-- 顶部标题 -->
		<view class="header">
			<view class="back-btn" @click="goBack">
				<text class="back-icon">‹</text>
			</view>
			<text class="title">选择服务时间</text>
		</view>

		<!-- 选择日期 -->
		<view class="date-section">
			<picker 
				mode="date" 
				:value="selectedDate" 
				:start="minDate"
				@change="onDateChange"
				class="date-picker-wrapper"
			>
				<view class="date-btn">
					<text class="date-text">选择日期</text>
				</view>
			</picker>
			<view class="selected-date" v-if="!selectedDate">
				<text class="placeholder-text">未选择日期</text>
			</view>
			<view class="selected-date" v-else>
				<text class="date-value">{{ formatDate(selectedDate) }}</text>
			</view>
		</view>

		<!-- 选择服务时段 -->
		<view class="time-section">
			<view class="section-header">
				<view class="icon-wrapper">
					<text class="time-icon">🕐</text>
				</view>
				<text class="section-title">选择服务时段</text>
			</view>
			
			<view class="time-picker-row">
				<view class="time-picker" @click="showStartTimePicker">
					<text class="time-label">开始时间</text>
					<text class="time-value" v-if="startTime">{{ startTime }}</text>
					<text class="time-placeholder" v-else>请选择</text>
					<text class="time-arrow">▼</text>
				</view>
				<view class="time-picker" @click="showEndTimePicker">
					<text class="time-label">结束时间</text>
					<text class="time-value" v-if="endTime">{{ endTime }}</text>
					<text class="time-placeholder" v-else>请选择</text>
					<text class="time-arrow">▼</text>
				</view>
			</view>
		</view>

		<!-- 输入地址或选择医院 -->
		<view class="location-section">
			<view class="section-header">
				<view class="icon-wrapper">
					<text class="location-icon">📍</text>
				</view>
				<text class="section-title">输入地址或选择医院</text>
			</view>
			
			<view class="location-input">
				<input 
					v-model="hospitalAddress" 
					placeholder="输入地址或选择医院" 
					class="address-input"
				/>
			</view>
		</view>

		<!-- 联系信息 -->
		<view class="contact-section">
			<view class="input-group">
				<input 
					v-model="patientName" 
					placeholder="姓名" 
					class="contact-input"
				/>
			</view>
			<view class="input-group">
				<input 
					v-model="phoneNumber" 
					placeholder="手机号" 
					class="contact-input"
					type="number"
					maxlength="11"
					@blur="validatePhone"
				/>
				<text v-if="phoneError" class="error-text">{{ phoneError }}</text>
			</view>
		</view>

		<!-- 预约按钮 -->
		<view class="confirm-section">
			<button class="confirm-btn" @click="confirmAppointment">
				确认预约
			</button>
		</view>



		<!-- 时间选择器弹窗 -->
		<view class="modal-overlay" v-if="showTimeModal" @click="hideTimePicker">
			<view class="time-modal" @click.stop>
				<view class="modal-header">
					<text class="modal-title">{{ timePickerTitle }}</text>
					<text class="close-btn" @click="hideTimePicker">✕</text>
				</view>
				
				<view class="time-list">
					<view 
						v-for="time in timeOptions" 
						:key="time"
						:class="['time-option', { 'selected': selectedTime === time }]"
						@click="selectTime(time)"
					>
						<text>{{ time }}</text>
					</view>
				</view>
				
				<view class="modal-footer">
					<button class="cancel-btn" @click="hideTimePicker">取消</button>
					<button class="confirm-time-btn" @click="confirmTime">确定</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// 接收页面参数
const serviceInfo = ref({})

// 系统信息
const statusBarHeight = ref(0)

// 响应式数据
const selectedDate = ref('')
const startTime = ref('')
const endTime = ref('')
const hospitalAddress = ref('')
const patientName = ref('')
const phoneNumber = ref('')
const phoneError = ref('')
const isPhoneValid = ref(false)

// 弹窗控制
const showTimeModal = ref(false)
const timePickerType = ref('') // 'start' 或 'end'
const selectedTime = ref('')

// 计算属性
const minDate = computed(() => {
	const today = new Date()
	return today.toISOString().split('T')[0]
})

const timePickerTitle = computed(() => {
	return timePickerType.value === 'start' ? '选择开始时间' : '选择结束时间'
})

const timeOptions = computed(() => {
	const options = []
	for (let hour = 8; hour <= 18; hour++) {
		for (let minute = 0; minute < 60; minute += 30) {
			const timeStr = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
			options.push(timeStr)
		}
	}
	return options
})

// 方法
const goBack = () => {
	// 获取当前页面栈
	const pages = getCurrentPages()
	if (pages.length >= 2) {
		// 获取上一个页面实例
		const prevPage = pages[pages.length - 2]
		// 如果上一个页面是预约页面，保持其选择状态
		if (prevPage.route === 'pages/appointment/appointment') {
			// 通过页面实例直接访问其数据，保持选择状态
			// 这样返回时用户的选择会被保留
		}
	}
	uni.navigateBack()
}

const onDateChange = (e) => {
	selectedDate.value = e.detail.value
}

const showStartTimePicker = () => {
	timePickerType.value = 'start'
	selectedTime.value = startTime.value
	showTimeModal.value = true
}

const showEndTimePicker = () => {
	timePickerType.value = 'end'
	selectedTime.value = endTime.value
	showTimeModal.value = true
}

const hideTimePicker = () => {
	showTimeModal.value = false
	selectedTime.value = ''
}

const selectTime = (time) => {
	selectedTime.value = time
}

const confirmTime = () => {
	if (timePickerType.value === 'start') {
		// 如果已选择结束时间，检查开始时间是否小于结束时间
		if (endTime.value && selectedTime.value >= endTime.value) {
			uni.showToast({
				title: '开始时间必须小于结束时间',
				icon: 'none'
			})
			return
		}
		startTime.value = selectedTime.value
	} else {
		// 检查结束时间是否大于开始时间
		if (startTime.value && selectedTime.value <= startTime.value) {
			uni.showToast({
				title: '结束时间必须大于开始时间',
				icon: 'none'
			})
			return
		}
		endTime.value = selectedTime.value
	}
	hideTimePicker()
}

const formatDate = (dateStr) => {
	if (!dateStr) return ''
	const date = new Date(dateStr)
	const year = date.getFullYear()
	const month = date.getMonth() + 1
	const day = date.getDate()
	return `${year}年${month}月${day}日`
}

const validatePhone = () => {
	const phoneRegex = /^1[3-9]\d{9}$/
	if (!phoneNumber.value) {
		phoneError.value = ''
		isPhoneValid.value = false
		return
	}
	
	if (phoneRegex.test(phoneNumber.value)) {
		phoneError.value = ''
		isPhoneValid.value = true
	} else {
		phoneError.value = '请输入正确的手机号格式（11位数字，以1开头）'
		isPhoneValid.value = false
	}
}

const confirmAppointment = () => {
	// 验证必填字段
	if (!selectedDate.value) {
		uni.showToast({
			title: '请选择服务日期',
			icon: 'none'
		})
		return
	}
	
	if (!startTime.value || !endTime.value) {
		uni.showToast({
			title: '请选择服务时间',
			icon: 'none'
		})
		return
	}
	
	if (!hospitalAddress.value) {
		uni.showToast({
			title: '请输入医院地址',
			icon: 'none'
		})
		return
	}
	
	if (!patientName.value) {
		uni.showToast({
			title: '请输入患者姓名',
			icon: 'none'
		})
		return
	}
	
	if (!phoneNumber.value) {
		uni.showToast({
			title: '请输入联系电话',
			icon: 'none'
		})
		return
	}
	
	// 验证手机号格式
	validatePhone()
	if (!isPhoneValid.value) {
		uni.showToast({
			title: phoneError.value || '请输入正确的手机号格式',
			icon: 'none'
		})
		return
	}
	
	// 提交预约信息
	uni.showToast({
		title: '预约成功',
		icon: 'success'
	})
	
	// 准备订单数据
	const orderData = {
		hospitalName: hospitalAddress.value,
		serviceDate: selectedDate.value,
		startTime: startTime.value,
		endTime: endTime.value,
		doctorName: serviceInfo.value?.companion?.name || '陪诊员',
		doctorAvatar: serviceInfo.value?.companion?.avatar || '/static/doctor1.jpg',
		patientName: patientName.value,
		patientPhone: phoneNumber.value,
		serviceType: '普通陪诊',
		serviceDetails: '医院内陪诊，包括挂号、取药、检查等'
	}
	
	// 清除保存的预约选择状态
	uni.removeStorageSync('appointmentSelections')
	
	// 先保存订单数据到本地存储
			setTimeout(() => {
				try {
					// 将订单数据保存到本地存储
					uni.setStorageSync('newOrderData', JSON.stringify(orderData))
					
					// 跳转到订单页面（tabBar页面使用switchTab）
					uni.switchTab({
						url: '/pages/order/order'
					})
				} catch (e) {
					console.error('保存订单数据失败:', e)
					uni.showToast({
						title: '跳转失败，请重试',
						icon: 'none'
					})
				}
			}, 1500)
}

onMounted(() => {
	// 获取系统信息
	uni.getSystemInfo({
		success: (res) => {
			statusBarHeight.value = res.statusBarHeight
		}
	})
	
	// 获取页面参数
	const pages = getCurrentPages()
	const currentPage = pages[pages.length - 1]
	const options = currentPage.options
	
	if (options.service) {
		try {
			serviceInfo.value = JSON.parse(decodeURIComponent(options.service))
			console.log('接收到的服务信息:', serviceInfo.value)
		} catch (e) {
			console.error('解析服务信息失败:', e)
		}
	}
})
</script>

<style scoped>
.appointment-time-container {
	min-height: 100vh;
	background-color: #f5f5f5;
	padding-bottom: 120rpx;
}

/* 状态栏占位 */
.status-bar {
	background-color: #ffffff;
}

/* 顶部标题 */
.header {
	padding: 20rpx;
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #fff;
}

.back-btn {
	position: absolute;
	left: 20rpx;
	width: 80rpx;
	height: 80rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.back-icon {
	font-size: 50rpx;
	color: #666;
	font-weight: normal;
}

.title {
	font-size: 36rpx;
	font-weight: bold;
	color: #333;
}

/* 日期选择区域 */
.date-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
	text-align: center;
}

.date-picker-wrapper {
	display: inline-block;
	margin-bottom: 30rpx;
}

.date-btn {
	background: linear-gradient(135deg, #4A90E2, #357ABD);
	color: white;
	padding: 20rpx 60rpx;
	border-radius: 50rpx;
	display: inline-block;
}

.date-text {
	font-size: 32rpx;
	font-weight: 600;
}

.selected-date {
	padding: 10rpx 0;
}

.placeholder-text {
	color: #999;
	font-size: 28rpx;
}

.date-value {
	color: #333;
	font-size: 32rpx;
	font-weight: 600;
}

/* 时间选择区域 */
.time-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
}

.section-header {
	display: flex;
	align-items: center;
	margin-bottom: 30rpx;
}

.icon-wrapper {
	width: 60rpx;
	height: 60rpx;
	background-color: #E3F2FD;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: 20rpx;
}

.time-icon, .location-icon {
	font-size: 28rpx;
}

.section-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #333;
}

.time-picker-row {
	display: flex;
	gap: 30rpx;
}

.time-picker {
	flex: 1;
	background-color: #f8f9fa;
	border-radius: 15rpx;
	padding: 30rpx 20rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
	border: 2rpx solid #e9ecef;
}

.time-label {
	font-size: 28rpx;
	color: #666;
}

.time-value {
	font-size: 28rpx;
	color: #333;
	font-weight: 600;
}

.time-placeholder {
	font-size: 28rpx;
	color: #999;
}

.time-arrow {
	font-size: 24rpx;
	color: #999;
}

/* 地址输入区域 */
.location-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
}

.location-input {
	background-color: #f8f9fa;
	border-radius: 15rpx;
	padding: 5rpx 20rpx;
	border: 2rpx solid #e9ecef;
}

.address-input {
	width: 100%;
	padding: 25rpx 0;
	font-size: 28rpx;
	color: #333;
	background: transparent;
	border: none;
	outline: none;
}

/* 联系信息区域 */
.contact-section {
	margin: 20rpx 30rpx;
	gap: 20rpx;
	display: flex;
	flex-direction: column;
}

.input-group {
	background-color: #ffffff;
	border-radius: 15rpx;
	padding: 5rpx 30rpx;
}

.contact-input {
	width: 100%;
	padding: 35rpx 0;
	font-size: 28rpx;
	color: #333;
	background: transparent;
	border: none;
	outline: none;
}

.error-text {
	font-size: 24rpx;
	color: #ff4757;
	margin-top: 8rpx;
	padding-left: 24rpx;
}

/* 确认按钮 */
.confirm-section {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	background-color: #ffffff;
	padding: 30rpx;
	border-top: 1rpx solid #eee;
}

.confirm-btn {
	width: 100%;
	height: 88rpx;
	background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
	color: white;
	font-size: 32rpx;
	font-weight: bold;
	border-radius: 44rpx;
	border: none;
	display: flex;
	align-items: center;
	justify-content: center;
}

/* 弹窗样式 */
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 1000;
}

.date-modal, .time-modal {
	background-color: #ffffff;
	border-radius: 20rpx;
	width: 80%;
	max-height: 80%;
	overflow: hidden;
}

.modal-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 30rpx;
	border-bottom: 1rpx solid #eee;
}

.modal-title {
	font-size: 32rpx;
	font-weight: 600;
	color: #333;
}

.close-btn {
	font-size: 32rpx;
	color: #999;
}



.time-list {
	max-height: 400rpx;
	overflow-y: auto;
	padding: 20rpx;
}

.time-option {
	padding: 25rpx 30rpx;
	margin: 10rpx 0;
	border-radius: 15rpx;
	background-color: #f8f9fa;
	text-align: center;
	font-size: 28rpx;
	color: #333;
	transition: all 0.3s ease;
}

.time-option.selected {
	background: linear-gradient(135deg, #4A90E2, #357ABD);
	color: white;
}

.modal-footer {
	display: flex;
	padding: 30rpx;
	gap: 20rpx;
	border-top: 1rpx solid #eee;
}

.cancel-btn {
	flex: 1;
	background-color: #f8f9fa;
	color: #666;
	border: none;
	border-radius: 25rpx;
	padding: 25rpx 0;
	font-size: 28rpx;
}

.confirm-time-btn {
	flex: 1;
	background: linear-gradient(135deg, #4A90E2, #357ABD);
	color: white;
	border: none;
	border-radius: 25rpx;
	padding: 25rpx 0;
	font-size: 28rpx;
	font-weight: 600;
}
</style>