<template>
	<view class="container">
		<view v-if="publicSafeMode" class="public-safe-profile">
			<view class="safe-hero">
				<text class="safe-title">服务说明</text>
				<text class="safe-desc">当前网站提供陪诊服务介绍、就医流程参考、健康管理建议与联系方式说明。</text>
			</view>

			<view class="safe-group">
				<text class="group-title">服务介绍</text>
				<view class="safe-item"><text class="safe-item-title">普通陪诊</text><text class="safe-item-desc">就医流程陪同、挂号缴费、取药取号等参考说明。</text></view>
				<view class="safe-item"><text class="safe-item-title">术后护理</text><text class="safe-item-desc">术后恢复阶段的陪护建议与康复注意事项展示。</text></view>
				<view class="safe-item"><text class="safe-item-title">急诊陪同</text><text class="safe-item-desc">紧急就医流程、急诊陪同注意事项与准备建议。</text></view>
				<view class="safe-item"><text class="safe-item-title">上门陪诊</text><text class="safe-item-desc">上门陪同场景说明、服务边界与准备材料参考。</text></view>
			</view>

			<view class="safe-group">
				<text class="group-title">说明与联系</text>
				<view class="safe-item"><text class="safe-item-title">使用说明</text><text class="safe-item-desc">当前网站以服务介绍、流程说明和信息展示为主。</text></view>
				<view class="safe-item"><text class="safe-item-title">隐私与协议</text><text class="safe-item-desc">请在正式使用前阅读服务协议与隐私说明，AI内容仅作健康科普参考。</text></view>
			</view>
		</view>

		<template v-else>
		<view class="user-section">
			<view v-if="userStore.isLoggedIn" class="user-info">
				<view class="user-avatar">
					<image 
						class="avatar-img" 
						:src="avatarDisplayUrl" 
						mode="aspectFill"
						@error="onAvatarError"
					></image>
				</view>
				<view class="user-details">
					<text class="user-name">{{ userStore.displayName }}</text>
					<text class="user-desc">{{ userStore.userInfo && userStore.userInfo.gender === 1 ? '先生' : userStore.userInfo && userStore.userInfo.gender === 2 ? '女士' : '' }}</text>
				</view>
				<view class="user-actions">
					<view class="action-btn" @click="handleEditProfile">
						<text class="action-text">编辑</text>
					</view>
				</view>
			</view>
			
			<view v-else class="login-prompt">
				<view class="prompt-icon">
					<image class="icon-img" src="/static/user-placeholder.png" mode="aspectFit"></image>
				</view>
				<text class="prompt-title">您还未登录</text>
				<text class="prompt-desc">登录后可享受更多个性化服务</text>
				<button class="login-btn" @click="goToLogin">
					<text class="btn-text">立即登录</text>
				</button>
			</view>
		</view>
		
		<view class="menu-section">
			<view class="menu-group">
				<text class="group-title">我的服务</text>
				<view class="menu-list">
					<view class="menu-item" @click="goToOrders">
						<view class="item-icon">
							<image src="/static/order.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">我的订单</text>
						<text class="item-arrow">〉</text>
					</view>
					<view class="menu-item" @click="goToAppointments">
						<view class="item-icon">
							<image src="/static/yvyue_2.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">我的预约</text>
						<text class="item-arrow">〉</text>
					</view>
					<view class="menu-item" @click="goToMessages">
						<view class="item-icon">
							<image src="/static/xiaoxi_2.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">消息中心</text>
						<text class="item-arrow">〉</text>
					</view>
				</view>
			</view>
			
			<view class="menu-group">
				<text class="group-title">设置</text>
				<view class="menu-list">
					<view class="menu-item" @click="goToSettings">
						<view class="item-icon">
							<image src="/static/settings.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">设置</text>
						<text class="item-arrow">〉</text>
					</view>
					<view class="menu-item" @click="goToHelp">
						<view class="item-icon">
							<image src="/static/help.png" mode="aspectFit"></image>
						</view>
						<text class="item-title">帮助与反馈</text>
						<text class="item-arrow">〉</text>
					</view>
					<view v-if="userStore.isLoggedIn" class="menu-item" @click="handleLogout">
						<view class="item-icon">
							<image src="/static/wode_2.png" mode="aspectFit"></image>
						</view>
						<text class="item-title logout-text">退出登录</text>
						<text class="item-arrow">〉</text>
					</view>
				</view>
			</view>
		</view>
		</template>
	</view>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { config } from '@/utils/api.js'
import { getUserById } from '@/api/user.js'
import { ensureRole } from '@/utils/auth-guard.js'
import { userPlaceholder } from '@/utils/assets.js'
import { PUBLIC_SAFE_LANDING_URL, isPublicSafeMode } from '@/utils/site-mode.js'

const PLACEHOLDER_AVATAR = userPlaceholder
const userStore = useUserStore()
const avatarLoadFailed = ref(false)
const publicSafeMode = isPublicSafeMode()

const getFullAvatarUrl = (relativePath) => {
	if (!relativePath || typeof relativePath !== 'string') return PLACEHOLDER_AVATAR
	const path = String(relativePath).trim()
	if (!path) return PLACEHOLDER_AVATAR
	if (path.startsWith('http')) return path
	const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : config.baseURL + '/'
	const normalized = path.startsWith('/') ? path.substring(1) : path
	return baseUrl + normalized
}

const avatarDisplayUrl = computed(() => {
	if (avatarLoadFailed.value) return PLACEHOLDER_AVATAR
	const avatar = userStore.avatar || userStore.userInfo?.avatar || ''
	return getFullAvatarUrl(avatar)
})

const onAvatarError = () => {
	avatarLoadFailed.value = true
}

watch(() => userStore.avatar, () => {
	avatarLoadFailed.value = false
})

const goToLogin = () => {
	uni.navigateTo({ url: '/pages/auth/login?role=user' })
}

const handleEditProfile = () => {
	if (!userStore.checkLoginStatus('/pages/role-user/profile')) return
	uni.navigateTo({ url: '/subpkg/profile/edit-profile' })
}

const goToOrders = () => {
	if (!userStore.checkLoginStatus('/pages/role-user/order')) return
	uni.switchTab({
		url: '/pages/role-user/order'
	})
}

const goToAppointments = () => {
	uni.showToast({ title: '预约列表待接入', icon: 'none' })
}

const goToMessages = () => {
	if (!userStore.checkLoginStatus('/pages/role-user/message')) return
	uni.switchTab({
		url: '/pages/role-user/message'
	})
}

const goToSettings = () => {
	uni.showToast({
		title: '功能开发中',
		icon: 'none'
	})
}

const goToHelp = () => {
	uni.showToast({
		title: '功能开发中',
		icon: 'none'
	})
}

const handleLogout = () => {
	uni.showModal({
		title: '提示',
		content: '确定要退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				userStore.logout()
			}
		}
	})
}

const fetchUserDetail = async () => {
	const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId
	if (!userStore.isLoggedIn || !uid) return
	try {
		const userDetailResponse = await getUserById(uid)
		if (userDetailResponse.data) {
			const updatedUserInfo = {
				...userStore.userInfo,
				name: userDetailResponse.data.name,
				phone: userDetailResponse.data.phone,
				avatar: userDetailResponse.data.avatar
			}
			userStore.setUserInfo(updatedUserInfo)
		}
	} catch (error) {
		console.error('获取用户详细信息失败:', error)
	}
}

onMounted(() => {
	userStore.restoreFromStorage()
	fetchUserDetail()
})

onShow(() => {
	if (publicSafeMode) {
		uni.reLaunch({ url: PUBLIC_SAFE_LANDING_URL })
		return
	}
	const fromRoute = uni.getStorageSync('guard_from_route')
	if (!userStore.isLoggedIn && (fromRoute === 'pages/role-user/profile' || fromRoute === '/pages/role-user/profile')) {
		uni.removeStorageSync('guard_from_route')
		uni.switchTab({ url: '/pages/role-user/home' })
		return
	}
	if (!ensureRole('user')) return
	userStore.restoreFromStorage()
	avatarLoadFailed.value = false
	fetchUserDetail()
})
</script>

<style lang="scss" scoped>
@import '@/styles/user-ui.scss';
.container {
	min-height: 100vh;
	background-color: #f5f7fa;
	padding: 20rpx;
}
.public-safe-profile {
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}
.safe-hero {
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
	border-radius: 20rpx;
	padding: 32rpx 28rpx;
	color: #fff;
}
.safe-title {
	display: block;
	font-size: 36rpx;
	font-weight: 700;
	margin-bottom: 12rpx;
}
.safe-desc {
	display: block;
	font-size: 24rpx;
	line-height: 1.7;
	color: rgba(255, 255, 255, 0.92);
}
.safe-group {
	background: #fff;
	border-radius: 20rpx;
	padding: 24rpx;
}
.safe-item {
	padding: 20rpx 0;
	border-bottom: 1rpx solid #edf2f7;
}
.safe-item:last-child {
	border-bottom: 0;
	padding-bottom: 0;
}
.safe-item-title {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: #16324f;
	margin-bottom: 8rpx;
}
.safe-item-desc {
	display: block;
	font-size: 24rpx;
	line-height: 1.7;
	color: #6a7f94;
}
.user-section {
	background: linear-gradient(135deg, #007AFF 0%, #2563EB 100%);
	border-radius: 20rpx;
	padding: 40rpx;
	margin-bottom: 30rpx;
	color: white;
}
.user-info {
	display: flex;
	align-items: center;
}
.user-avatar {
	margin-right: 30rpx;
}
.avatar-img {
	width: 120rpx;
	height: 120rpx;
	border-radius: 60rpx;
	border: 4rpx solid rgba(255, 255, 255, 0.3);
}
.user-details {
	flex: 1;
}
.user-name {
	font-size: 36rpx;
	font-weight: bold;
	color: white;
	display: block;
	margin-bottom: 10rpx;
}
.user-desc {
	font-size: 24rpx;
	color: rgba(255, 255, 255, 0.8);
	display: block;
}
.user-actions {
	margin-left: 20rpx;
}
.action-btn {
	background: rgba(255, 255, 255, 0.2);
	border-radius: 30rpx;
	padding: 15rpx 30rpx;
	border: 2rpx solid rgba(255, 255, 255, 0.3);
}
.action-text {
	color: white;
	font-size: 24rpx;
}
.login-prompt {
	text-align: center;
	padding: 40rpx 0;
}
.prompt-icon {
	margin-bottom: 30rpx;
}
.icon-img {
	width: 120rpx;
	height: 120rpx;
	opacity: 0.8;
}
.prompt-title {
	font-size: 32rpx;
	font-weight: bold;
	color: white;
	display: block;
	margin-bottom: 15rpx;
}
.prompt-desc {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.8);
	display: block;
	margin-bottom: 40rpx;
}
.login-btn {
	background: white;
	border-radius: 50rpx;
	padding: 20rpx 60rpx;
	border: none;
	margin: 0;
}
.login-btn::after {
	border: none;
}
.btn-text {
	color: #007AFF;
	font-size: 28rpx;
	font-weight: bold;
}
.menu-section {
	background: white;
	border-radius: 20rpx;
	padding: 30rpx;
}
.menu-group {
	margin-bottom: 40rpx;
}
.menu-group:last-child {
	margin-bottom: 0;
}
.group-title {
	font-size: 28rpx;
	font-weight: bold;
	color: #333;
	display: block;
	margin-bottom: 20rpx;
	padding-left: 10rpx;
}
.menu-list {
	background: #f8f9fa;
	border-radius: 15rpx;
	overflow: hidden;
}
.menu-item {
	display: flex;
	align-items: center;
	padding: 30rpx 20rpx;
	background: white;
	margin-bottom: 2rpx;
	position: relative;
}
.menu-item:last-child {
	margin-bottom: 0;
}
.item-icon {
	width: 50rpx;
	height: 50rpx;
	margin-right: 30rpx;
}
.item-icon image {
	width: 100%;
	height: 100%;
}
.item-title {
	flex: 1;
	font-size: 28rpx;
	color: #333;
}
.logout-text {
	color: #ff4757;
}
.item-arrow {
	font-size: 24rpx;
	color: #999;
	font-weight: bold;
}
</style>
