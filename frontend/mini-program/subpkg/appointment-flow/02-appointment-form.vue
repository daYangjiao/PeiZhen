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
			<view class="date-card-shell" @click="showCalendarPicker">
				<view class="date-card-main">
					<view class="date-copy">
						<text class="date-label">服务日期</text>
						<text class="date-card-value" :class="{ placeholder: !selectedDate }">
							{{ selectedDate ? formatDate(selectedDate) : '请选择就诊日期' }}
						</text>
					</view>
					<view class="date-badge">
						<text class="date-badge-text">{{ selectedDate ? '已选择' : '选择' }}</text>
					</view>
				</view>
			</view>
		</view>

		<appointment-time-range-picker
			v-model:start-time="startTime"
			v-model:end-time="endTime"
			:selected-date="selectedDate"
		/>

		<!-- 输入地址或选择医院 -->
		<view class="location-section">
			<view class="section-header">
				<view class="icon-wrapper">
					<text class="location-icon">📍</text>
				</view>
				<text class="section-title">选择就诊医院</text>
			</view>
			
			<view class="hospital-card-shell" @click="showHospitalPicker">
				<view class="hospital-card-main">
					<view class="hospital-copy">
						<text class="hospital-label">医院 / 地址</text>
						<text class="hospital-card-value" :class="{ placeholder: !hospitalAddress }">
							{{ hospitalAddress || '点击选择常用医院，或搜索后确认' }}
						</text>
					</view>
					<view class="hospital-badge">
						<text class="hospital-badge-text">{{ hospitalAddress ? '已选择' : '选择' }}</text>
					</view>
				</view>
				<view class="hospital-card-meta">
					<text class="hospital-meta-text">可以搜索常用医院，也可以直接填写具体医院或地址</text>
				</view>
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
				<view
					class="symptom-item"
					:class="{ selected: isSymptomSelected(item) }"
					v-for="(item, index) in symptoms"
					:key="index"
					@click="toggleSymptom(item)"
				>
					<view class="checkbox-indicator" :class="{ selected: isSymptomSelected(item) }">
						<text class="checkbox-mark" v-if="isSymptomSelected(item)">✓</text>
					</view>
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
      type="text"
      confirm-type="next"
      cursor-spacing="120"
      @input="handlePatientNameInput"
    />
  </view>
  <view class="input-group">
    <input
      v-model="phoneNumber"
      placeholder="手机号"
      class="contact-input"
      type="number"
      inputmode="numeric"
      maxlength="11"
      confirm-type="done"
      cursor-spacing="120"
      @input="handlePhoneInput"
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
					auto-height
				/>
			</view>
		</view>
		<!-- --- 结束新增：其他需求区域 --- -->

		<!-- 预约按钮 -->
		<view class="confirm-section">
			<view v-if="formError" class="form-error-card">
				<text class="form-error-title">信息还没填完整</text>
				<text class="form-error-desc">{{ formError }}</text>
			</view>
			<button class="confirm-btn" :class="{ disabled: !isFormComplete }" @click="confirmAppointment">
				确认预约
			</button>
		</view>

		<!-- 日期选择弹窗 -->
		<view class="modal-overlay" v-if="showCalendarModal" @click="hideCalendarPicker">
			<view class="calendar-modal-simple" @click.stop>
				<view class="calendar-modal-header">
					<view>
						<text class="calendar-modal-title">选择服务日期</text>
						<text class="calendar-modal-subtitle">仅支持当天之后的可预约日期</text>
					</view>
					<text class="close-btn" @click="hideCalendarPicker">✕</text>
				</view>

				<view class="cal-header">
					<text class="cal-month">{{ currentMonthYear }}</text>
					<view class="cal-nav">
						<text class="cal-btn" @click="prevMonth">‹</text>
						<text class="cal-btn" @click="nextMonth">›</text>
					</view>
				</view>

				<view class="cal-weekdays">
					<text class="cal-wd" v-for="dayLabel in weekdays" :key="dayLabel">{{ dayLabel }}</text>
				</view>

				<view class="cal-days">
					<view
						v-for="(day, index) in calendarDays"
						:key="`${day.date || 'empty'}-${index}`"
						class="cal-day"
						:class="{
							empty: day.isEmpty,
							today: day.isToday,
							selected: day.isSelected,
							disabled: day.isDisabled
						}"
						@click="selectDay(day)"
					>
						<text class="cal-d">{{ day.day }}</text>
					</view>
				</view>

				<view class="cal-footer">
					<button class="cal-cancel" @click="hideCalendarPicker">取消</button>
					<button class="cal-ok" @click="confirmCalendar">确定</button>
				</view>
			</view>
		</view>

		<!-- 医院选择弹窗 -->
		<view class="modal-overlay" v-if="showHospitalModal" @click="hideHospitalPicker">
			<view class="hospital-modal hospital-modal-animated" @click.stop>
				<view class="hospital-modal-header">
					<view>
						<text class="hospital-modal-title">选择就诊医院</text>
						<text class="hospital-modal-subtitle">支持搜索常用医院，也可直接使用输入内容</text>
					</view>
					<text class="close-btn" @click="hideHospitalPicker">✕</text>
				</view>

				<view class="hospital-search-shell">
					<input
						v-model="hospitalKeyword"
						class="hospital-search-input"
						placeholder="搜索医院名称或输入具体地址"
						confirm-type="search"
					/>
				</view>

				<scroll-view class="hospital-list" scroll-y>
					<view
						v-for="option in filteredHospitalOptions"
						:key="option"
						class="hospital-option"
						:class="{ selected: pendingHospitalAddress === option }"
						@click="selectHospital(option)"
					>
						<view class="hospital-option-copy">
							<text class="hospital-option-name">{{ option }}</text>
							<text class="hospital-option-desc">常用医院</text>
						</view>
						<text class="hospital-option-check" :class="{ visible: pendingHospitalAddress === option }">✓</text>
					</view>

					<view
						v-if="showCustomHospitalOption"
						class="hospital-option custom"
						:class="{ selected: pendingHospitalAddress === normalizedHospitalKeyword }"
						@click="selectHospital(normalizedHospitalKeyword)"
					>
						<view class="hospital-option-copy">
							<text class="hospital-option-name">{{ normalizedHospitalKeyword }}</text>
							<text class="hospital-option-desc">使用当前输入作为医院/地址</text>
						</view>
						<text class="hospital-option-check" :class="{ visible: pendingHospitalAddress === normalizedHospitalKeyword }">✓</text>
					</view>

					<view v-if="!filteredHospitalOptions.length && !showCustomHospitalOption" class="hospital-empty">
						<text class="hospital-empty-text">没有匹配结果，请继续输入更完整的医院名称或地址</text>
					</view>
				</scroll-view>

				<view class="hospital-footer">
					<button class="cancel-btn" @click="hideHospitalPicker">取消</button>
					<button class="confirm-time-btn" @click="confirmHospital">确定</button>
				</view>
			</view>
		</view>

		<!-- --- 新增：添加症状弹窗 --- -->
		<view class="modal-overlay symptom-sheet-overlay" v-if="showAddSymptomModalFlag" @click="hideAddSymptomModal">
			<view class="symptom-sheet" @click.stop>
				<view class="symptom-sheet-handle"></view>
				<view class="symptom-sheet-header">
					<view class="symptom-sheet-copy">
						<text class="symptom-sheet-title">添加其他症状</text>
					</view>
					<view class="symptom-sheet-close" @click="hideAddSymptomModal">
						<text class="symptom-sheet-close-icon">✕</text>
					</view>
				</view>

				<view class="symptom-sheet-body">
					<view class="symptom-input-card" :class="{ error: !!newSymptomError }">
						<view class="symptom-input-field">
							<input
								v-model="newSymptomInput"
								placeholder="例如：夜间胸闷、持续性头痛、术后伤口疼痛"
								class="symptom-input-control"
								@confirm="confirmAddSymptom"
								maxlength="50"
							></input>
						</view>
					</view>
					<text v-if="newSymptomError" class="symptom-sheet-error">{{ newSymptomError }}</text>
				</view>

				<view class="symptom-sheet-footer">
					<button class="symptom-sheet-secondary-btn" @click="hideAddSymptomModal">取消</button>
					<button class="symptom-sheet-primary-btn" @click="confirmAddSymptom">确认添加</button>
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
import { appointmentServiceLogos } from '@/utils/assets.js'
import { HOSPITAL_OPTIONS } from '@/utils/hospital-options.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import {
	normalizeContactName,
	normalizeContactPhone
} from '@/utils/appointment-form.mjs'
import AppointmentTimeRangePicker from '@/components/appointment-time-range-picker.vue'

// --- 接收页面参数 ---
// 使用 onLoad 钩子接收从上一个页面传递的参数
const serviceTypeNumber = ref(0); // 从上一个页面传入的数字 (1-4)
const serviceTypeName = ref(''); // 从上一个页面传入的名称 (例如 "普通陪诊")

onLoad((options) => {
	if (redirectPublicSafeToHome()) return
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
const hospitalIndex = ref(-1)
const patientName = ref('')
const phoneNumber = ref('')
const phoneError = ref('')
const isPhoneValid = ref(false)
const formError = ref('')

// --- 弹窗控制 ---
const showTimeModal = ref(false)
const timePickerType = ref('') // 'start' 或 'end'
const selectedTime = ref('')
const showCalendarModal = ref(false)
const calendarViewDate = ref(new Date())
const pendingDate = ref('')
const timeGroupExpandedState = ref({})
const timeScrollTarget = ref('')
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const showHospitalModal = ref(false)
const hospitalKeyword = ref('')
const pendingHospitalAddress = ref('')

// --- 新增：症状和需求相关数据 ---
const symptoms = ref(['胸痛', '头痛', '发热', '呼吸困难', '恶心呕吐', '腹痛', '头晕']);
const selectedSymptoms = ref([]);
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

const normalizedHospitalKeyword = computed(() => String(hospitalKeyword.value || '').trim())

const filteredHospitalOptions = computed(() => {
	const keyword = normalizedHospitalKeyword.value.toLowerCase()
	if (!keyword) {
		return HOSPITAL_OPTIONS
	}
	return HOSPITAL_OPTIONS.filter(option => option.toLowerCase().includes(keyword))
})

const showCustomHospitalOption = computed(() => {
	const keyword = normalizedHospitalKeyword.value
	return !!keyword && !HOSPITAL_OPTIONS.includes(keyword)
})

const currentMonthYear = computed(() => {
	const year = calendarViewDate.value.getFullYear()
	const month = calendarViewDate.value.getMonth() + 1
	return `${year}年${month}月`
})

const calendarDays = computed(() => {
	const current = calendarViewDate.value
	const year = current.getFullYear()
	const month = current.getMonth()
	const firstDay = new Date(year, month, 1)
	const lastDay = new Date(year, month + 1, 0)
	const today = new Date()
	today.setHours(0, 0, 0, 0)
	const days = []

	for (let i = 0; i < firstDay.getDay(); i++) {
		days.push({ isEmpty: true, day: '' })
	}

	for (let day = 1; day <= lastDay.getDate(); day++) {
		const date = new Date(year, month, day)
		date.setHours(0, 0, 0, 0)
		const dateString = formatDateKey(date)
		days.push({
			day,
			date: dateString,
			isEmpty: false,
			isDisabled: date.getTime() < today.getTime(),
			isToday: date.getTime() === today.getTime(),
			isSelected: pendingDate.value === dateString
		})
	}

	return days
})

const timePickerTitle = computed(() => {
	return timePickerType.value === 'start' ? '选择开始时间' : '选择结束时间'
})

const TIME_GROUP_DEFINITIONS = [
	{ key: 'lateNight', label: '凌晨', rangeLabel: '00:00 - 05:30', start: 0, end: 359 },
	{ key: 'morning', label: '上午', rangeLabel: '06:00 - 11:30', start: 360, end: 719 },
	{ key: 'afternoon', label: '下午', rangeLabel: '12:00 - 17:30', start: 720, end: 1079 },
	{ key: 'evening', label: '晚上', rangeLabel: '18:00 - 23:30', start: 1080, end: 1439 }
]

const getTodayEarliestStartMinutes = () => {
	const now = new Date()
	const currentMinutes = now.getHours() * 60 + now.getMinutes()
	const currentSlotStartMinutes = Math.floor(currentMinutes / 30) * 30
	const minutesAfterSlotStart = currentMinutes - currentSlotStartMinutes
	if (minutesAfterSlotStart <= 5) {
		return currentSlotStartMinutes
	}
	return currentSlotStartMinutes + 30
}

const getExpiredStartTimeMessage = (minAllowedStartMinutes) =>
	`您选择的开始时间已超过可预约时限，请重新选择 ${formatMinutesToTime(minAllowedStartMinutes)} 及之后的开始时间`

const isTodayDate = (dateValue) => {
	if (!dateValue) return false
	const appointmentDate = createSafeDate(dateValue)
	if (Number.isNaN(appointmentDate.getTime())) return false
	appointmentDate.setHours(0, 0, 0, 0)
	const today = new Date()
	today.setHours(0, 0, 0, 0)
	return appointmentDate.getTime() === today.getTime()
}

const calculateSlotDurationMinutes = (startValue, endValue) => {
	const startMinutes = parseTimeToMinutes(startValue)
	const endMinutes = parseTimeToMinutes(endValue)
	if (Number.isNaN(startMinutes) || Number.isNaN(endMinutes)) {
		return NaN
	}
	if (endMinutes === startMinutes) {
		return 0
	}
	return endMinutes > startMinutes
		? endMinutes - startMinutes
		: 24 * 60 - startMinutes + endMinutes
}

const formatMinutesToTime = (minutes) => {
	const safeMinutes = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
	const hour = Math.floor(safeMinutes / 60)
	const minute = safeMinutes % 60
	return `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
}

const createTimeOption = (timeStr, isNextDay = false, relativeOrder = 0) => {
	const minutes = parseTimeToMinutes(timeStr)
	return {
		time: timeStr,
		minutes,
		isNextDay,
		relativeOrder,
		groupKey: findTimeGroupByMinutes(minutes)?.key || '',
		displayLabel: `${isNextDay ? '次日 ' : ''}${timeStr}`
	}
}

const getAvailableTimeOptions = (dateValue, pickerType = 'start') => {
	const options = []
	const selectedStartMinutes = parseTimeToMinutes(startTime.value)
	if (pickerType === 'end' && !Number.isNaN(selectedStartMinutes)) {
		for (let offset = 30; offset < 24 * 60; offset += 30) {
			const absoluteMinutes = selectedStartMinutes + offset
			const isNextDay = absoluteMinutes >= 24 * 60
			const timeStr = formatMinutesToTime(absoluteMinutes)
			options.push(createTimeOption(timeStr, isNextDay, offset))
		}
		return options
	}

	for (let hour = 0; hour <= 23; hour++) {
		for (let minute = 0; minute < 60; minute += 30) {
			const timeStr = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
			const timeMinutes = parseTimeToMinutes(timeStr)
			if (pickerType === 'start' && isTodayDate(dateValue) && timeMinutes < getTodayEarliestStartMinutes()) {
				continue
			}
			options.push(createTimeOption(timeStr))
		}
	}
	return options
}

const timeOptions = computed(() => getAvailableTimeOptions(selectedDate.value, timePickerType.value))

const findTimeGroupByMinutes = (minutes) => TIME_GROUP_DEFINITIONS.find(group => minutes >= group.start && minutes <= group.end) || null

const findPreferredTimeGroupKey = (options) => {
	if (timePickerType.value === 'end' && startTime.value) {
		return options[0]?.groupKey || ''
	}
	const preferredOrder = ['morning', 'afternoon', 'evening', 'lateNight']
	for (const key of preferredOrder) {
		if (options.some(option => option.groupKey === key)) {
			return key
		}
	}
	return ''
}

const timeGroups = computed(() => {
	if (!timeOptions.value.length) {
		return []
	}

	return TIME_GROUP_DEFINITIONS
		.map(group => ({
			...group,
			anchorId: `time-group-${group.key}`,
			options: timeOptions.value.filter(option => option.groupKey === group.key)
		}))
		.filter(group => group.options.length > 0)
})

const isTimeGroupExpanded = (groupKey) => !!timeGroupExpandedState.value[groupKey]

const initializeTimeGroupState = () => {
	if (timePickerType.value === 'end') {
		timeGroupExpandedState.value = {}
		timeScrollTarget.value = ''
		return
	}

	const nextState = {}
	const options = timeOptions.value
	const preferredKey = findPreferredTimeGroupKey(options)
	const selectedOption = timeOptions.value.find(option => isSelectedTimeOption(option))
	const selectedGroupKey = selectedOption?.groupKey || ''

	for (const group of timeGroups.value) {
		nextState[group.key] = group.key === preferredKey || group.key === selectedGroupKey
	}

	if (!Object.values(nextState).some(Boolean) && timeGroups.value.length > 0) {
		nextState[timeGroups.value[0].key] = true
	}

	timeGroupExpandedState.value = nextState
	const targetKey = selectedGroupKey && nextState[selectedGroupKey]
		? selectedGroupKey
		: preferredKey || timeGroups.value[0]?.key || ''
	timeScrollTarget.value = targetKey ? `time-group-${targetKey}` : ''
}

// --- 方法 ---
// 返回上一页 (可选，根据需求决定是否需要)
const goBack = () => {
	uni.navigateBack({
		delta: 1 // 返回上一页
	});
}

const formatDateKey = (date) => {
	const year = date.getFullYear()
	const month = String(date.getMonth() + 1).padStart(2, '0')
	const day = String(date.getDate()).padStart(2, '0')
	return `${year}-${month}-${day}`
}

const syncCalendarViewDate = (dateString) => {
	const safeDateString = dateString || minDate.value
	const nextDate = new Date(`${safeDateString}T00:00:00`)
	if (Number.isNaN(nextDate.getTime())) {
		calendarViewDate.value = new Date()
		return
	}
	calendarViewDate.value = nextDate
}

const showCalendarPicker = () => {
	pendingDate.value = selectedDate.value || minDate.value
	syncCalendarViewDate(pendingDate.value)
	showCalendarModal.value = true
}

const hideCalendarPicker = () => {
	showCalendarModal.value = false
}

const prevMonth = () => {
	const current = calendarViewDate.value
	calendarViewDate.value = new Date(current.getFullYear(), current.getMonth() - 1, 1)
}

const nextMonth = () => {
	const current = calendarViewDate.value
	calendarViewDate.value = new Date(current.getFullYear(), current.getMonth() + 1, 1)
}

const selectDay = (day) => {
	if (!day || day.isEmpty || day.isDisabled) {
		return
	}
	pendingDate.value = day.date
}

const confirmCalendar = () => {
	if (!pendingDate.value) {
		uni.showToast({
			title: '请选择服务日期',
			icon: 'none'
		})
		return
	}
	selectedDate.value = pendingDate.value
	syncSelectedTimesForDate()
	hideCalendarPicker()
}

const showHospitalPicker = () => {
	hospitalKeyword.value = hospitalAddress.value
	pendingHospitalAddress.value = hospitalAddress.value
	showHospitalModal.value = true
}

const hideHospitalPicker = () => {
	showHospitalModal.value = false
}

const selectHospital = (option) => {
	pendingHospitalAddress.value = option
}

const confirmHospital = () => {
	const finalHospital = String(pendingHospitalAddress.value || normalizedHospitalKeyword.value || '').trim()
	if (!finalHospital) {
		uni.showToast({
			title: '请选择或输入医院',
			icon: 'none'
		})
		return
	}
	hospitalAddress.value = finalHospital
	hospitalIndex.value = HOSPITAL_OPTIONS.indexOf(finalHospital)
	hideHospitalPicker()
}

const showStartTimePicker = () => {
	if (!selectedDate.value) {
		uni.showToast({
			title: '请先选择服务日期',
			icon: 'none'
		})
		return
	}
	timePickerType.value = 'start'
	selectedTime.value = startTime.value
	showTimeModal.value = true
	initializeTimeGroupState()
}

const showEndTimePicker = () => {
	if (!selectedDate.value) {
		uni.showToast({
			title: '请先选择服务日期',
			icon: 'none'
		})
		return
	}
	if (!startTime.value) {
		uni.showToast({
			title: '请先选择开始时间',
			icon: 'none'
		})
		return
	}
	timePickerType.value = 'end'
	selectedTime.value = endTime.value
	showTimeModal.value = true
	initializeTimeGroupState()
}

const hideTimePicker = () => {
	showTimeModal.value = false
	selectedTime.value = ''
	timeScrollTarget.value = ''
}

const selectTime = (option) => {
	selectedTime.value = option.time
}

const isNextDayEndTimeOption = (timeValue) => {
	if (timePickerType.value !== 'end' || !startTime.value) return false
	const durationMinutes = calculateSlotDurationMinutes(startTime.value, timeValue)
	const endMinutes = parseTimeToMinutes(timeValue)
	const startMinutes = parseTimeToMinutes(startTime.value)
	return !Number.isNaN(durationMinutes) && durationMinutes > 0 && endMinutes < startMinutes
}

const isSelectedTimeOption = (option) => {
	if (!option || selectedTime.value !== option.time) return false
	if (timePickerType.value !== 'end') return true
	return isNextDayEndTimeOption(option.time) === option.isNextDay
}

const formatSelectedEndTimeDisplay = () => {
	if (!endTime.value) return ''
	if (!startTime.value) return endTime.value
	const durationMinutes = calculateSlotDurationMinutes(startTime.value, endTime.value)
	if (Number.isNaN(durationMinutes) || durationMinutes <= 0) {
		return endTime.value
	}
	return parseTimeToMinutes(endTime.value) < parseTimeToMinutes(startTime.value)
		? `次日${endTime.value}`
		: endTime.value
}

const isNextDaySelectedEndTime = () => {
	if (!startTime.value || !endTime.value) return false
	const durationMinutes = calculateSlotDurationMinutes(startTime.value, endTime.value)
	if (Number.isNaN(durationMinutes) || durationMinutes <= 0) return false
	return parseTimeToMinutes(endTime.value) < parseTimeToMinutes(startTime.value)
}

const toggleTimeGroup = (groupKey) => {
	timeGroupExpandedState.value = {
		...timeGroupExpandedState.value,
		[groupKey]: !timeGroupExpandedState.value[groupKey]
	}
}

const syncSelectedTimesForDate = () => {
	const availableStartTimes = getAvailableTimeOptions(selectedDate.value, 'start').map(option => option.time)
	if (startTime.value && !availableStartTimes.includes(startTime.value)) {
		startTime.value = ''
		endTime.value = ''
		return
	}

	const availableEndTimes = getAvailableTimeOptions(selectedDate.value, 'end').map(option => option.time)
	if (endTime.value && (!availableEndTimes.includes(endTime.value) || calculateSlotDurationMinutes(startTime.value, endTime.value) <= 0)) {
		endTime.value = ''
	}
}

const confirmTime = () => {
	if (!selectedTime.value) {
		uni.showToast({
			title: '请选择时间',
			icon: 'none'
		})
		return
	}
	if (timePickerType.value === 'start') {
		if (startTime.value !== selectedTime.value) {
			endTime.value = ''
		}
		startTime.value = selectedTime.value
		timePickerType.value = 'end'
		selectedTime.value = endTime.value
		uni.showToast({
			title: '请选择结束时间',
			icon: 'none'
		})
		return
	} else {
		const durationMinutes = calculateSlotDurationMinutes(startTime.value, selectedTime.value)
		if (Number.isNaN(durationMinutes) || durationMinutes <= 0) {
			uni.showToast({
				title: '结束时间必须晚于开始时间',
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
	const date = createSafeDate(dateStr)
	if (Number.isNaN(date.getTime())) return dateStr
	const year = date.getFullYear()
	const month = date.getMonth() + 1
	const day = date.getDate()
	return `${year}年${month}月${day}日`
}

const createSafeDate = (dateStr) => {
	const normalizedDate = String(dateStr || '').trim().replace(/-/g, '/')
	return new Date(normalizedDate)
}

const parseTimeToMinutes = (timeValue) => {
	const normalizedTime = String(timeValue || '').trim()
	if (!normalizedTime) return NaN
	const startSegment = normalizedTime.includes('-') ? normalizedTime.split('-')[0].trim() : normalizedTime
	const match = startSegment.match(/^(\d{1,2}):(\d{2})$/)
	if (!match) return NaN
	const hour = Number(match[1])
	const minute = Number(match[2])
	if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return NaN
	return hour * 60 + minute
}

const validateAppointmentDateTime = () => {
	if (!selectedDate.value) {
		return { valid: false, message: '请选择服务日期' }
	}

	const appointmentDate = createSafeDate(selectedDate.value)
	if (Number.isNaN(appointmentDate.getTime())) {
		return { valid: false, message: '服务日期格式错误，请重新选择' }
	}
	appointmentDate.setHours(0, 0, 0, 0)

	const now = new Date()
	const today = new Date(now)
	today.setHours(0, 0, 0, 0)

	if (appointmentDate.getTime() < today.getTime()) {
		return { valid: false, message: '不能预约过去的日期' }
	}

	if (!startTime.value) {
		return { valid: false, message: '请选择开始时间' }
	}

	const startMinutes = parseTimeToMinutes(startTime.value)
	if (Number.isNaN(startMinutes)) {
		return { valid: false, message: '服务开始时间格式错误，请重新选择' }
	}

	if (appointmentDate.getTime() === today.getTime()) {
		const minAllowedStartMinutes = getTodayEarliestStartMinutes()
		if (startMinutes < minAllowedStartMinutes) {
			return {
				valid: false,
				message: getExpiredStartTimeMessage(minAllowedStartMinutes),
				expiredStartTime: true,
				minAllowedStartMinutes
			}
		}
	}

	return { valid: true, message: '', expiredStartTime: false, minAllowedStartMinutes: null }
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

const getInputValue = (event) => String(event?.detail?.value ?? event?.target?.value ?? '')

const handlePatientNameInput = (event) => {
	const nextValue = normalizeContactName(getInputValue(event))
	patientName.value = nextValue
	return nextValue
}

const handlePhoneInput = (event) => {
	const nextValue = normalizeContactPhone(getInputValue(event))
	phoneNumber.value = nextValue
	if (!phoneNumber.value || phoneNumber.value.length === 11) {
		validatePhone()
	} else {
		phoneError.value = ''
		isPhoneValid.value = false
	}
	return nextValue
}

const isValidPhone = computed(() => {
	const phoneRegex = /^1[3-9]\d{9}$/
	return !!phoneNumber.value && phoneRegex.test(String(phoneNumber.value).trim())
})

const normalizeSymptom = (symptom) => String(symptom || '').trim()

const getSelectedSymptoms = () => selectedSymptoms.value
	.map(normalizeSymptom)
	.filter(Boolean)

const isSymptomSelected = (symptom) => {
	const normalizedSymptom = normalizeSymptom(symptom)
	return !!normalizedSymptom && getSelectedSymptoms().includes(normalizedSymptom)
}

// 除“其他需求”外，其余都必须填写/选择（含症状至少选择1项）
const isFormComplete = computed(() => {
	const appointmentTimeValidation = validateAppointmentDateTime()
	return (
		!!selectedDate.value &&
		!!startTime.value &&
		!!endTime.value &&
		!!String(hospitalAddress.value || '').trim() &&
		!!String(patientName.value || '').trim() &&
		isValidPhone.value &&
		getSelectedSymptoms().length > 0 &&
		appointmentTimeValidation.valid
	)
})

// --- 获取服务图标路径 ---
const getServiceIcon = (typeNumber) => {
	return appointmentServiceLogos[typeNumber] || appointmentServiceLogos[1]
}

// --- 新增：症状选择方法 ---
const toggleSymptom = (symptom) => {
	const normalizedSymptom = normalizeSymptom(symptom)
	if (!normalizedSymptom) {
		return
	}
	const nextSelectedSymptoms = getSelectedSymptoms()
	if (nextSelectedSymptoms.includes(normalizedSymptom)) {
		selectedSymptoms.value = nextSelectedSymptoms.filter(item => item !== normalizedSymptom)
	} else {
		selectedSymptoms.value = [...nextSelectedSymptoms, normalizedSymptom]
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
	const normalizedNewSymptom = normalizeSymptom(newSymptomInput.value)
	if (!normalizedNewSymptom) {
		newSymptomError.value = '请输入症状描述'
		return false
	}
	if (normalizedNewSymptom.length > 50) {
		newSymptomError.value = '症状描述不能超过50个字符'
		return false
	}
	// 检查是否已经存在
	const normalizedSymptomOptions = symptoms.value.map(normalizeSymptom)
	if (normalizedSymptomOptions.includes(normalizedNewSymptom)) {
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
	const normalizedNewSymptom = normalizeSymptom(newSymptomInput.value)
	// 添加到症状列表
	symptoms.value.push(normalizedNewSymptom)
	selectedSymptoms.value = [...getSelectedSymptoms(), normalizedNewSymptom]
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
	if (!String(hospitalAddress.value || '').trim()) missing.push('就诊医院')
	if (getSelectedSymptoms().length === 0) missing.push('症状（至少选择1项）')
	if (!String(patientName.value || '').trim()) missing.push('姓名')
	if (!String(phoneNumber.value || '').trim()) missing.push('手机号')

	if (missing.length > 0) {
		formError.value = `请先完善：${missing.join('、')}`
		return
	}
	formError.value = ''

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
	const durationMinutes = calculateSlotDurationMinutes(startTime.value, endTime.value)
	if (Number.isNaN(durationMinutes) || durationMinutes <= 0) {
		uni.showToast({ title: '结束时间需晚于开始时间', icon: 'none' })
		return
	}

	const appointmentTimeValidation = validateAppointmentDateTime()
	if (!appointmentTimeValidation.valid) {
		if (appointmentTimeValidation.expiredStartTime) {
			startTime.value = ''
			endTime.value = ''
			selectedTime.value = ''
		}
		uni.showToast({
			title: appointmentTimeValidation.message,
			icon: 'none'
		})
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
		symptoms: getSelectedSymptoms(),
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

			// 2. 普通预约不指定陪诊师，支付后进入公共接单大厅，状态为待接单。
			const orderRequest = {
				appointmentNo: appointmentNo
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
					url: '/subpkg/appointment-flow/04-order-confirm-page?orderNo=' + orderNo
				});
			} else {
				throw new Error(orderResponse.message || '创建订单失败');
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
			// 仅 H5 存在 document，小程序环境直接使用响应式数据即可
			if (typeof document !== 'undefined' && document.documentElement?.style) {
				document.documentElement.style.setProperty('--status-bar-height', `${res.statusBarHeight}px`)
			}
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
				validatePhone()
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

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
/* 原始样式保持不变 */
.appointment-time-container {
	min-height: 100vh;
	background-color: #f5f7fa;
	padding-bottom: 150rpx;
	padding-top: 30rpx;
	width: 100%;
	max-width: 100%;
	overflow-x: clip;
	box-sizing: border-box;
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
	border: 2rpx solid #007AFF; /* 边框，突出卡片 */
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

.date-card-shell,
.hospital-card-shell {
	background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
	border: 2rpx solid #e4eefc;
	border-radius: 28rpx;
	padding: 26rpx 24rpx 22rpx;
	box-shadow: 0 10rpx 28rpx rgba(37, 99, 235, 0.07);
}

.date-card-main,
.hospital-card-main {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 20rpx;
}

.date-copy,
.hospital-copy {
	flex: 1;
	min-width: 0;
}

.date-label,
.hospital-label {
	display: block;
	font-size: 24rpx;
	font-weight: 600;
	color: #7a8ca5;
	letter-spacing: 1rpx;
	margin-bottom: 12rpx;
}

.date-card-value,
.hospital-card-value {
	display: block;
	font-size: 32rpx;
	line-height: 1.4;
	font-weight: 700;
	color: #1f2937;
	word-break: break-all;
}

.date-card-value.placeholder,
.hospital-card-value.placeholder {
	color: #a0aec0;
	font-weight: 500;
}

.date-badge,
.hospital-badge {
	flex-shrink: 0;
	min-width: 108rpx;
	height: 54rpx;
	padding: 0 22rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 8rpx 20rpx rgba(37, 99, 235, 0.2);
}

.date-badge-text,
.hospital-badge-text {
	font-size: 24rpx;
	font-weight: 700;
	color: #fff;
}

.hospital-card-meta {
	margin-top: 18rpx;
	padding-top: 18rpx;
	border-top: 1rpx solid #ebf2fb;
}

.hospital-meta-text {
	font-size: 24rpx;
	color: #8b9bb1;
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
	flex-wrap: wrap;
}

.time-inline-tip {
	width: 100%;
	font-size: 24rpx;
	line-height: 1.6;
	color: #6b7c93;
	margin-top: 6rpx;
}

.time-picker {
	flex: 1;
	background-color: #f8f9fa;
	border-radius: 15rpx;
	padding: 26rpx 16rpx;
	display: flex;
	align-items: center;
	gap: 10rpx;
	border: 2rpx solid #e9ecef;
}

.time-label {
	font-size: 26rpx;
	color: #666;
	flex-shrink: 0;
}

.time-value {
	font-size: 26rpx;
	color: #333;
	font-weight: 600;
	flex: 1;
	min-width: 0;
	text-align: right;
	white-space: nowrap;
	line-height: 1.2;
}

.time-value-compact {
	font-size: 22rpx;
	letter-spacing: 0;
}

.time-placeholder {
	font-size: 26rpx;
	color: #999;
	flex: 1;
	min-width: 0;
	text-align: right;
	white-space: nowrap;
	line-height: 1.2;
}

.time-arrow {
	font-size: 24rpx;
	color: #999;
	flex-shrink: 0;
}

/* 地址输入区域 */
.location-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加阴影 */
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
	background-color: #fff;
	transition: all 0.2s ease;
}

.symptom-item.selected {
	border-color: #007AFF;
	background-color: #F0F7FF;
	box-shadow: 0 4rpx 12rpx rgba(0, 122, 255, 0.08);
}

.checkbox-indicator {
	width: 36rpx;
	height: 36rpx;
	border-radius: 50%;
	border: 2rpx solid #cdd6e1;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #fff;
	flex-shrink: 0;
}

.checkbox-indicator.selected {
	border-color: #007AFF;
	background: linear-gradient(135deg, #007AFF, #2563EB);
}

.checkbox-mark {
	font-size: 20rpx;
	line-height: 1;
	color: #fff;
	font-weight: 700;
}

.symptom-text {
	color: #333;
	flex: 1;
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
	width: 100%;
	max-width: 100%;
	box-sizing: border-box;
	overflow: hidden;
}

.requirements-textarea {
	display: block;
	width: 100%;
	max-width: 100%;
	min-width: 0;
	min-height: 100rpx; /* 设置最小高度 */
	padding: 25rpx 0;
	font-size: 28rpx;
	color: #333;
	background: transparent;
	border: none;
	outline: none;
	box-sizing: border-box;
	white-space: pre-wrap;
	word-wrap: break-word;
	word-break: break-all;
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
  box-sizing: border-box;
  width: 100%;
  overflow: hidden;
}


.contact-input {
  display: block;
  width: 100%;
  min-width: 0;
  height: 86rpx;
  padding: 25rpx 0; /* 与地址输入框一致的上下内边距 */
  line-height: 36rpx;
  font-size: 28rpx;
  color: #333;
  background: transparent;
  border: none;
  outline: none;
  box-sizing: border-box;
  caret-color: #007AFF;
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
	z-index: 40;
	background-color: #ffffff;
	padding: 30rpx 30rpx calc(30rpx + env(safe-area-inset-bottom));
	border-top: 1rpx solid #eee;
	box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.05); /* 添加底部阴影 */
	box-sizing: border-box;
}

.form-error-card {
	margin-bottom: 18rpx;
	padding: 18rpx 22rpx;
	border-radius: 22rpx;
	background: #fff6f6;
	border: 1rpx solid rgba(239, 68, 68, 0.22);
	box-shadow: 0 8rpx 22rpx rgba(239, 68, 68, 0.08);
	display: flex;
	flex-direction: column;
	gap: 6rpx;
	box-sizing: border-box;
}

.form-error-title {
	color: #d83b3b;
	font-size: 25rpx;
	font-weight: 800;
	line-height: 1.35;
}

.form-error-desc {
	color: #8c5d5d;
	font-size: 24rpx;
	line-height: 1.45;
	word-break: break-word;
}

.confirm-btn {
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

.confirm-btn.disabled {
	background: #d9d9d9;
	color: #ffffff;
	opacity: 0.85;
}

/* 弹窗样式 */
.modal-overlay {
	position: fixed;
	inset: 0;
	background-color: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 99990;
	padding: 24rpx 20rpx calc(24rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.date-modal, .time-modal, .add-symptom-modal, .hospital-modal {
	background-color: #ffffff;
	border-radius: 20rpx;
	width: 100%;
	max-width: 720rpx;
	max-height: calc(100dvh - 48rpx - env(safe-area-inset-bottom));
	overflow: hidden;
	box-sizing: border-box;
}

.time-modal {
	max-height: 82%;
	border-radius: 28rpx;
}

.calendar-modal-simple {
	background-color: #ffffff;
	border-radius: 28rpx;
	width: 100%;
	max-width: 720rpx;
	max-height: calc(100dvh - 48rpx - env(safe-area-inset-bottom));
	overflow: hidden;
	box-sizing: border-box;
}

.calendar-modal-header,
.hospital-modal-header,
.time-modal-header {
	padding: 28rpx 30rpx 24rpx;
	border-bottom: 1rpx solid #eef3f8;
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 20rpx;
}

.calendar-modal-title,
.hospital-modal-title,
.time-modal-title {
	display: block;
	font-size: 32rpx;
	font-weight: 700;
	color: #1f2937;
}

.calendar-modal-subtitle,
.hospital-modal-subtitle,
.time-modal-subtitle {
	display: block;
	margin-top: 10rpx;
	font-size: 24rpx;
	color: #8b9bb1;
}

.cal-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 24rpx 30rpx 18rpx;
}

.cal-month {
	font-size: 30rpx;
	font-weight: 700;
	color: #1f2937;
}

.cal-nav {
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.cal-btn {
	width: 56rpx;
	height: 56rpx;
	border-radius: 50%;
	background: #f1f6fd;
	color: #2563eb;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 34rpx;
	font-weight: 700;
}

.cal-weekdays {
	display: grid;
	grid-template-columns: repeat(7, 1fr);
	padding: 0 20rpx;
}

.cal-wd {
	text-align: center;
	font-size: 24rpx;
	color: #94a3b8;
	padding: 12rpx 0;
}

.cal-days {
	display: grid;
	grid-template-columns: repeat(7, 1fr);
	row-gap: 12rpx;
	padding: 12rpx 20rpx 24rpx;
}

.cal-day {
	height: 76rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	border-radius: 22rpx;
}

.cal-day.empty {
	visibility: hidden;
}

.cal-day.disabled .cal-d {
	color: #cbd5e1;
}

.cal-day.today {
	background: #eef6ff;
}

.cal-day.selected {
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
	box-shadow: 0 10rpx 20rpx rgba(37, 99, 235, 0.2);
}

.cal-day.selected .cal-d {
	color: #ffffff;
	font-weight: 700;
}

.cal-d {
	font-size: 28rpx;
	color: #334155;
}

.cal-footer,
.hospital-footer {
	display: flex;
	gap: 20rpx;
	padding: 24rpx 30rpx 30rpx;
	border-top: 1rpx solid #eef3f8;
}

.cal-cancel,
.cal-ok {
	flex: 1;
	border-radius: 999rpx;
	font-size: 28rpx;
	height: 82rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}

.cal-cancel {
	background: #f3f6fb;
	color: #64748b;
}

.cal-ok {
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
	color: #ffffff;
}


.hospital-modal-animated {
	transform-origin: center bottom;
	animation: hospitalModalIn 0.22s cubic-bezier(0.22, 1, 0.36, 1);
	will-change: transform, opacity;
}

@keyframes hospitalModalIn {
	0% {
		opacity: 0;
		transform: translateY(28rpx) scale(0.96);
	}
	100% {
		opacity: 1;
		transform: translateY(0) scale(1);
	}
}
.hospital-search-shell {
	width: 100%;
	box-sizing: border-box;
	padding: 24rpx 30rpx 18rpx;
}

.hospital-search-input {
	width: 100%;
	height: 88rpx;
	background: #f8fafc;
	border: 2rpx solid #e2e8f0;
	border-radius: 22rpx;
	padding: 0 26rpx;
	font-size: 28rpx;
	color: #1f2937;
	box-sizing: border-box;
}

.hospital-list {
	width: 100%;
	box-sizing: border-box;
	max-height: 620rpx;
	padding: 0 30rpx 18rpx;
}

.hospital-option {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
	padding: 24rpx 22rpx;
	border-radius: 22rpx;
	background: #f8fafc;
	border: 2rpx solid transparent;
	margin-bottom: 18rpx;
	box-sizing: border-box;
	width: 100%;
	transition: background-color 0.2s ease, border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.hospital-option.selected {
	background: #eef6ff;
	border-color: #93c5fd;
	box-shadow: 0 10rpx 24rpx rgba(37, 99, 235, 0.08);
	transform: translateY(-2rpx);
}

.hospital-option.custom {
	background: #fffaf0;
}

.hospital-option-copy {
	flex: 1;
	min-width: 0;
}

.hospital-option-name {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: #1f2937;
	line-height: 1.4;
	word-break: break-word;
}

.hospital-option-desc {
	display: block;
	margin-top: 8rpx;
	font-size: 24rpx;
	color: #94a3b8;
}

.hospital-option-check {
	flex-shrink: 0;
	width: 32rpx;
	text-align: center;
	font-size: 30rpx;
	font-weight: 700;
	color: #2563eb;
	opacity: 0;
	transform: scale(0.8);
	transition: opacity 0.18s ease, transform 0.18s ease;
}

.hospital-option-check.visible {
	opacity: 1;
	transform: scale(1);
}

.hospital-empty {
	padding: 50rpx 24rpx 60rpx;
	text-align: center;
}

.hospital-empty-text {
	font-size: 26rpx;
	color: #94a3b8;
	line-height: 1.6;
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
	max-height: 620rpx;
	padding: 22rpx 24rpx 16rpx;
	box-sizing: border-box;
}

.time-empty-state {
	padding: 48rpx 24rpx 52rpx;
	text-align: center;
}

.time-empty-text {
	font-size: 26rpx;
	line-height: 1.7;
	color: #94a3b8;
}

.time-group {
	margin-bottom: 20rpx;
	border-radius: 26rpx;
	background: linear-gradient(180deg, #f8fbff 0%, #f4f8fd 100%);
	border: 2rpx solid #e5edf8;
	overflow: hidden;
	box-shadow: 0 8rpx 24rpx rgba(37, 99, 235, 0.06);
}

.time-group-header {
	display: flex;
	align-items: center;
	gap: 16rpx;
	padding: 24rpx 24rpx 22rpx;
}

.time-group-copy {
	flex: 1;
	min-width: 0;
}

.time-group-title {
	display: block;
	font-size: 28rpx;
	font-weight: 700;
	color: #1f2937;
}

.time-group-meta {
	display: block;
	margin-top: 8rpx;
	font-size: 23rpx;
	color: #8b9bb1;
}

.time-group-badge {
	flex-shrink: 0;
	min-width: 52rpx;
	height: 44rpx;
	padding: 0 14rpx;
	border-radius: 999rpx;
	background: rgba(37, 99, 235, 0.1);
	display: flex;
	align-items: center;
	justify-content: center;
}

.time-group-count {
	font-size: 22rpx;
	font-weight: 700;
	color: #2563eb;
}

.time-group-arrow {
	flex-shrink: 0;
	font-size: 28rpx;
	line-height: 1;
	color: #94a3b8;
	transform: rotate(0deg);
	transition: transform 0.22s ease;
}

.time-group-arrow.expanded {
	transform: rotate(180deg);
}

.time-group-options {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 16rpx;
	padding: 0 24rpx 24rpx;
}

.time-sequence-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 16rpx;
	padding: 8rpx 8rpx 4rpx;
}

.time-option {
	padding: 26rpx 18rpx;
	border-radius: 22rpx;
	background: #ffffff;
	border: 2rpx solid transparent;
	text-align: center;
	color: #334155;
	transition: all 0.3s ease;
	box-sizing: border-box;
	min-width: 0;
}

.sequence-option {
	padding: 24rpx 16rpx;
}

.time-option.selected {
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
	border-color: rgba(59, 130, 246, 0.24);
	box-shadow: 0 12rpx 24rpx rgba(37, 99, 235, 0.18);
}

.time-option-text {
	font-size: 27rpx;
	font-weight: 600;
	color: inherit;
}

.time-option.selected .time-option-text {
	color: #fff;
}

.time-footer {
	display: flex;
	padding: 24rpx 30rpx 30rpx;
	gap: 20rpx;
	border-top: 1rpx solid #eef3f8;
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
	background: linear-gradient(135deg, #007AFF, #2563EB);
	color: white;
	border: none;
	border-radius: 25rpx;
	padding: 25rpx 0;
	font-size: 28rpx;
	font-weight: 600;
}

.symptom-sheet-overlay {
	align-items: flex-end;
	padding: 24rpx 20rpx calc(20rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.symptom-sheet {
	width: 100%;
	background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
	border-radius: 34rpx 34rpx 28rpx 28rpx;
	box-shadow: 0 -16rpx 44rpx rgba(15, 23, 42, 0.12), 0 18rpx 50rpx rgba(15, 23, 42, 0.18);
	overflow: hidden;
	animation: symptomSheetRise 0.28s cubic-bezier(0.2, 0.9, 0.2, 1);
}

.symptom-sheet-handle {
	width: 88rpx;
	height: 10rpx;
	border-radius: 999rpx;
	background: rgba(148, 163, 184, 0.35);
	margin: 16rpx auto 0;
}

.symptom-sheet-header {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 20rpx;
	padding: 22rpx 28rpx 20rpx;
}

.symptom-sheet-copy {
	display: flex;
	flex-direction: column;
	gap: 10rpx;
	flex: 1;
	min-width: 0;
}

.symptom-sheet-eyebrow {
	font-size: 22rpx;
	line-height: 1;
	letter-spacing: 2rpx;
	font-weight: 700;
	color: #2563eb;
}

.symptom-sheet-title {
	font-size: 38rpx;
	line-height: 1.14;
	font-weight: 700;
	color: #16324f;
}

.symptom-sheet-subtitle {
	font-size: 24rpx;
	line-height: 1.7;
	color: #5d738b;
}

.symptom-sheet-close {
	width: 64rpx;
	height: 64rpx;
	border-radius: 999rpx;
	background: rgba(148, 163, 184, 0.12);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}

.symptom-sheet-close-icon {
	font-size: 34rpx;
	color: #64748b;
	line-height: 1;
}

.symptom-sheet-body {
	padding: 4rpx 28rpx 20rpx;
}

.symptom-input-card {
	padding: 24rpx;
	border-radius: 26rpx;
	background: #ffffff;
	border: 1rpx solid rgba(220, 232, 248, 0.92);
	box-shadow: 0 10rpx 28rpx rgba(18, 56, 109, 0.08);
	box-sizing: border-box;
}

.symptom-input-card.error {
	border-color: rgba(239, 68, 68, 0.45);
	box-shadow: 0 10rpx 28rpx rgba(239, 68, 68, 0.08);
}

.symptom-input-label-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
	margin-bottom: 18rpx;
}

.symptom-input-badge {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	min-height: 40rpx;
	padding: 0 16rpx;
	border-radius: 999rpx;
	background: rgba(0, 122, 255, 0.10);
	color: #007aff;
	font-size: 22rpx;
	font-weight: 700;
}

.symptom-input-hint {
	font-size: 22rpx;
	color: #8aa0b8;
}

.symptom-input-field {
	display: flex;
	align-items: center;
	gap: 16rpx;
	padding: 22rpx 20rpx;
	border-radius: 22rpx;
	background: linear-gradient(180deg, #f7fbff 0%, #f0f7ff 100%);
	border: 1rpx solid rgba(214, 234, 255, 0.9);
}

.symptom-input-icon {
	font-size: 28rpx;
	color: #007aff;
	line-height: 1;
	flex-shrink: 0;
}

.symptom-input-control {
	flex: 1;
	width: 100%;
	font-size: 28rpx;
	line-height: 1.4;
	color: #16324f;
	background: transparent;
	border: none;
}

.symptom-sheet-error {
	display: block;
	margin-top: 16rpx;
	padding-left: 8rpx;
	font-size: 24rpx;
	line-height: 1.5;
	color: #ef4444;
}

.symptom-sheet-footer {
	display: flex;
	gap: 18rpx;
	padding: 18rpx 28rpx calc(22rpx + env(safe-area-inset-bottom));
	background: rgba(255, 255, 255, 0.92);
	backdrop-filter: blur(16rpx);
	border-top: 1rpx solid rgba(220, 232, 248, 0.85);
}

.symptom-sheet-secondary-btn,
.symptom-sheet-primary-btn {
	flex: 1;
	height: 88rpx;
	line-height: 88rpx;
	border-radius: 999rpx;
	font-size: 28rpx;
	font-weight: 600;
	border: none;
}

.symptom-sheet-secondary-btn {
	background: #ffffff;
	color: #007aff;
	border: 1rpx solid rgba(0, 122, 255, 0.22);
	box-shadow: 0 8rpx 20rpx rgba(148, 163, 184, 0.08);
}

.symptom-sheet-primary-btn {
	background: linear-gradient(135deg, #007aff 0%, #2563eb 100%);
	color: #ffffff;
	box-shadow: 0 16rpx 32rpx rgba(37, 99, 235, 0.24);
}

@keyframes symptomSheetRise {
	from {
		opacity: 0;
		transform: translateY(44rpx) scale(0.98);
	}
	to {
		opacity: 1;
		transform: translateY(0) scale(1);
	}
}

</style>
