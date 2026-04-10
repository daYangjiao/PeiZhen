<template>
	<view class="container">
		<view class="header">
			<text class="title">登录功能测试</text>
		</view>
		
		<view class="content">
			<view class="info-section">
				<text class="section-title">当前登录状态</text>
				<view class="status-item">
					<text>是否已登录: {{ isLoggedIn ? '是' : '否' }}</text>
				</view>
				<view class="status-item">
					<text>用户ID: {{ userInfo?.id || '未登录' }}</text>
				</view>
				<view class="status-item">
					<text>用户名: {{ userInfo?.username || '未登录' }}</text>
				</view>
				<view class="status-item">
					<text>显示名称: {{ displayName || '未登录' }}</text>
				</view>
				<view class="status-item">
					<text>用户类型: {{ userTypeLabel }}</text>
				</view>
			</view>
			
			<view class="test-section">
				<text class="section-title">测试功能</text>
				<button class="test-btn" @click="testLogin">测试登录</button>
				<button class="test-btn" @click="testLogout">退出登录</button>
				<button class="test-btn" @click="refreshUserInfo">刷新用户信息</button>
			</view>
			
			<view class="log-section">
				<text class="section-title">日志输出</text>
				<scroll-view class="log-container" scroll-y="true">
					<view v-for="(log, index) in logs" :key="index" class="log-item">
						<text>[{{ log.time }}] {{ log.message }}</text>
					</view>
				</scroll-view>
			</view>
		</view>
	</view>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'

export default {
	data() {
		return {
			logs: []
		}
	},
	
	computed: {
		userStore() {
			return useUserStore()
		},
		
		isLoggedIn() {
			return this.userStore.isLoggedIn
		},
		
		userInfo() {
			return this.userStore.userInfo
		},
		
		displayName() {
			return this.userStore.displayName
		},
		
		userTypeLabel() {
			if (!this.userInfo) return '未登录'
			const userType = this.userInfo.userType
			return userType === 1 ? '陪诊师' : '普通用户'
		}
	},
	
	onLoad() {
		this.addLog('页面加载完成')
		this.refreshUserInfo()
	},
	
	methods: {
		addLog(message) {
			const time = new Date().toLocaleTimeString()
			this.logs.push({
				time,
				message
			})
			console.log(`[${time}] ${message}`)
		},
		
		refreshUserInfo() {
			this.userStore.restoreFromStorage()
			this.addLog('刷新用户信息完成')
		},
		
		async testLogin() {
			this.addLog('开始测试登录...')
			
			try {
				// 使用测试账号登录
				const testData = {
					username: 'testuser',
					password: '123456'
				}
				
				this.addLog(`尝试登录用户: ${testData.username}`)
				
				const response = await login(testData)
				this.addLog('登录接口调用成功')
				this.addLog(`响应数据: ${JSON.stringify(response.data)}`)
				
				if (response.data && response.data.token) {
					this.addLog('登录成功!')
					this.refreshUserInfo()
				} else {
					this.addLog('登录失败: 未返回有效token')
				}
			} catch (error) {
				let errorMsg = '登录失败';
				if (error.message) {
					errorMsg = error.message;
				} else if (error.statusCode === 401 || (error.data && error.data.code === 401)) {
					errorMsg = '用户名或密码错误';
				} else if (error.statusCode) {
					errorMsg = `请求失败 (${error.statusCode})`;
				}
				
				this.addLog(`登录失败: ${errorMsg}`)
				console.error('登录测试错误:', error)
			}
		},
		
		testLogout() {
			this.addLog('执行退出登录')
			this.userStore.clearUserInfo()
			this.addLog('退出登录完成')
		}
	}
}
</script>

<style scoped>
.container {
	padding: 20rpx;
}

.header {
	text-align: center;
	margin-bottom: 40rpx;
}

.title {
	font-size: 36rpx;
	font-weight: bold;
	color: #333;
}

.content {
}

.info-section, .test-section, .log-section {
	background: #fff;
	border-radius: 20rpx;
	padding: 30rpx;
	margin-bottom: 30rpx;
	box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}

.section-title {
	font-size: 32rpx;
	font-weight: bold;
	color: #333;
	margin-bottom: 20rpx;
	display: block;
}

.status-item {
	padding: 15rpx 0;
	border-bottom: 1rpx solid #eee;
}

.status-item:last-child {
	border-bottom: none;
}

.test-btn {
	width: 100%;
	height: 80rpx;
	background: #007aff;
	color: #fff;
	border: none;
	border-radius: 12rpx;
	font-size: 30rpx;
	margin-bottom: 20rpx;
}

.test-btn:active {
	background: #0062cc;
}

.log-container {
	height: 400rpx;
	background: #f5f5f5;
	border-radius: 12rpx;
	padding: 20rpx;
}

.log-item {
	padding: 10rpx 0;
	font-size: 24rpx;
	color: #666;
	border-bottom: 1rpx solid #eee;
}

.log-item:last-child {
	border-bottom: none;
}
</style>