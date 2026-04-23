<template>
	<view class="page-wrap">
		<view class="header">
			<view class="header-module">
				<view class="top-row">
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
						<view class="action-btn" :class="{ active: showFilterPopup || hasActiveFilter }" @click="showFilterPopup = true">
							<image class="action-icon" src="/static/filter.png" mode="aspectFit" />
						</view>
						<view class="action-btn" @click="refreshOrders" :class="{ spinning: isRefreshVisual, refreshing: isRefreshVisual }">
							<image class="action-icon" src="/static/refresh.svg" mode="aspectFit" />
						</view>
					</view>
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
		>
			<view class="content-wrapper">
				<view v-if="filteredOrderList.length > 0" class="order-container">
					<order-card
						v-for="(order, index) in filteredOrderList"
						:key="`${order.orderId || order.id || 'order'}-${index}`"
						:order-data="formatOrderData(order)"
						:show-actions="true"
						:action-type="'accept'"
						@card-click="goToDetail"
						@view-detail="goToDetail"
						@main-action="handleAccept"
					/>
				</view>
				<view v-else-if="!isLoading" class="empty-state">
					<image class="empty-icon" src="/static/order.png" mode="aspectFit" />
					<text class="empty-text">暂无待接订单，稍后再来看看吧</text>
				</view>
				<view v-else class="loading-more initial">
					<text>加载中...</text>
				</view>
			</view>

			<view class="loading-more" v-if="isLoading && orderList.length > 0">
				<text>加载中...</text>
			</view>
		</scroll-view>

		<exclusive-dispatch-popup />
		<escort-bottom-bar active="hall" />
	</view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import ExclusiveDispatchPopup from '@/components/exclusive-dispatch-popup.vue'
import OrderCard from '@/components/order-card.vue'
import EscortBottomBar from '@/components/escort-bottom-bar.vue'
import { get, post } from '@/utils/api.js'
import { ensureRole } from '@/utils/auth-guard.js'
import { userPlaceholder } from '@/utils/assets.js'
import { redirectPublicSafeToHome } from '@/utils/site-mode.js'
import { formatServiceTimeSlot } from '@/utils/order-display.js'
import { addOrderListener, removeOrderListener, connectOrderSocket } from '@/utils/order-websocket.js'

const orderList = ref([])
const searchKeyword = ref('')
const isLoading = ref(false)
const isRefreshing = ref(false)
const isRefreshVisual = ref(false)
const page = ref(0)
const pageSize = 10
const hasMore = ref(true)
const showFilterPopup = ref(false)
const acceptingOrderId = ref('')

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
let localOrderUpdatedListener = null
let socketListener = null
let pageActive = false
let queuedSilentRefresh = false

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
const hasActiveFilter = computed(() =>
	filterServiceTypeIndex.value > 0 || filterDurationIndex.value > 0 || filterFeeIndex.value > 0
)
const handleSearch = () => {}

const formatOrderData = (raw) => {
	const symptomDescription = raw.specialRequirements || ''
	const otherRequirement = (raw.customRequirement && raw.customRequirement !== '无') ? raw.customRequirement : ''

	// 接单大厅：出于隐私保护，统一使用本地默认头像（与原陪诊师端一致）
	const placeholder = userPlaceholder

	return {
		id: raw.orderId,
		orderId: raw.orderId,
		userName: raw.patientName || raw.contactPerson || '匿名患者',
		userAge: raw.patientAge || '--',
		userGender: raw.patientSex || '未知',
		userAvatar: placeholder,
		serviceType: raw.serviceContent || raw.serviceTypeName || '陪诊服务',
		hospitalName: raw.hospital || '未知医院',
		appointmentTime: [raw.serviceDate, formatServiceTimeSlot(raw.serviceTimeSlot || '')].filter(Boolean).join(' ').trim(),
		price: ((raw.orderAmount || 0) * 0.9).toFixed(2),
		symptomDescription,
		otherRequirement,
		phone: raw.contactPhone || raw.userPhone,
		orderStatus: raw.orderStatus
	}
}

const loadOrders = async ({ reset = false, silent = false } = {}) => {
	if (isLoading.value) {
		if (reset && silent) queuedSilentRefresh = true
		return
	}
	if (!reset && !hasMore.value) return
	if (reset) {
		page.value = 0
		hasMore.value = true
		if (!silent) {
			orderList.value = []
		}
	}
	if (silent) isRefreshing.value = true
	else isLoading.value = true
	try {
		const params = { page: page.value, size: pageSize }
		if (filterParams.value.serviceType != null) params.serviceType = filterParams.value.serviceType
		if (filterParams.value.expectedDurationMinHours != null) params.expectedDurationMinHours = filterParams.value.expectedDurationMinHours
		if (filterParams.value.orderAmountMax != null) params.orderAmountMax = filterParams.value.orderAmountMax
		const res = await get('/attendant/orders/waiting', params)
		if (res.code === 200 && res.data) {
			const newOrders = res.data.content || []
			if (reset) {
				orderList.value = newOrders
			} else {
				const idSet = new Set(orderList.value.map(item => String(item.orderId || item.id || '')))
				const uniqueAppends = newOrders.filter(item => {
					const id = String(item.orderId || item.id || '')
					if (!id) return true
					if (idSet.has(id)) return false
					idSet.add(id)
					return true
				})
				orderList.value = [...orderList.value, ...uniqueAppends]
				if (newOrders.length > 0 && uniqueAppends.length === 0) {
					hasMore.value = false
				}
			}

			const backendPage = Number(res.data.number)
			if (Number.isFinite(backendPage)) page.value = backendPage + 1
			else if (newOrders.length > 0) page.value++

			if (res.data.last === true) {
				hasMore.value = false
			} else if (newOrders.length < pageSize) {
				hasMore.value = false
			}
		}
	} catch (e) {
		console.error('加载订单失败:', e)
	} finally {
		isLoading.value = false
		isRefreshing.value = false
		if (queuedSilentRefresh && pageActive) {
			queuedSilentRefresh = false
			loadOrders({ reset: true, silent: true })
		}
	}
}

const beginRefreshVisual = () => {
	isRefreshVisual.value = true
}

const endRefreshVisual = (startTs) => {
	const elapsed = Date.now() - startTs
	const remain = Math.max(0, 700 - elapsed)
	setTimeout(() => {
		isRefreshVisual.value = false
	}, remain)
}

const onRefresh = async () => {
	if (isLoading.value || isRefreshing.value) return
	const startTs = Date.now()
	beginRefreshVisual()
	isRefreshing.value = true
	await loadOrders({ reset: true })
	endRefreshVisual(startTs)
}

const refreshOrders = () => {
	if (isLoading.value || isRefreshing.value) return
	onRefresh()
}
const loadMore = () => {
	if (isRefreshing.value || isLoading.value || !hasMore.value) return
	loadOrders({})
}

const wait = (ms = 300) => new Promise((resolve) => setTimeout(resolve, ms))

const openEscortOrderDetail = async (orderId, attempt = 0) => {
	const targetOrderId = Number(orderId || 0)
	if (!targetOrderId) {
		throw new Error('订单信息有误')
	}
	try {
		await get(`/attendant/orders/${targetOrderId}`)
		uni.navigateTo({ url: `/subpkg/order/escort-detail?orderId=${targetOrderId}` })
	} catch (error) {
		if (attempt < 1) {
			await wait(350)
			return openEscortOrderDetail(targetOrderId, attempt + 1)
		}
		throw error
	}
}

// 查看详情：陪诊师端从大厅进入“陪诊师专用订单详情”
const goToDetail = async (orderData) => {
	const id = orderData.orderId || orderData.id
	if (!id) {
		uni.showToast({ title: '订单信息有误', icon: 'none' })
		return
	}
	try {
		await openEscortOrderDetail(id)
	} catch (error) {
		uni.showToast({ title: error?.message || '订单详情暂时无法打开', icon: 'none' })
	}
}

const handleAccept = (actionData) => {
	const order = actionData.data || actionData
	const currentOrderId = order.orderId || order.id
	if (!currentOrderId) {
		uni.showToast({ title: '订单信息有误', icon: 'none' })
		return
	}
	const attendantInfo = uni.getStorageSync('userInfo')
	if (!attendantInfo || !attendantInfo.id) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		return
	}
	uni.showModal({
		title: '确认接单',
		content: '确定要接受该订单吗？',
		success: async (res) => {
			if (!res.confirm || acceptingOrderId.value) return
			acceptingOrderId.value = String(currentOrderId)
			uni.showLoading({ title: '接单中...', mask: true })
			try {
				const response = await post(`/attendant/orders/${currentOrderId}/accept?attendantId=${attendantInfo.id}`)
				if (response.code === 200) {
					uni.showToast({ title: '接单成功', icon: 'success' })
					loadOrders({ reset: true, silent: true })
					const acceptedOrderId = response?.data?.orderId || currentOrderId
					acceptingOrderId.value = ''
					uni.hideLoading()
					setTimeout(async () => {
						try {
							await openEscortOrderDetail(acceptedOrderId)
						} catch (error) {
							console.error('接单后跳转详情失败:', error)
							uni.showToast({ title: error?.message || '接单成功，请到我的订单查看', icon: 'none' })
							uni.switchTab({ url: '/pages/role-escort/order' })
						}
					}, 300)
				} else {
					acceptingOrderId.value = ''
					uni.hideLoading()
					uni.showToast({ title: response.message || '接单失败', icon: 'none' })
				}
			} catch (e) {
				acceptingOrderId.value = ''
				uni.hideLoading()
				uni.showToast({ title: e.message || '接单失败，请稍后重试', icon: 'none' })
			}
		}
	})
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
	loadOrders({ reset: true })
}

const handleOrderSocketMessage = (message) => {
	if (!pageActive || !message) return
	if (String(message.type || '').toUpperCase() !== 'WAITING_ORDER_UPDATED') return
	setTimeout(() => {
		loadOrders({ reset: true, silent: true })
	}, 600)
}

onMounted(() => {
	setupOrderSocketListener()
	loadOrders({ reset: true })
	localOrderUpdatedListener = (payload) => {
		if (payload && payload.action === 'released') {
			loadOrders({ reset: true, silent: true })
		}
	}
	uni.$on('escort-order-updated', localOrderUpdatedListener)
})
onShow(() => {
	if (redirectPublicSafeToHome()) return
	pageActive = true
	ensureRole('escort')
	connectOrderSocket()
	if (orderList.value.length > 0) {
		loadOrders({ reset: true, silent: true })
	}
})
onHide(() => {
	pageActive = false
})
onUnmounted(() => {
	if (localOrderUpdatedListener) {
		uni.$off('escort-order-updated', localOrderUpdatedListener)
	}
	if (socketListener) {
		removeOrderListener(socketListener)
		socketListener = null
	}
})

const setupOrderSocketListener = () => {
	if (socketListener) {
		removeOrderListener(socketListener)
	}
	socketListener = handleOrderSocketMessage
	addOrderListener(socketListener)
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.page-wrap {
  @include escort-page;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  padding: 18rpx 24rpx 14rpx;
  background: #f5f7fa;
  flex-shrink: 0;
  z-index: 20;
}

.header-module {
  background: $escort-color-surface;
  border-radius: $escort-radius-card;
  padding: 16rpx 14rpx;
  box-shadow: $escort-shadow-card;
}

.top-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.search-box {
  flex: 1;
  background: #f5f7fa;
  border: 1rpx solid #e6edf5;
  border-radius: 40rpx;
  padding: 14rpx 22rpx;
  display: flex;
  align-items: center;
  min-height: 72rpx;
  box-sizing: border-box;
}

.search-icon {
  width: 30rpx;
  height: 30rpx;
  margin-right: 14rpx;
  opacity: 0.55;
}

.search-input {
  flex: 1;
  font-size: 27rpx;
  color: #1f2937;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.action-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  background: #ffffff;
  border: 1rpx solid #e0e8f2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-icon {
  width: 28rpx;
  height: 28rpx;
  margin-right: 0;
}

.action-btn.active,
.action-btn.refreshing {
  background: #eaf3ff;
  border-color: $escort-color-primary;
  box-shadow: $escort-shadow-primary;
}

.action-btn.spinning .action-icon {
  animation: spin 0.8s linear infinite;
}

.filter-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.filter-popup {
  width: 100%;
  background: #f5f7fa;
  border-radius: 24rpx 24rpx 0 0;
  padding: 32rpx 32rpx calc(env(safe-area-inset-bottom) + 30rpx);
  box-shadow: 0 -10rpx 36rpx rgba(31, 41, 55, 0.14);
}

.filter-popup-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 22rpx;
  text-align: center;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #e4ebf3;
}

.filter-row:last-of-type {
  border-bottom: none;
}

.filter-label {
  font-size: 29rpx;
  color: #1f2937;
  font-weight: 600;
}

.filter-value-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.filter-value {
  font-size: 26rpx;
  color: #007AFF;
}

.filter-arrow {
  font-size: 20rpx;
  color: #8a94a6;
}

.filter-options {
  background: #edf2f9;
  border: 1rpx solid #dce5f2;
  border-radius: 14rpx;
  margin: 14rpx 0 20rpx;
  padding: 8rpx 0;
  max-height: 360rpx;
  overflow-y: auto;
}

.filter-option {
  padding: 22rpx 26rpx;
  font-size: 27rpx;
  color: #445062;
  border-radius: 12rpx;
  margin: 0 12rpx 8rpx;
}

.filter-option.active {
  color: #007AFF;
  font-weight: 600;
  background: rgba(102, 166, 255, 0.14);
}

.filter-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 30rpx;
}

.filter-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  font-size: 30rpx;
  border-radius: 42rpx;
  border: none;
}

.filter-btn.reset {
  background: #e7edf5;
  color: #4b5565;
}

.filter-btn.confirm {
  background: linear-gradient(135deg, $escort-color-primary, $escort-color-primary-deep);
  color: #fff;
  box-shadow: $escort-shadow-primary;
}

.content-wrapper {
  padding: 14rpx 24rpx calc(164rpx + env(safe-area-inset-bottom));
}

.order-container {
  padding-top: 0;
}

.order-list {
  flex: 1;
  height: 0;
  min-height: 0;
  box-sizing: border-box;
  overflow: hidden;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 120rpx;
}

.empty-icon {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 24rpx;
  opacity: 0.55;
}

.empty-text {
  color: #7d8898;
  font-size: 28rpx;
}

.loading-more {
  text-align: center;
  padding: 36rpx;
  color: #7d8898;
  font-size: 25rpx;
}
</style>
