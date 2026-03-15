<template>
	<view class="page-wrap" :style="{ paddingTop: statusBarHeight + 'px' }">
		<view class="header">
			<view class="search-box">
				<image class="search-icon" src="/static/sous.png" mode="aspectFit" />
				<input
					class="search-input"
					placeholder="搜索医院、患者或服务类型"
					v-model="searchKeyword"
					@confirm="handleSearch"
					confirm-type="search"
				/>
			</view>
			<view class="header-actions">
				<view class="action-btn" @click="showFilterPopup = true">
					<image class="action-icon" src="/static/filter.png" mode="aspectFit" />
					<text class="action-text">筛选</text>
				</view>
				<view class="action-btn" @click="refreshOrders" :class="{ spinning: isRefreshing }">
					<image class="action-icon" src="/static/refresh.svg" mode="aspectFit" />
					<text class="action-text">刷新</text>
				</view>
			</view>
		</view>

		<view v-if="showFilterPopup" class="filter-mask" @click="closeFilterPopup">
			<view class="filter-popup" @click.stop>
				<view class="filter-popup-title">筛选条件</view>
				<view class="filter-row" @click="toggleExpand('service')">
					<text class="filter-label">服务类型</text>
					<view class="filter-value-wrap">
						<text class="filter-value">{{ serviceTypeLabels[filterServiceTypeIndex] }}</text>
						<text class="filter-arrow">▼</text>
					</view>
				</view>
				<view v-if="expandWhich === 'service'" class="filter-options">
					<view
						v-for="(opt, idx) in serviceTypeOptions"
						:key="'s'+idx"
						class="filter-option"
						:class="{ active: filterServiceTypeIndex === idx }"
						@click.stop="selectServiceType(idx)"
					>
						{{ opt.label }}
					</view>
				</view>
				<view class="filter-row" @click="toggleExpand('duration')">
					<text class="filter-label">预计时长</text>
					<view class="filter-value-wrap">
						<text class="filter-value">{{ durationLabels[filterDurationIndex] }}</text>
						<text class="filter-arrow">▼</text>
					</view>
				</view>
				<view v-if="expandWhich === 'duration'" class="filter-options">
					<view
						v-for="(opt, idx) in durationOptions"
						:key="'d'+idx"
						class="filter-option"
						:class="{ active: filterDurationIndex === idx }"
						@click.stop="selectDuration(idx)"
					>
						{{ opt.label }}
					</view>
				</view>
				<view class="filter-row" @click="toggleExpand('fee')">
					<text class="filter-label">基础费用</text>
					<view class="filter-value-wrap">
						<text class="filter-value">{{ feeLabels[filterFeeIndex] }}</text>
						<text class="filter-arrow">▼</text>
					</view>
				</view>
				<view v-if="expandWhich === 'fee'" class="filter-options">
					<view
						v-for="(opt, idx) in feeOptions"
						:key="'f'+idx"
						class="filter-option"
						:class="{ active: filterFeeIndex === idx }"
						@click.stop="selectFee(idx)"
					>
						{{ opt.label }}
					</view>
				</view>
				<view class="filter-actions">
					<button class="filter-btn reset" @click="resetFilter">重置</button>
					<button class="filter-btn confirm" @click="confirmFilter">确定</button>
				</view>
			</view>
		</view>

		<scroll-view
			class="order-list"
			scroll-y="true"
			@scrolltolower="loadMore"
			refresher-enabled="true"
			:refresher-triggered="isRefreshing"
			@refresherrefresh="onRefresh"
		>
			<view v-if="!isLoading" class="content-wrapper">
				<view v-if="filteredOrderList.length > 0" class="order-container">
					<OrderCard
						v-for="(order, index) in filteredOrderList"
						:key="order.orderId"
						:order-data="formatOrderData(order)"
						:show-actions="true"
						:action-type="'accept'"
						@contact="contactPatient"
						@main-action="handleAccept"
						@view-detail="goToDetail"
					/>
				</view>
				<view v-else class="empty-state">
					<image class="empty-icon" src="/static/order.png" mode="aspectFit" />
					<text class="empty-text">暂无待接订单，稍后再来看看吧</text>
				</view>
			</view>

			<view class="loading-more" v-if="isLoading">
				<text>加载中...</text>
			</view>
		</scroll-view>

		<EscortBottomBar active="hall" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import OrderCard from '@/components/OrderCard.vue'
import EscortBottomBar from '@/components/EscortBottomBar.vue'
import { get, post, config } from '@/utils/api.js'
import { ensureRole } from '@/utils/auth-guard.js'

const statusBarHeight = ref(0)
const orderList = ref([])
const searchKeyword = ref('')
const isLoading = ref(false)
const isRefreshing = ref(false)
const page = ref(0)
const showFilterPopup = ref(false)

const serviceTypeOptions = [
	{ label: '全部', value: null },
	{ label: '普通陪诊', value: 1 },
	{ label: '术后护理', value: 2 },
	{ label: '急诊陪同', value: 3 },
	{ label: '上门陪诊', value: 4 }
]
const durationOptions = [
	{ label: '不限时长', value: null },
	{ label: '大于两小时', value: 2 },
	{ label: '大于三小时', value: 3 },
	{ label: '大于四小时', value: 4 }
]
const feeOptions = [
	{ label: '不限价格', value: null },
	{ label: '小于80元', value: 80 },
	{ label: '小于100元', value: 100 },
	{ label: '小于150元', value: 150 }
]
const serviceTypeLabels = serviceTypeOptions.map(o => o.label)
const durationLabels = durationOptions.map(o => o.label)
const feeLabels = feeOptions.map(o => o.label)

const filterServiceTypeIndex = ref(0)
const filterDurationIndex = ref(0)
const filterFeeIndex = ref(0)
const expandWhich = ref(null)

const filterParams = ref({
	serviceType: null,
	expectedDurationMinHours: null,
	orderAmountMax: null
})

const filteredOrderList = computed(() => {
	const kw = (searchKeyword.value || '').trim().toLowerCase()
	if (!kw) return orderList.value
	return orderList.value.filter((o) => {
		const hospital = (o.hospital || '').toLowerCase()
		const patient = (o.patientName || o.contactPerson || '').toLowerCase()
		const service = (o.serviceTypeName || o.serviceContent || '').toString().toLowerCase()
		const req = (o.specialRequirements || '').toLowerCase()
		const custom = (o.customRequirement || '').toLowerCase()
		return hospital.includes(kw) || patient.includes(kw) || service.includes(kw) || req.includes(kw) || custom.includes(kw)
	})
})

const handleSearch = () => {}

const formatOrderData = (raw) => {
	const symptomDescription = raw.specialRequirements || ''
	const otherRequirement = (raw.customRequirement && raw.customRequirement !== '无') ? raw.customRequirement : ''

	// 接单大厅：出于隐私保护，统一使用本地默认头像（与原陪诊师端一致）
	const placeholder = '/static/user-placeholder.png'

	return {
		id: raw.orderId,
		orderId: raw.orderId,
		userName: raw.patientName || raw.contactPerson || '匿名患者',
		userAge: raw.patientAge || '--',
		userGender: raw.patientSex || '未知',
		userAvatar: placeholder,
		serviceType: raw.serviceContent || raw.serviceTypeName || '陪诊服务',
		hospitalName: raw.hospital || '未知医院',
		appointmentTime: (raw.serviceDate || '') + ' ' + (raw.serviceTimeSlot || ''),
		price: ((raw.orderAmount || 0) * 0.9).toFixed(2),
		symptomDescription,
		otherRequirement,
		phone: raw.contactPhone || raw.userPhone,
		orderStatus: raw.orderStatus
	}
}

const loadOrders = async (reset = false) => {
	if (isLoading.value) return
	if (reset) {
		page.value = 0
		orderList.value = []
	}
	isLoading.value = true
	try {
		const params = { page: page.value, size: 10 }
		if (filterParams.value.serviceType != null) params.serviceType = filterParams.value.serviceType
		if (filterParams.value.expectedDurationMinHours != null) params.expectedDurationMinHours = filterParams.value.expectedDurationMinHours
		if (filterParams.value.orderAmountMax != null) params.orderAmountMax = filterParams.value.orderAmountMax
		const res = await get('/attendant/orders/waiting', params)
		if (res.code === 200 && res.data) {
			const newOrders = res.data.content || []
			if (reset) orderList.value = newOrders
			else orderList.value = [...orderList.value, ...newOrders]
			if (newOrders.length > 0) page.value++
		}
	} catch (e) {
		console.error('加载订单失败:', e)
	} finally {
		isLoading.value = false
		isRefreshing.value = false
	}
}

const onRefresh = () => {
	isRefreshing.value = true
	loadOrders(true)
}

const refreshOrders = () => onRefresh()
const loadMore = () => loadOrders()

// 查看详情：陪诊师端从大厅进入“陪诊师专用订单详情”
const goToDetail = (orderData) => {
	const id = orderData.orderId || orderData.id
	if (!id) {
		uni.showToast({ title: '订单信息有误', icon: 'none' })
		return
	}
	uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${id}` })
}

const handleAccept = (actionData) => {
	const order = actionData.data || actionData
	const attendantInfo = uni.getStorageSync('userInfo')
	if (!attendantInfo || !attendantInfo.id) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		return
	}
	uni.showModal({
		title: '确认接单',
		content: `确定要接受该订单吗？`,
		success: async (res) => {
			if (res.confirm) {
				try {
					const response = await post(`/attendant/orders/${order.id}/accept?attendantId=${attendantInfo.id}`)
					if (response.code === 200) {
						uni.showToast({ title: '接单成功', icon: 'success' })
						loadOrders(true)
					} else {
						uni.showToast({ title: response.message || '接单失败', icon: 'none' })
					}
				} catch (e) {
					uni.showToast({ title: e.message || '接单失败，请稍后重试', icon: 'none' })
				}
			}
		}
	})
}

const contactPatient = (order) => {
	if (order.phone) uni.makePhoneCall({ phoneNumber: order.phone })
	else uni.showToast({ title: '暂无联系电话', icon: 'none' })
}

const closeFilterPopup = () => {
	showFilterPopup.value = false
	expandWhich.value = null
}

const toggleExpand = (which) => {
	expandWhich.value = expandWhich.value === which ? null : which
}
const selectServiceType = (idx) => { filterServiceTypeIndex.value = idx; expandWhich.value = null }
const selectDuration = (idx) => { filterDurationIndex.value = idx; expandWhich.value = null }
const selectFee = (idx) => { filterFeeIndex.value = idx; expandWhich.value = null }
const resetFilter = () => { filterServiceTypeIndex.value = 0; filterDurationIndex.value = 0; filterFeeIndex.value = 0; expandWhich.value = null }

const confirmFilter = () => {
	filterParams.value = {
		serviceType: serviceTypeOptions[filterServiceTypeIndex.value].value,
		expectedDurationMinHours: durationOptions[filterDurationIndex.value].value,
		orderAmountMax: feeOptions[filterFeeIndex.value].value
	}
	showFilterPopup.value = false
	expandWhich.value = null
	loadOrders(true)
}

onMounted(() => {
	const sys = uni.getSystemInfoSync()
	statusBarHeight.value = sys.statusBarHeight || 0
	loadOrders(true)
})
onShow(() => ensureRole('escort'))
</script>

<style lang="scss" scoped>
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.page-wrap { min-height: 100vh; background-color: #f5f7fa; }
.header { padding: 20rpx 30rpx; background-color: #fff; display: flex; align-items: center; gap: 16rpx; }
.search-box { flex: 1; background-color: #f5f7fa; border-radius: 40rpx; padding: 16rpx 30rpx; display: flex; align-items: center; }
.search-icon { width: 32rpx; height: 32rpx; margin-right: 20rpx; opacity: 0.5; }
.search-input { flex: 1; font-size: 28rpx; color: #333; }
.header-actions { display: flex; align-items: center; gap: 8rpx; }
.action-btn { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 12rpx 20rpx; min-width: 80rpx; }
.action-icon { width: 36rpx; height: 36rpx; margin-bottom: 4rpx; }
.action-btn.spinning .action-icon { animation: spin 0.8s linear infinite; }
.action-text { font-size: 22rpx; color: #666; }
.filter-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.45); z-index: 1000; display: flex; align-items: flex-end; justify-content: center; }
.filter-popup { width: 100%; background: #fff; border-radius: 24rpx 24rpx 0 0; padding: 32rpx 40rpx calc(env(safe-area-inset-bottom) + 32rpx); box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.08); }
.filter-popup-title { font-size: 34rpx; font-weight: 600; color: #1a1a1a; margin-bottom: 32rpx; text-align: center; }
.filter-row { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.filter-row:last-of-type { border-bottom: none; }
.filter-label { font-size: 30rpx; color: #333; font-weight: 500; }
.filter-value-wrap { display: flex; align-items: center; gap: 12rpx; }
.filter-value { font-size: 28rpx; color: #4A90E2; font-weight: 500; }
.filter-arrow { font-size: 20rpx; color: #999; }
.filter-options { background: #f8faff; border-radius: 16rpx; margin: 0 0 20rpx 0; padding: 12rpx 0; max-height: 400rpx; overflow-y: auto; border: 1rpx solid #e8eeff; }
.filter-option { padding: 24rpx 36rpx; font-size: 28rpx; color: #444; border-radius: 12rpx; margin: 0 16rpx 8rpx; }
.filter-option.active { color: #4A90E2; font-weight: 600; background: rgba(74, 144, 226, 0.1); }
.filter-actions { display: flex; gap: 24rpx; margin-top: 40rpx; }
.filter-btn { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; font-size: 30rpx; font-weight: 500; border-radius: 44rpx; border: none; }
.filter-btn.reset { background: #f5f5f5; color: #666; }
.filter-btn.confirm { background: linear-gradient(135deg, #4A90E2, #357abd); color: #fff; box-shadow: 0 8rpx 24rpx rgba(74, 144, 226, 0.35); }
.content-wrapper { padding: 0 24rpx 40rpx; }
.order-container { padding-top: 20rpx; }
.order-list { flex: 1; min-height: 60vh; box-sizing: border-box; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding-top: 120rpx; }
.empty-icon { width: 160rpx; height: 160rpx; margin-bottom: 32rpx; opacity: 0.5; }
.empty-text { color: #999; font-size: 28rpx; }
.loading-more { text-align: center; padding: 40rpx; color: #999; font-size: 26rpx; }
</style>

