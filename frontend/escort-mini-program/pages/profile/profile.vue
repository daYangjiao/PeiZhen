<template>
	<view class="container">
		<!-- 用户信息卡片 -->
		<view class="user-card">
			<view class="user-info">
				<image class="avatar" :src="getFullAvatarUrl(attendantInfo.avatarUrl || attendantInfo.avatar)" mode="aspectFill"></image>
				<view class="user-details">
					<text class="name">{{ attendantInfo.name || attendantInfo.username || '陪诊师' }}</text>
					<text class="phone">{{ attendantInfo.phone || '暂无电话' }}</text>
					<view class="status">
						<text class="status-text online">在线接单</text>
					</view>
				</view>
				<view class="edit-btn" @click="editProfile">
					<text>编辑</text>
				</view>
			</view>
			
			<!-- 统计信息 -->
			<view class="stats">
				<view class="stat-item">
					<text class="stat-number">{{ attendantInfo.totalOrders || 0 }}</text>
					<text class="stat-label">总订单</text>
				</view>
				<view class="stat-item">
					<text class="stat-number">{{ attendantInfo.completedOrders || 0 }}</text>
					<text class="stat-label">已完成</text>
				</view>
				<view class="stat-item">
					<text class="stat-number">{{ attendantInfo.score || '5.0' }}</text>
					<text class="stat-label">评分</text>
				</view>
				<view class="stat-item">
					<text class="stat-number">¥{{ attendantInfo.totalEarnings || '0.00' }}</text>
					<text class="stat-label">总收入</text>
				</view>
			</view>
		</view>
		
		<!-- 功能菜单 -->
		<view class="menu-section">
			<view class="menu-group">
				<view class="menu-item" @click="navigateTo('/subpkg/profile/certification')">
					<image class="menu-icon" src="/static/ren_1.png" mode="aspectFit"></image>
					<text class="menu-text">实名认证</text>
					<view class="menu-status verified" v-if="attendantInfo.certificate">
						<text>已认证</text>
					</view>
					<text class="menu-arrow">›</text>
				</view>
				
				<view class="menu-item" @click="navigateTo('/subpkg/profile/wallet')">
					<image class="menu-icon" src="/static/wujiaoxin.png" mode="aspectFit"></image>
					<text class="menu-text">我的钱包</text>
					<view class="menu-balance">
						<text>¥{{ attendantInfo.balance || '0.00' }}</text>
					</view>
					<text class="menu-arrow">›</text>
				</view>
				
				<view class="menu-item" @click="navigateTo('/subpkg/profile/schedule')">
					<image class="menu-icon" src="/static/time.png" mode="aspectFit"></image>
					<text class="menu-text">工作时间</text>
					<text class="menu-arrow">›</text>
				</view>
			</view>
			
			<view class="menu-group">
				<view class="menu-item" @click="navigateTo('/subpkg/profile/reviews')">
					<image class="menu-icon" src="/static/xin.png" mode="aspectFit"></image>
					<text class="menu-text">用户评价</text>
					<text class="menu-arrow">›</text>
				</view>
				
				<view class="menu-item" @click="navigateTo('/subpkg/profile/help')">
					<image class="menu-icon" src="/static/help.png" mode="aspectFit"></image>
					<text class="menu-text">帮助中心</text>
					<text class="menu-arrow">›</text>
				</view>
				
				<view class="menu-item" @click="navigateTo('/subpkg/profile/settings')">
					<image class="menu-icon" src="/static/settings.png" mode="aspectFit"></image>
					<text class="menu-text">设置</text>
					<text class="menu-arrow">›</text>
				</view>
			</view>
		</view>
		
		<!-- 退出登录 -->
		<view class="logout-section">
			<button class="logout-btn" @click="logout">退出登录</button>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get, clearToken, config } from '@/utils/api.js'

const attendantInfo = ref({})

onMounted(() => {
	loadAttendantInfo()
})

const getFullAvatarUrl = (relativePath) => {
  if (!relativePath) {
    return '/static/default-avatar.jpg'; // 默认头像
  }
  if (relativePath.startsWith('http')) {
      return relativePath;
  }
  const baseUrl = config.baseURL.endsWith('/') ? config.baseURL : config.baseURL + '/';
  const avatarPath = relativePath.startsWith('/') ? relativePath.substring(1) : relativePath;
  return baseUrl + avatarPath;
}

const loadAttendantInfo = async () => {
	const userInfo = uni.getStorageSync('userInfo')
	if (!userInfo || !userInfo.id) {
		uni.showToast({ title: '请先登录', icon: 'none' })
		setTimeout(() => {
			uni.reLaunch({ url: '/subpkg/auth/login' })
		}, 1500)
		return
	}

	try {
		const res = await get(`/attendant/profile/${userInfo.id}`)
		if (res.code === 200 && res.data) {
			attendantInfo.value = res.data
		} else {
		    // 如果接口失败，回退到本地缓存
		    attendantInfo.value = userInfo
		}
	} catch (e) {
		console.error('获取陪诊师信息失败:', e)
		// 如果接口失败，回退到本地缓存
		attendantInfo.value = userInfo
	}
}

const editProfile = () => {
	uni.showToast({ title: '编辑功能开发中', icon: 'none' })
}

const navigateTo = (url) => {
	uni.showToast({ title: '功能开发中', icon: 'none' })
}

const logout = () => {
	uni.showModal({
		title: '确认退出',
		content: '确定要退出登录吗？',
		success: (res) => {
			if (res.confirm) {
				clearToken()
				uni.removeStorageSync('userInfo')
				uni.removeStorageSync('isLoggedIn')
				uni.showToast({ title: '已退出登录', icon: 'success' })
				setTimeout(() => {
					uni.reLaunch({ url: '/subpkg/auth/login' })
				}, 1500)
			}
		}
	})
}
</script>

<style lang="scss" scoped>
.container {
	min-height: 100vh;
	background-color: #f5f5f5;
	padding-bottom: calc(24px + env(safe-area-inset-bottom));
}

.user-card {
	background-color: #ffffff;
	margin: 16px;
	border-radius: 8px;
	padding: 24px;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
	position: relative;
	overflow: hidden;
	
	&::before {
		content: '';
		position: absolute;
		top: -50%;
		right: -20%;
		width: 200px;
		height: 200px;
		background: #4A90E2;
		opacity: 0.05;
		border-radius: 50%;
	}
	
	.user-info {
		display: flex;
		align-items: center;
		margin-bottom: 24px;
		position: relative;
		z-index: 1;
		
		.avatar {
			width: 70px;
			height: 70px;
			border-radius: 50%;
			margin-right: 16px;
			border: 3px solid #4A90E2;
			box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
			transition: all 0.3s ease;
			
			&:active {
				transform: scale(0.95);
			}
		}
		
		.user-details {
			flex: 1;
			
			.name {
				font-size: 18px;
				font-weight: 600;
				color: #333333;
				display: block;
				margin-bottom: 4px;
			}
			
			.phone {
				font-size: 14px;
				color: #666666;
				display: block;
				margin-bottom: 8px;
			}
			
			.status {
				.status-text {
					font-size: 12px;
					padding: 4px 8px;
					border-radius: 4px;
					font-weight: 500;
					position: relative;
					
					&.online {
						background-color: rgba(76, 175, 80, 0.1);
						color: #52C41A;
						border: 1px solid rgba(76, 175, 80, 0.2);
						
						&::before {
							content: '';
							position: absolute;
							left: 6px;
							top: 50%;
							transform: translateY(-50%);
							width: 4px;
							height: 4px;
							background: #52C41A;
							border-radius: 50%;
							animation: pulse 2s infinite;
						}
					}
					
					&.offline {
						background-color: rgba(244, 67, 54, 0.1);
						color: #FF4D4F;
						border: 1px solid rgba(244, 67, 54, 0.2);
					}
					
					&.busy {
						background-color: rgba(255, 152, 0, 0.1);
						color: #FF9500;
						border: 1px solid rgba(255, 152, 0, 0.2);
					}
				}
			}
		}
		
		.edit-btn {
			position: absolute;
			top: 16px;
			right: 16px;
			width: 36px;
			height: 36px;
			border-radius: 50%;
			background-color: rgba(255, 255, 255, 0.9);
			border: 1px solid #f0f0f0;
			transition: all 0.3s ease;
			
			&:active {
				background-color: #4A90E2;
				border-color: #4A90E2;
				transform: scale(0.95);
				
				text {
					color: #ffffff;
				}
			}
			
			text {
				font-size: 14px;
				color: #666666;
				font-weight: 500;
				transition: color 0.3s ease;
			}
		}
	}
	
	.stats {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 12px;
		position: relative;
		z-index: 1;
		
		.stat-item {
			text-align: center;
			transition: all 0.3s ease;
			padding: 12px;
			border-radius: 6px;
			
			&:active {
				background-color: #f8f9fa;
				transform: scale(0.95);
			}
			
			.stat-number {
				font-size: 20px;
				font-weight: 700;
				color: #4A90E2;
				display: block;
				margin-bottom: 4px;
				line-height: 1;
			}
			
			.stat-label {
				font-size: 12px;
				color: #999999;
				font-weight: 500;
			}
		}
	}
}

.menu-section {
	margin: 0 16px;
	
	.menu-group {
		background-color: #ffffff;
		border-radius: 8px;
		margin-bottom: 16px;
		overflow: hidden;
		box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
		animation: slideUp 0.3s ease-out;
		
		.menu-item {
			display: flex;
			align-items: center;
			padding: 16px;
			border-bottom: 1px solid #f0f0f0;
			transition: all 0.3s ease;
			position: relative;
			overflow: hidden;
			
			&:last-child {
				border-bottom: none;
			}
			
			&::before {
				content: '';
				position: absolute;
				top: 0;
				left: -100%;
				width: 100%;
				height: 100%;
				background: linear-gradient(90deg, transparent, rgba(74, 144, 226, 0.1), transparent);
				transition: left 0.5s ease;
			}
			
			&:active {
				background-color: #f8f9fa;
				transform: scale(0.98);
				
				&::before {
					left: 100%;
				}
				
				.menu-arrow {
					transform: translateX(4px);
				}
			}
			
			.menu-icon {
				width: 24px;
				height: 24px;
				margin-right: 12px;
				color: #4A90E2;
				transition: all 0.3s ease;
			}
			
			.menu-text {
				flex: 1;
				font-size: 16px;
				color: #333333;
				font-weight: 500;
			}
			
			.menu-status {
				margin-right: var(--spacing-sm);
				
				&.verified {
					text {
						font-size: 12px;
						color: var(--success-color);
						background-color: rgba(76, 175, 80, 0.1);
						padding: var(--spacing-xs) var(--spacing-sm);
						border-radius: var(--radius-small);
						border: 1px solid rgba(76, 175, 80, 0.2);
						font-weight: 500;
					}
				}
			}
			
			.menu-balance {
				margin-right: var(--spacing-sm);
				
				text {
					font-size: 14px;
					color: var(--primary-color);
					font-weight: 600;
				}
			}
			
			.menu-arrow {
				font-size: 16px;
				color: #999999;
				transition: all 0.3s ease;
			}
		}
	}
}

.logout-section {
	margin: 24px 16px 0;
	
	.logout-btn {
		width: 100%;
		height: 50px;
		background: linear-gradient(135deg, #FF4D4F, #ff3838);
		color: #ffffff;
		border: none;
		border-radius: 8px;
		font-size: 16px;
		font-weight: 600;
		transition: all 0.3s ease;
		box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
		position: relative;
		overflow: hidden;
		
		&::before {
			content: '';
			position: absolute;
			top: 50%;
			left: 50%;
			width: 0;
			height: 0;
			background: rgba(255, 255, 255, 0.3);
			border-radius: 50%;
			transition: all 0.3s ease;
			transform: translate(-50%, -50%);
		}
		
		&:active {
			background: linear-gradient(135deg, #ff3838, #ff1f1f);
			transform: scale(0.98);
			box-shadow: 0 2px 8px rgba(255, 77, 79, 0.4);
			
			&::before {
				width: 100px;
				height: 100px;
			}
		}
	}
}

// 响应式适配
@media (max-width: 375px) {
	.user-card {
		margin: 12px;
		padding: 16px;
		
		.user-info {
			.avatar {
				width: 60px;
				height: 60px;
			}
			
			.user-details {
				.name {
					font-size: 16px;
				}
			}
		}
		
		.stats {
			.stat-item {
				.stat-number {
					font-size: 18px;
				}
			}
		}
	}
	
	.menu-section {
		margin: 0 12px;
		
		.menu-group {
			.menu-item {
				padding: 12px;
			}
		}
	}
	
	.logout-section {
		margin: 16px 12px 0;
		
		.logout-btn {
			height: 45px;
			font-size: 15px;
		}
	}
}

// 动画效果
@keyframes pulse {
	0% {
		transform: scale(1);
		opacity: 1;
	}
	50% {
		transform: scale(1.2);
		opacity: 0.7;
	}
	100% {
		transform: scale(1);
		opacity: 1;
	}
}

@keyframes slideUp {
	from {
		opacity: 0;
		transform: translateY(20px);
	}
	to {
		opacity: 1;
		transform: translateY(0);
	}
}

.menu-group:nth-child(1) {
	animation: slideUp 0.3s ease-out 0.1s both;
}

.menu-group:nth-child(2) {
	animation: slideUp 0.3s ease-out 0.2s both;
}

.logout-section {
	animation: slideUp 0.3s ease-out 0.3s both;
}
</style>