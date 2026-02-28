<template>
	<view class="appointment-time-container">

		<!-- 显示服务类型信息 (使用长方形卡片样式) -->
		<view class="service-type-header">
			<text class="service-type-title">选择的服务类型</text>
			<view class="service-type-card-container">
				<!-- 修改：使用长方形卡片展示服务信息 -->
				<view class="service-type-card">
					<image class="service-icon-large" :src="getServiceIcon(serviceTypeNumber)" mode="aspectFit"></image>
					<text class="service-type-name">{{ serviceTypeName }}</text>
				</view>
			</view>
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

		<!-- --- 新增：症状选择区域 --- -->
		<view class="symptom-section">
			<view class="section-header">
				<view class="icon-wrapper">
					<text class="symptom-icon">🤒</text>
				</view>
				<text class="section-title">症状选择</text>
			</view>
			<view class="symptom-list">
				<view class="symptom-item" v-for="(item, index) in symptoms" :key="index" @click="toggleSymptom(index)">
					<checkbox :checked="selectedSymptoms.includes(index)" class="checkbox" />
					<text class="symptom-text">{{ item }}</text>
				</view>
			</view>
			<view class="add-symptom" @click="showAddSymptomModal">
				<text>+ 添加其他症状</text>
			</view>
		</view>
		<!-- --- 结束新增：症状选择区域 --- -->



		<!-- 联系信息 -->
<view class="contact-section">
  <view class="section-header">
    <view class="icon-wrapper">
      <!-- 添加统一的联系信息表情 -->
      <text class="contact-icon">👤</text>
    </view>
    <text class="section-title">联系信息</text>
  </view>

  <!-- 将姓名和手机号都放入 input-group 中，以保持样式一致 -->
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

		
		<!-- --- 新增：其他需求区域 --- -->
		<view class="other-requirements-section">
			<view class="section-header">
				<view class="icon-wrapper">
					<text class="requirements-icon">📝</text>
				</view>
				<text class="section-title">其他需求</text>
			</view>
			<view class="requirements-input">
				<textarea 
					v-model="otherRequirements" 
					placeholder="如：希望陪诊师有经验、会英语等" 
					class="requirements-textarea"
					:autosize="{ minHeight: 100 }" 
				/>
			</view>
		</view>
		<!-- --- 结束新增：其他需求区域 --- -->

		<!-- 预约按钮 -->
		<view class="confirm-section">
			<button class="confirm-btn" :class="{ disabled: !isFormComplete }" @click="confirmAppointment">
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

		<!-- --- 新增：添加症状弹窗 --- -->
		<view class="modal-overlay" v-if="showAddSymptomModalFlag" @click="hideAddSymptomModal">
			<view class="add-symptom-modal" @click.stop>
				<view class="modal-header">
					<text class="modal-title">添加自定义症状</text>
					<text class="close-btn" @click="hideAddSymptomModal">✕</text>
				</view>
				
				<view class="modal-body">
					<view class="input-group">
						<input 
							v-model="newSymptomInput" 
							placeholder="请输入症状描述" 
							class="contact-input"
							@confirm="confirmAddSymptom" 
						></input>
					</view>
					<text v-if="newSymptomError" class="error-text">{{ newSymptomError }}</text>
				</view>
				
				<view class="modal-footer">
					<button class="cancel-btn" @click="hideAddSymptomModal">取消</button>
					<button class="confirm-time-btn" @click="confirmAddSymptom">确定</button>
				</view>
			</view>
		</view>
		<!-- --- 结束新增：添加症状弹窗 --- -->
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app' // 使用 onLoad 获取参数

// --- 导入你的 API 文件 ---
import { post, get } from '@/utils/api.js' // 👈 现在同时导入 post 和 get

// --- 接收页面参数 ---
// 使用 onLoad 钩子接收从上一个页面传递的参数
const serviceTypeNumber = ref(0); // 从上一个页面传入的数字 (1-4)
const serviceTypeName = ref(''); // 从上一个页面传入的名称 (例如 "普通陪诊")

onLoad((options) => {
	// 从URL参数获取服务类型信息
	if (options.serviceTypeNumber) {
		serviceTypeNumber.value = parseInt(options.serviceTypeNumber); // 转换为数字
	}
	if (options.serviceTypeName) {
		serviceTypeName.value = decodeURIComponent(options.serviceTypeName); // 解码中文
	}
	// 可以在这里打印出来检查是否接收成功
	// console.log('接收到的 serviceTypeNumber:', serviceTypeNumber.value);
	// console.log('接收到的 serviceTypeName:', serviceTypeName.value);
});

// --- 系统信息 ---
const statusBarHeight = ref(0)

// --- 响应式数据 ---
const selectedDate = ref('')
const startTime = ref('')
const endTime = ref('')
const hospitalAddress = ref('')
const patientName = ref('')
const phoneNumber = ref('')
const phoneError = ref('')
const isPhoneValid = ref(false)

// --- 弹窗控制 ---
const showTimeModal = ref(false)
const timePickerType = ref('') // 'start' 或 'end'
const selectedTime = ref('')

// --- 新增：症状和需求相关数据 ---
const symptoms = ref(['胸痛', '头痛', '发热', '呼吸困难', '恶心呕吐', '腹痛', '头晕']);
const selectedSymptoms = ref([]); // 存储选中的症状索引
const otherRequirements = ref(''); // 存储其他需求文本

// --- 新增：添加症状弹窗 ---
const showAddSymptomModalFlag = ref(false)
const newSymptomInput = ref('')
const newSymptomError = ref('')

// --- 计算属性 ---
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

// --- 方法 ---
// 返回上一页 (可选，根据需求决定是否需要)
const goBack = () => {
	uni.navigateBack({
		delta: 1 // 返回上一页
	});
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

const isValidPhone = computed(() => {
	const phoneRegex = /^1[3-9]\d{9}$/
	return !!phoneNumber.value && phoneRegex.test(String(phoneNumber.value).trim())
})

// 除“其他需求”外，其余都必须填写/选择（含症状至少选择1项）
const isFormComplete = computed(() => {
	return (
		!!selectedDate.value &&
		!!startTime.value &&
		!!endTime.value &&
		!!String(hospitalAddress.value || '').trim() &&
		!!String(patientName.value || '').trim() &&
		isValidPhone.value &&
		Array.isArray(selectedSymptoms.value) &&
		selectedSymptoms.value.length > 0
	)
})

// --- 获取服务图标路径 ---
const getServiceIcon = (typeNumber) => {
	// 根据服务类型数字返回对应的图标路径
	switch(typeNumber) {
		case 1: return '/static/logo_1.png'; // 普通陪诊
		case 2: return '/static/logo_2.png'; // 术后护理
		case 3: return '/static/logo_3.jpg'; // 急诊陪同
		case 4: return '/static/logo_4.jpg'; // 上门陪诊
		default: return '/static/default_icon.png'; // 默认图标
	}
}

// --- 新增：症状选择方法 ---
const toggleSymptom = (index) => {
	if (selectedSymptoms.value.includes(index)) {
		selectedSymptoms.value = selectedSymptoms.value.filter(i => i !== index)
	} else {
		selectedSymptoms.value.push(index)
	}
}

// --- 新增：添加症状弹窗方法 ---
const showAddSymptomModal = () => {
	newSymptomInput.value = '' // 清空输入框
	newSymptomError.value = '' // 清空错误信息
	showAddSymptomModalFlag.value = true
}

const hideAddSymptomModal = () => {
	showAddSymptomModalFlag.value = false
}

const validateNewSymptom = () => {
	if (!newSymptomInput.value.trim()) {
		newSymptomError.value = '请输入症状描述'
		return false
	}
	if (newSymptomInput.value.trim().length > 50) {
		newSymptomError.value = '症状描述不能超过50个字符'
		return false
	}
	// 检查是否已经存在
	if (symptoms.value.includes(newSymptomInput.value.trim())) {
		newSymptomError.value = '该症状已存在'
		return false
	}
	newSymptomError.value = ''
	return true
}

const confirmAddSymptom = () => {
	if (!validateNewSymptom()) {
		return
	}
	// 添加到症状列表
	symptoms.value.push(newSymptomInput.value.trim())
	// 可以选择是否自动选中新增的症状
	// selectedSymptoms.value.push(symptoms.value.length - 1);
	hideAddSymptomModal()
	uni.showToast({
		title: '症状添加成功',
		icon: 'success'
	})
}

// --- 确认预约 ---
const confirmAppointment = async () => { // ⚠️ 修改为异步函数

	// 聚合校验：除其他需求外均需填写/选择
	const missing = []
	if (!selectedDate.value) missing.push('服务日期')
	if (!startTime.value || !endTime.value) missing.push('服务时段')
	if (!String(hospitalAddress.value || '').trim()) missing.push('医院地址')
	if (!selectedSymptoms.value || selectedSymptoms.value.length === 0) missing.push('症状（至少选择1项）')
	if (!String(patientName.value || '').trim()) missing.push('姓名')
	if (!String(phoneNumber.value || '').trim()) missing.push('手机号')

	if (missing.length > 0) {
		uni.showModal({
			title: '提示',
			content: `请先完善：${missing.join('、')}，全部输入完成后才能点击确认预约`,
			showCancel: false
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

	// 时间段合法性：结束时间必须晚于开始时间
	const toMinutes = (t) => {
		const [h, m] = String(t).split(':').map(Number)
		return (h || 0) * 60 + (m || 0)
	}
	if (toMinutes(endTime.value) <= toMinutes(startTime.value)) {
		uni.showToast({ title: '结束时间需晚于开始时间', icon: 'none' })
		return
	}

	// --- 构建提交需求的数据 ---
	const demandData = {
		// 服务类型信息
		serviceTypeNumber: serviceTypeNumber.value, // 确保是数字
		// serviceTypeName: serviceTypeName.value, // ⚠️ 移除这行，因为后端不识别
		// 服务时间信息
		serviceDate: selectedDate.value, // YYYY-MM-DD
		serviceStartTime: startTime.value, // HH:mm
		serviceEndTime: endTime.value, // HH:mm
		// 医院信息
		hospital: String(hospitalAddress.value || '').trim(),
		// 联系人信息
		patientName: String(patientName.value || '').trim(),
		patientPhone: String(phoneNumber.value || '').trim(),
		// --- 新增：症状和需求 ---
		// 症状 (确保是一个数组)
		symptoms: selectedSymptoms.value.map(i => symptoms.value[i]), // 将索引转换为实际症状文本
		// 其他需求 (确保是一个字符串)
		otherRequirement: otherRequirements.value,
		// --- 结束新增 ---
	};

	// --- 调试：打印即将发送的数据 ---
	console.log('【02页面】即将发送的请求数据 (JSON.stringify):', JSON.stringify(demandData, null, 2));
	console.log('【02页面】即将发送的请求数据 (JS Object):', demandData);

	// --- 调试：检查字段类型 ---
	console.log('【02页面】字段类型检查:');
	console.log('  serviceTypeNumber:', typeof demandData.serviceTypeNumber, demandData.serviceTypeNumber);
	console.log('  serviceDate:', typeof demandData.serviceDate, demandData.serviceDate);
	console.log('  serviceStartTime:', typeof demandData.serviceStartTime, demandData.serviceStartTime);
	console.log('  serviceEndTime:', typeof demandData.serviceEndTime, demandData.serviceEndTime);
	console.log('  hospital:', typeof demandData.hospital, demandData.hospital);
	console.log('  patientName:', typeof demandData.patientName, demandData.patientName);
	console.log('  patientPhone:', typeof demandData.patientPhone, demandData.patientPhone);
	console.log('  otherRequirement:', typeof demandData.otherRequirement, demandData.otherRequirement);
	console.log('  symptoms:', typeof demandData.symptoms, demandData.symptoms, Array.isArray(demandData.symptoms));

	// --- 调用 API 提交需求并创建订单 ---
	try {
		uni.showLoading({ title: '提交中...' }); // 显示加载提示

		// 1. 提交预约信息
		const submitResponse = await post('/ai/guide/appointments', demandData);
		console.log('【02页面】提交预约响应:', submitResponse);

		if (submitResponse && submitResponse.code === 200) {
			const appointmentNo = submitResponse.data.appointmentNo;
			console.log('【02页面】获取到预约编号:', appointmentNo);

			if (!appointmentNo) {
				throw new Error('未能获取到预约编号');
			}

			// 2. 根据预约编号匹配陪诊师
			const matchResponse = await get(`/ai/guide/attendants/match?appointmentNo=${appointmentNo}`);
			console.log('【02页面】匹配陪诊师响应:', matchResponse);

			if (matchResponse && matchResponse.code === 200 && matchResponse.data.attendants.length > 0) {
				// 自动选择第一个陪诊师
				const firstAttendant = matchResponse.data.attendants[0];
				const attendantId = firstAttendant.id;
				
				// 3. 创建订单
				const orderRequest = {
					appointmentNo: appointmentNo,
					attendantId: attendantId.toString()
				};
				
				const orderResponse = await post('/ai/guide/orders', orderRequest);
				console.log('【02页面】创建订单响应:', orderResponse);
				
				if (orderResponse && orderResponse.code === 200) {
					const orderNo = orderResponse.data.orderNo;
					console.log('【02页面】获取到订单编号:', orderNo);
					
					if (!orderNo) {
						throw new Error('未能获取到订单编号');
					}

					// --- 保存订单编号 ---
					uni.setStorageSync('orderNo', orderNo);

					// --- 直接跳转到订单确认页面 ---
					uni.redirectTo({
						url: '/pages/AItriage/04_OrderConfirmPage?orderNo=' + orderNo
					});
				} else {
					throw new Error(orderResponse.message || '创建订单失败');
				}
			} else {
				throw new Error('未找到可用陪诊师');
			}
		} else {
			const errorMsg = submitResponse.message || '提交预约失败';
			throw new Error(errorMsg);
		}
	} catch (error) {
		console.error('【02页面】提交预约失败:', error);
		let errorMessage = error.message || '提交预约失败，请稍后重试';
		// 如果是 HTTP 错误，err 可能是 response 对象
		if (error.statusCode && error.data) {
		   errorMessage = `HTTP ${error.statusCode}: ${error.data.message || '请求失败'}`;
		   console.error('【02页面】API 响应详情:', error.data); // 打印详细响应
		} else if (error.errMsg) {
		    errorMessage = `请求失败: ${error.errMsg}`;
		}
		uni.showToast({
			title: errorMessage,
			icon: 'none'
		});
	} finally {
		uni.hideLoading(); // 隐藏加载提示
	}
}
// --- 页面加载完成后执行 ---
onMounted(async () => {
	// 获取系统信息
	uni.getSystemInfo({
		success: (res) => {
			statusBarHeight.value = res.statusBarHeight
			// 设置 CSS 变量 (如果需要)
			document.documentElement.style.setProperty('--status-bar-height', `${res.statusBarHeight}px`);
		}
	})
	
	// 获取当前登录用户信息并填充默认值
	try {
		// 直接调用 API 获取当前用户信息
		const response = await get('/api/users/current')
		console.log('获取用户信息响应:', response)
		
		if (response && response.code === 200 && response.data) {
			const userData = response.data
			console.log('用户数据:', userData)
			
			// 填充姓名
			if (userData.name && !patientName.value) {
				patientName.value = userData.name
			}
			
			// 填充电话
			if (userData.phone && !phoneNumber.value) {
				phoneNumber.value = userData.phone
			}
			
			console.log('自动填充完成 - 姓名:', patientName.value, '电话:', phoneNumber.value)
		} else {
			console.log('未获取到用户信息或用户未登录')
		}
	} catch (error) {
		console.log('获取用户信息失败:', error)
	}
})
</script>

<style scoped>
/* 原始样式保持不变 */
.appointment-time-container {
	min-height: 100vh;
	background-color: #f5f5f5;
	padding-bottom: 150rpx;
	padding-top: 30rpx;
}

/* 新增：服务类型卡片区域 (更接近01的长方形卡片样式) */
.service-type-header {
	background-color: #ffffff; /* 白色背景 */
	color: #333;
	padding: 30rpx 30rpx;
	margin: 0rpx 30rpx;
	border-radius: 20rpx;
	text-align: center;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影，提升卡片感 */
}

.service-type-title {
	font-size: 32rpx;
	font-weight: bold;
	display: block;
	margin-bottom: 20rpx;
	color: #333;
}

.service-type-card-container {
	display: flex;
	justify-content: center; /* 水平居中 */
}

/* 新增：长方形服务卡片样式 */
.service-type-card {
	background-color: #f0f8ff; /* 浅蓝色背景，区分度高 */
	border-radius: 20rpx; /* 圆角 */
	padding: 15rpx 40rpx; /* 内边距 */
	display: flex;
	align-items: center; /* 垂直居中 */
	gap: 20rpx; /* 图标和文字之间的间距 */
	min-width: 450rpx; /* 固定宽度，保证卡片长度 */
	min-height: 50rpx; /* 固定高度，保证卡片高度 */
	justify-content: center; /* 水平居中内容 */
	border: 2rpx solid #4A90E2; /* 边框，突出卡片 */
}

.service-icon-large {
	width: 60rpx; /* 调整图标大小 */
	height: 60rpx;
}

.service-type-name {
	font-size: 32rpx;
	font-weight: 600; /* 加粗显示名称 */
	color: #333;
	text-align: center; /* 文本居中 */
}

/* 日期选择区域 */
.date-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
	text-align: center;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
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
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
}

.section-header {
	display: flex;
	align-items: center;
	margin-bottom: 30rpx;
}

.icon-wrapper {
  width: 60rpx;
  height: 60rpx;
  background-color: #E3F2FD; /* 使用与其它区域一致的背景色 */
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.time-icon, .location-icon, .symptom-icon, .requirements-icon, .contact-icon {
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
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
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

/* --- 新增：症状选择区域样式 --- */
.symptom-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
}

.symptom-list {
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: 12px;
	margin-bottom: 16px;
}

.symptom-item {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px;
	border: 1px solid #eee;
	border-radius: 8px;
	font-size: 28rpx;
	cursor: pointer;
}

.checkbox {
	transform: scale(0.8);
}

.symptom-text {
	color: #333;
}

.add-symptom {
	color: #007AFF;
	font-size: 28rpx;
	padding: 8px;
	text-align: center;
	cursor: pointer;
}

/* --- 新增：其他需求区域样式 --- */
.other-requirements-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
}

.requirements-input {
	background-color: #f8f9fa;
	border-radius: 15rpx;
	padding: 5rpx 20rpx;
	border: 2rpx solid #e9ecef;
}

.requirements-textarea {
	width: 100%;
	min-height: 100rpx; /* 设置最小高度 */
	padding: 25rpx 0;
	font-size: 28rpx;
	color: #333;
	background: transparent;
	border: none;
	outline: none;
	resize: vertical; /* 允许垂直调整大小 */
}

/* 联系信息区域 */
.contact-section {
  background-color: #ffffff;
  margin: 20rpx 30rpx;
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
}

.input-group {
  background-color: #f8f9fa; /* 与地址输入区域一致的背景色 */
  border-radius: 15rpx;
  padding: 5rpx 20rpx; /* 与地址输入区域一致的内边距 */
  border: 2rpx solid #e9ecef; /* 与地址输入区域一致的边框 */
  margin-bottom: 20rpx; 
}


.contact-input {
  width: 100%;
  padding: 25rpx 0; /* 与地址输入框一致的上下内边距 */
  font-size: 28rpx;
  color: #333;
  background: transparent;
  border: none;
  outline: none;
  /* 如果需要，可以设置字体粗细 */
  /* font-weight: normal; */
}


/* 错误信息样式 */
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
	box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加底部阴影 */
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

.confirm-btn.disabled {
	background: #d9d9d9;
	color: #ffffff;
	opacity: 0.85;
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

.date-modal, .time-modal, .add-symptom-modal {
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

/* --- 新增：添加症状弹窗样式 --- */
.add-symptom-modal .modal-body {
	padding: 30rpx;
}

.add-symptom-modal .input-group {
  background-color: #f8f9fa; /* 与其它输入区域一致的背景色 */
  border-radius: 15rpx;
  padding: 5rpx 20rpx; /* 与其它输入区域一致的内边距 */
  border: 2rpx solid #e9ecef; /* 与其它输入区域一致的边框 */
  margin-bottom: 20rpx; 
  width: 90%;
 
  /* 确保输入框填满容器 */
}

.add-symptom-modal .contact-input {
  width: 100%;
  padding: 25rpx ; /* 与其它输入框一致的上下内边距 */
  font-size: 28rpx;
  color: #333;
  background: transparent;
  border: none;
  outline: none;
  
  /* 如果需要，可以设置字体粗细 */
  /* font-weight: normal; */
}

/* --- 结束新增：添加症状弹窗样式 --- */

</style>