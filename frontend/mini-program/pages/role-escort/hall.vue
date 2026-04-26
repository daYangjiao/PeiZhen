<template>
	<view class="page-wrap">
		<view v-if="!qualificationGate.checked || qualificationGate.allowed" class="header">
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

		<view v-if="showFilterPopup && qualificationGate.allowed" class="filter-mask" @click="closeFilterPopup">
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

		<view v-if="qualificationGate.checked && !qualificationGate.allowed" class="qualification-block">
			<view class="block-card" :class="`state-${qualificationGate.state}`">
				<view class="block-icon">{{ qualificationGate.state === 'pending' ? '审' : '!' }}</view>
				<text class="block-title">{{ qualificationGate.title }}</text>
				<text class="block-desc">{{ qualificationGate.message }}</text>
				<view class="block-actions">
					<button v-if="qualificationGate.state !== 'pending' && qualificationGate.state !== 'blocked'" class="block-btn primary" @click="goQualification">去资质管理</button>
					<button class="block-btn" @click="checkHallGate({ showPopup: false })">刷新状态</button>
				</view>
			</view>
		</view>

		<scroll-view
			v-else
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
			<view v-if="acceptConfirm.visible" class="accept-modal-mask" @click="closeAcceptConfirm">
				<view class="accept-modal" @click.stop>
					<view class="accept-mark">接</view>
					<view class="accept-title">确认接单</view>
					<view class="accept-desc">接单后订单会进入你的服务列表，请确认服务时间和医院信息后再继续。</view>
					<view class="accept-summary">
						<view class="accept-summary-row">
							<text>服务医院</text>
							<text>{{ acceptConfirm.order?.hospital || '-' }}</text>
						</view>
						<view class="accept-summary-row">
							<text>服务时间</text>
							<text>{{ acceptConfirm.order?.displayServiceTime || formatServiceTimeSlot(acceptConfirm.order || {}) }}</text>
						</view>
					</view>
					<view class="accept-actions">
						<button class="accept-btn secondary" @click="closeAcceptConfirm">取消</button>
						<button class="accept-btn primary" :disabled="Boolean(acceptingOrderId)" @click="confirmAcceptOrder">确认接单</button>
					</view>
				</view>
			</view>
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
import { guardEscortHallAccess } from '@/utils/escort-qualification-guard.js'
import { calculateEstimatedAttendantIncome } from '@/utils/settlement.mjs'
import { useExclusiveDispatchStore } from '@/stores/exclusive-dispatch.js'

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
const acceptConfirm = ref({
	visible: false,
	order: null
})
const qualificationGate = ref({
	checked: false,
	allowed: false,
	state: 'pending',
	title: '资质审核中',
	message: '平台正在审核你的入驻资料，审核通过后即可查看接单大厅。'
})

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
		price: calculateEstimatedAttendantIncome(raw).toFixed(2),
		symptomDescription,
		otherRequirement,
		phone: raw.contactPhone || raw.userPhone,
		orderStatus: raw.orderStatus
	}
}

const loadOrders = async ({ reset = false, silent = false } = {}) => {
	if (!qualificationGate.value.allowed) return
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
	if (!qualificationGate.value.allowed) {
		checkHallGate({ showPopup: qualificationGate.value.state !== 'pending' })
		return
	}
	if (isLoading.value || isRefreshing.value) return
	onRefresh()
}
const loadMore = () => {
	if (!qualificationGate.value.allowed) return
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
	if (!qualificationGate.value.allowed) {
		checkHallGate({ showPopup: qualificationGate.value.state !== 'pending' })
		return
	}
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
	acceptConfirm.value = {
		visible: true,
		order: {
			...order,
			orderId: currentOrderId,
			attendantId: attendantInfo.id
		}
	}
}

const closeAcceptConfirm = () => {
	if (acceptingOrderId.value) return
	acceptConfirm.value = { visible: false, order: null }
}

const confirmAcceptOrder = async () => {
	const order = acceptConfirm.value.order || {}
	const currentOrderId = order.orderId || order.id
	const attendantId = order.attendantId
	if (!currentOrderId || !attendantId || acceptingOrderId.value) return
	acceptingOrderId.value = String(currentOrderId)
	uni.showLoading({ title: '接单中...', mask: true })
	try {
		const response = await post(`/attendant/orders/${currentOrderId}/accept?attendantId=${attendantId}`)
		if (response.code === 200) {
			acceptConfirm.value = { visible: false, order: null }
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

const goQualification = () => {
	uni.navigateTo({ url: '/subpkg/profile/qualification' })
}

const checkHallGate = async ({ showPopup = true } = {}) => {
	const gate = await guardEscortHallAccess({ showPopup, redirectOnConfirm: true })
	qualificationGate.value = { checked: true, ...gate }
	if (!gate.allowed) {
		orderList.value = []
		hasMore.value = false
		isLoading.value = false
		isRefreshing.value = false
		return gate
	}
	hasMore.value = true
	return gate
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
	if (!pageActive || !message || !qualificationGate.value.allowed) return
	if (String(message.type || '').toUpperCase() !== 'WAITING_ORDER_UPDATED') return
	setTimeout(() => {
		loadOrders({ reset: true, silent: true })
	}, 600)
}

onMounted(() => {
	setupOrderSocketListener()
	localOrderUpdatedListener = (payload) => {
		if (payload && payload.action === 'released' && qualificationGate.value.allowed) {
			loadOrders({ reset: true, silent: true })
		}
	}
	uni.$on('escort-order-updated', localOrderUpdatedListener)
})
onShow(() => {
	if (redirectPublicSafeToHome()) return
	pageActive = true
	ensureRole('escort')
	const dispatchStore = useExclusiveDispatchStore()
	dispatchStore.ensureInitialized()
	dispatchStore.refreshPendingExclusiveOrders()
	checkHallGate({ showPopup: true }).then((gate) => {
		if (!gate.allowed) return
		connectOrderSocket()
		loadOrders({ reset: true, silent: orderList.value.length > 0 })
	})
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

.qualification-block {
  flex: 1;
  padding: 120rpx 30rpx 180rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.block-card {
  width: 100%;
  background: #ffffff;
  border: 1rpx solid #e5eefb;
  border-radius: 34rpx;
  padding: 52rpx 34rpx;
  box-shadow: 0 24rpx 60rpx rgba(46, 107, 184, 0.12);
  text-align: center;
  box-sizing: border-box;
}

.block-icon {
  width: 88rpx;
  height: 88rpx;
  margin: 0 auto 22rpx;
  border-radius: 32rpx;
  background: #eef6ff;
  color: $escort-color-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
  font-weight: 800;
}

.state-rejected .block-icon,
.state-expired .block-icon,
.state-incomplete .block-icon {
  background: #fff2f0;
  color: #ef4444;
}

.state-blocked .block-icon {
  background: #f3f4f6;
  color: #4b5563;
}

.block-title {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #162033;
}

.block-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #667085;
}

.block-actions {
  margin-top: 34rpx;
  display: flex;
  justify-content: center;
  gap: 18rpx;
}

.block-btn {
  height: 76rpx;
  padding: 0 30rpx;
  border-radius: 999rpx;
  background: #f3f7fb;
  color: #36506d;
  font-size: 26rpx;
}

.block-btn.primary {
  background: $escort-color-primary;
  color: #ffffff;
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

	.accept-modal-mask {
	  position: fixed;
	  inset: 0;
	  z-index: 2100;
	  background: rgba(17, 24, 39, 0.42);
	  display: flex;
	  align-items: center;
	  justify-content: center;
	  padding: 48rpx;
	  box-sizing: border-box;
	}

	.accept-modal {
	  width: 100%;
	  max-width: 630rpx;
	  padding: 46rpx 34rpx 32rpx;
	  border-radius: 40rpx;
	  background: #ffffff;
	  box-shadow: 0 30rpx 90rpx rgba(25, 66, 128, 0.22);
	  box-sizing: border-box;
	  animation: acceptModalIn 180ms ease-out;
	}

	.accept-mark {
	  width: 92rpx;
	  height: 92rpx;
	  margin: 0 auto 24rpx;
	  border-radius: 32rpx;
	  background: #eef6ff;
	  color: $escort-color-primary;
	  display: flex;
	  align-items: center;
	  justify-content: center;
	  font-size: 38rpx;
	  font-weight: 800;
	}

	.accept-title {
	  text-align: center;
	  font-size: 36rpx;
	  line-height: 1.3;
	  color: #172033;
	  font-weight: 800;
	}

	.accept-desc {
	  margin-top: 14rpx;
	  color: #667085;
	  font-size: 26rpx;
	  line-height: 1.6;
	  text-align: center;
	}

	.accept-summary {
	  margin-top: 24rpx;
	  padding: 20rpx 22rpx;
	  border-radius: 26rpx;
	  background: #f7faff;
	}

	.accept-summary-row {
	  display: flex;
	  align-items: flex-start;
	  justify-content: space-between;
	  gap: 18rpx;
	  padding: 10rpx 0;
	  color: #53627a;
	  font-size: 26rpx;
	}

	.accept-summary-row text:last-child {
	  flex: 1;
	  text-align: right;
	  color: #172033;
	  font-weight: 700;
	}

	.accept-actions {
	  margin-top: 30rpx;
	  display: grid;
	  grid-template-columns: 1fr 1.25fr;
	  gap: 18rpx;
	}

	.accept-btn {
	  height: 84rpx;
	  line-height: 84rpx;
	  border: none;
	  border-radius: 999rpx;
	  font-size: 28rpx;
	  font-weight: 700;
	}

	.accept-btn.secondary {
	  background: #f2f6fb;
	  color: #53627a;
	}

	.accept-btn.primary {
	  background: linear-gradient(135deg, #1777ff, #0f9ed8);
	  color: #ffffff;
	  box-shadow: 0 14rpx 30rpx rgba(23, 119, 255, 0.22);
	}

	.accept-btn[disabled] {
	  opacity: 0.65;
	}

	@keyframes acceptModalIn {
	  from {
	    opacity: 0;
	    transform: translateY(28rpx) scale(0.96);
	  }
	  to {
	    opacity: 1;
	    transform: translateY(0) scale(1);
	  }
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
