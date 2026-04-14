<template>
	<view class="time-section" :class="{ compact }">
		<view v-if="!hideHeader" class="section-header">
			<view class="icon-wrapper">
				<text class="time-icon">🕐</text>
			</view>
			<text class="section-title">选择服务时段</text>
		</view>
		
		<view class="time-picker-row">
			<view class="time-picker" @click="showStartTimePicker">
				<text class="time-label">开始时间</text>
				<text class="time-value" v-if="startTimeModel">{{ startTimeModel }}</text>
				<text class="time-placeholder" v-else>请选择</text>
				<text class="time-arrow">▼</text>
			</view>
			<view class="time-picker" @click="showEndTimePicker">
				<text class="time-label">结束时间</text>
				<text :class="['time-value', { 'time-value-compact': isNextDaySelectedEndTime() }]" v-if="endTimeModel">{{ formatSelectedEndTimeDisplay() }}</text>
				<text class="time-placeholder" v-else>请选择</text>
				<text class="time-arrow">▼</text>
			</view>
		</view>
		<text class="time-inline-tip">今天的开始时间会随当前时间变化，请尽快确认预约</text>

		<view class="modal-overlay" v-if="showTimeModal" @click="hideTimePicker">
			<view class="time-modal" @click.stop>
				<view class="time-modal-header">
					<view>
						<text class="time-modal-title">{{ timePickerTitle }}</text>
						<text class="time-modal-subtitle">24小时可预约，结束时间可跨至次日</text>
					</view>
					<text class="close-btn" @click="hideTimePicker">✕</text>
				</view>
				
				<scroll-view
					class="time-list"
					scroll-y
					:scroll-into-view="timeScrollTarget"
					scroll-with-animation
				>
					<view v-if="timeOptions.length === 0" class="time-empty-state">
						<text class="time-empty-text">当前日期已无可预约时段，请选择其他日期</text>
					</view>
					<template v-if="timePickerType === 'start'">
						<view
							v-for="group in timeGroups"
							:key="group.key"
							:id="group.anchorId"
							class="time-group"
						>
							<view class="time-group-header" @click="toggleTimeGroup(group.key)">
								<view class="time-group-copy">
									<text class="time-group-title">{{ group.label }}</text>
									<text class="time-group-meta">{{ group.rangeLabel }}</text>
								</view>
								<view class="time-group-badge">
									<text class="time-group-count">{{ group.options.length }}</text>
								</view>
								<text class="time-group-arrow" :class="{ expanded: isTimeGroupExpanded(group.key) }">⌄</text>
							</view>

							<view v-if="isTimeGroupExpanded(group.key)" class="time-group-options">
								<view
									v-for="option in group.options"
									:key="`${group.key}-${option.time}-${option.isNextDay ? 'next' : 'same'}`"
									:class="['time-option', { 'selected': isSelectedTimeOption(option) }]"
									@click="selectTime(option)"
								>
									<text class="time-option-text">{{ option.displayLabel }}</text>
								</view>
							</view>
						</view>
					</template>
					<view v-else class="time-sequence-grid">
						<view
							v-for="option in timeOptions"
							:key="`end-${option.time}-${option.isNextDay ? 'next' : 'same'}`"
							:class="['time-option', 'sequence-option', { 'selected': isSelectedTimeOption(option) }]"
							@click="selectTime(option)"
						>
							<text class="time-option-text">{{ option.displayLabel }}</text>
						</view>
					</view>
				</scroll-view>
				
				<view class="time-footer">
					<button class="cancel-btn" @click="hideTimePicker">取消</button>
					<button class="confirm-time-btn" @click="confirmTime">确定</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
	selectedDate: {
		type: String,
		default: ''
	},
	compact: {
		type: Boolean,
		default: false
	},
	hideHeader: {
		type: Boolean,
		default: false
	},
	startTime: {
		type: String,
		default: ''
	},
	endTime: {
		type: String,
		default: ''
	}
})

const emit = defineEmits(['update:startTime', 'update:endTime'])

const startTimeModel = computed({
	get: () => props.startTime || '',
	set: (value) => emit('update:startTime', value)
})

const endTimeModel = computed({
	get: () => props.endTime || '',
	set: (value) => emit('update:endTime', value)
})

const showTimeModal = ref(false)
const timePickerType = ref('') // 'start' 或 'end'
const selectedTime = ref('')
const timeGroupExpandedState = ref({})
const timeScrollTarget = ref('')

const TIME_GROUP_DEFINITIONS = [
	{ key: 'lateNight', label: '凌晨', rangeLabel: '00:00 - 05:30', start: 0, end: 359 },
	{ key: 'morning', label: '上午', rangeLabel: '06:00 - 11:30', start: 360, end: 719 },
	{ key: 'afternoon', label: '下午', rangeLabel: '12:00 - 17:30', start: 720, end: 1079 },
	{ key: 'evening', label: '晚上', rangeLabel: '18:00 - 23:30', start: 1080, end: 1439 }
]

const timePickerTitle = computed(() => {
	return timePickerType.value === 'start' ? '选择开始时间' : '选择结束时间'
})

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

const formatMinutesToTime = (minutes) => {
	const safeMinutes = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
	const hour = Math.floor(safeMinutes / 60)
	const minute = safeMinutes % 60
	return `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`
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

const isTodayDate = (dateValue) => {
	if (!dateValue) return false
	const appointmentDate = createSafeDate(dateValue)
	if (Number.isNaN(appointmentDate.getTime())) return false
	appointmentDate.setHours(0, 0, 0, 0)
	const today = new Date()
	today.setHours(0, 0, 0, 0)
	return appointmentDate.getTime() === today.getTime()
}

const getExpiredStartTimeMessage = (minAllowedStartMinutes) =>
	`您选择的开始时间已超过可预约时限，请重新选择 ${formatMinutesToTime(minAllowedStartMinutes)} 及之后的开始时间`

const getAvailableTimeOptions = (dateValue, pickerType = 'start') => {
	const options = []
	const selectedStartMinutes = parseTimeToMinutes(startTimeModel.value)
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

const timeOptions = computed(() => getAvailableTimeOptions(props.selectedDate, timePickerType.value))

const findTimeGroupByMinutes = (minutes) => TIME_GROUP_DEFINITIONS.find(group => minutes >= group.start && minutes <= group.end) || null

const findPreferredTimeGroupKey = (options) => {
	if (timePickerType.value === 'end' && startTimeModel.value) {
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

const selectTime = (option) => {
	selectedTime.value = option.time
}

const isNextDayEndTimeOption = (timeValue) => {
	if (timePickerType.value !== 'end' || !startTimeModel.value) return false
	const durationMinutes = calculateSlotDurationMinutes(startTimeModel.value, timeValue)
	const endMinutes = parseTimeToMinutes(timeValue)
	const startMinutes = parseTimeToMinutes(startTimeModel.value)
	return !Number.isNaN(durationMinutes) && durationMinutes > 0 && endMinutes < startMinutes
}

const isSelectedTimeOption = (option) => {
	if (!option || selectedTime.value !== option.time) return false
	if (timePickerType.value !== 'end') return true
	return isNextDayEndTimeOption(option.time) === option.isNextDay
}

const formatSelectedEndTimeDisplay = () => {
	if (!endTimeModel.value) return ''
	if (!startTimeModel.value) return endTimeModel.value
	const durationMinutes = calculateSlotDurationMinutes(startTimeModel.value, endTimeModel.value)
	if (Number.isNaN(durationMinutes) || durationMinutes <= 0) {
		return endTimeModel.value
	}
	return parseTimeToMinutes(endTimeModel.value) < parseTimeToMinutes(startTimeModel.value)
		? `次日${endTimeModel.value}`
		: endTimeModel.value
}

const isNextDaySelectedEndTime = () => {
	if (!startTimeModel.value || !endTimeModel.value) return false
	const durationMinutes = calculateSlotDurationMinutes(startTimeModel.value, endTimeModel.value)
	if (Number.isNaN(durationMinutes) || durationMinutes <= 0) return false
	return parseTimeToMinutes(endTimeModel.value) < parseTimeToMinutes(startTimeModel.value)
}

const toggleTimeGroup = (groupKey) => {
	timeGroupExpandedState.value = {
		...timeGroupExpandedState.value,
		[groupKey]: !timeGroupExpandedState.value[groupKey]
	}
}

const syncSelectedTimesForDate = () => {
	const availableStartTimes = getAvailableTimeOptions(props.selectedDate, 'start').map(option => option.time)
	if (startTimeModel.value && !availableStartTimes.includes(startTimeModel.value)) {
		startTimeModel.value = ''
		endTimeModel.value = ''
		return
	}

	const availableEndTimes = getAvailableTimeOptions(props.selectedDate, 'end').map(option => option.time)
	if (endTimeModel.value && (!availableEndTimes.includes(endTimeModel.value) || calculateSlotDurationMinutes(startTimeModel.value, endTimeModel.value) <= 0)) {
		endTimeModel.value = ''
	}
}

const showStartTimePicker = () => {
	if (!props.selectedDate) {
		uni.showToast({
			title: '请先选择服务日期',
			icon: 'none'
		})
		return
	}
	timePickerType.value = 'start'
	selectedTime.value = startTimeModel.value
	showTimeModal.value = true
	initializeTimeGroupState()
}

const showEndTimePicker = () => {
	if (!props.selectedDate) {
		uni.showToast({
			title: '请先选择服务日期',
			icon: 'none'
		})
		return
	}
	if (!startTimeModel.value) {
		uni.showToast({
			title: '请先选择开始时间',
			icon: 'none'
		})
		return
	}
	timePickerType.value = 'end'
	selectedTime.value = endTimeModel.value
	showTimeModal.value = true
	initializeTimeGroupState()
}

const hideTimePicker = () => {
	showTimeModal.value = false
	selectedTime.value = ''
	timeScrollTarget.value = ''
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
		if (startTimeModel.value !== selectedTime.value) {
			endTimeModel.value = ''
		}
		startTimeModel.value = selectedTime.value
		timePickerType.value = 'end'
		selectedTime.value = endTimeModel.value
		uni.showToast({
			title: '请选择结束时间',
			icon: 'none'
		})
		return
	} else {
		const durationMinutes = calculateSlotDurationMinutes(startTimeModel.value, selectedTime.value)
		if (Number.isNaN(durationMinutes) || durationMinutes <= 0) {
			uni.showToast({
				title: '结束时间必须晚于开始时间',
				icon: 'none'
			})
			return
		}
		endTimeModel.value = selectedTime.value
	}
	hideTimePicker()
}

watch(
	() => props.selectedDate,
	() => {
		syncSelectedTimesForDate()
	},
	{ immediate: true }
)

const createSafeDate = (dateStr) => {
	const normalizedDate = String(dateStr || '').trim().replace(/-/g, '/')
	return new Date(normalizedDate)
}
</script>

<style scoped lang="scss">
.time-section {
	background-color: #ffffff;
	margin: 20rpx 30rpx;
	border-radius: 20rpx;
	padding: 40rpx 30rpx;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
}

.time-section.compact {
	margin: 0;
	padding: 0;
	box-shadow: none;
	background: transparent;
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

.time-icon {
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
	letter-spacing: -0.5rpx;
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

.time-modal {
	background-color: #ffffff;
	border-radius: 20rpx;
	width: 86%;
	max-height: 82%;
	overflow: hidden;
}

.time-modal-header {
	padding: 28rpx 30rpx 24rpx;
	border-bottom: 1rpx solid #eef3f8;
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 20rpx;
}

.time-modal-title {
	display: block;
	font-size: 32rpx;
	font-weight: 700;
	color: #1f2937;
}

.time-modal-subtitle {
	display: block;
	margin-top: 10rpx;
	font-size: 24rpx;
	color: #8b9bb1;
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
</style>
