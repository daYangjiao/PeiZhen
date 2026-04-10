<template>
	<view class="login-container">
		<!-- 顶部装饰 -->
		<view class="header-decoration">
			<view class="decoration-circle circle1"></view>
			<view class="decoration-circle circle2"></view>
			<view class="decoration-circle circle3"></view>
		</view>
		
		<!-- Logo和标题 -->
		<view class="logo-section">
			<image class="logo" src="/static/mynewlogo.png" mode="aspectFit"></image>
			<text class="app-name">愈安伴</text>
			<text class="app-desc">专业贴心的医疗陪护服务</text>
		</view>
		
		<!-- 登录区域 -->
		<view class="login-section">
			<view class="login-card">
				<text class="login-title">欢迎使用陪诊服务</text>
				<text class="login-subtitle">请使用账号密码登录</text>
				
				<!-- 账号密码登录表单 -->
				<view class="login-form">
					<view class="input-group">
						<input 
							class="form-input" 
							type="text" 
							placeholder="请输入用户名" 
							v-model="formData.username"
						/>
					</view>
					<view class="input-group">
						<input 
							class="form-input" 
							type="password" 
							placeholder="请输入密码" 
							v-model="formData.password"
						/>
					</view>
					<button 
						class="login-btn" 
						@click="handleLogin"
						:loading="isSubmitting"
					>
						登录
					</button>
				</view>
				
				<!-- 传统注册入口 -->
				<view class="register-link">
					<text class="link-text">没有账号？</text>
					<text class="link-action" @click="navigateToRegister">立即注册</text>
				</view>
			</view>
		</view>
		
		<!-- 底部说明 -->
		<view class="footer-info">
			<view class="agreement-checkbox">
				<checkbox :checked="agreedToTerms" @tap="toggleAgreement" />
				<text class="agreement-text">我已阅读并同意</text>
				<text class="link-text" @click="viewPrivacyPolicy">《隐私政策》</text>
				<text class="agreement-text">和</text>
				<text class="link-text" @click="viewTermsOfService">《服务条款》</text>
			</view>
			<text v-if="errors.agreement" class="error-text agreement-error">{{ errors.agreement }}</text>
		</view>
	</view>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'

export default {
	data() {
			return {
				redirectUrl: '/pages/index/index',
				formData: {
					username: '',
					password: ''
				},
				isSubmitting: false,
				// 是否同意条款
				agreedToTerms: false,
				// 错误信息
				errors: {
					agreement: ''
				}
			}
	},
	
	onLoad(options) {
		// 获取重定向URL
		if (options.redirect) {
			this.redirectUrl = decodeURIComponent(options.redirect)
		}
		
		// 初始化用户store
		this.userStore = useUserStore()
		
		// 从本地存储恢复状态
		this.userStore.restoreFromStorage()
		
		// 检查是否已登录
		if (this.userStore.isLoggedIn) {
			this.navigateToHome()
		}
	},
	
	methods: {
		// 账号密码登录
		async handleLogin() {
			// 表单验证
			if (!this.formData.username) {
				uni.showToast({
					title: '请输入用户名',
					icon: 'none'
				});
				return;
			}
			
			if (!this.formData.password) {
				uni.showToast({
					title: '请输入密码',
					icon: 'none'
				});
				return;
			}
			
			// 验证是否同意条款
			if (!this.agreedToTerms) {
				this.errors.agreement = '请阅读并同意隐私政策和服务条款';
				uni.showToast({
					title: '请阅读并同意隐私政策和服务条款',
					icon: 'none'
				});
				return;
			}
			
			// 设置提交状态
			this.isSubmitting = true;
			
			try {
				// 调用登录接口
				const response = await login(this.formData);
				
				console.log('登录响应数据:', JSON.stringify(response));
				
				// 登录成功
				if (response.data && response.data.token) {
                    const userInfo = response.data.userInfo;

                    // 关键修复：检查用户类型
                    if (userInfo.userType === 1) { // 1 代表陪诊师
                        uni.showToast({
                            title: '陪诊师请使用陪诊师端登录',
                            icon: 'none',
                            duration: 2000
                        });
                        return; // 阻止后续登录逻辑
                    }

					uni.showToast({
						title: '登录成功',
						icon: 'success'
					});

					// 保存 Token 到本地存储
					uni.setStorageSync('token', response.data.token);

					// 将 Token 也合并到 userInfo 中，以防万一
					if (userInfo) {
						userInfo.token = response.data.token;
					}

					console.log('准备保存的用户信息:', JSON.stringify(userInfo));
					
					// 保存用户信息到store
					this.userStore.setUserInfo(userInfo);
					
					// 延迟跳转，让用户看到成功提示
					setTimeout(() => {
						this.navigateToHome();
					}, 1500);
				} else {
					throw new Error(response.message || '登录失败');
				}
			} catch (error) {
				console.error('登录失败:', error);
				let errorMsg = '登录失败';
				
				// 根据不同错误类型给出具体提示
				if (error.message) {
					errorMsg = error.message;
				} else if (error.statusCode === 401 || (error.data && error.data.code === 401)) {
					errorMsg = '用户名或密码错误';
				} else if (error.statusCode >= 500 || (error.data && error.data.code >= 500)) {
					errorMsg = '服务器错误，请稍后重试';
				} else if (error.statusCode) {
					errorMsg = `请求失败 (${error.statusCode})`;
				}
				
				console.log('显示错误信息:', errorMsg);
				uni.showToast({
					title: errorMsg,
					icon: 'none'
				});
			} finally {
				this.isSubmitting = false;
			}
		},
			

		
		// 跳转到首页
		navigateToHome() {
			uni.reLaunch({
				url: this.redirectUrl
			})
		},
		
		// 查看隐私政策
		viewPrivacyPolicy() {
			uni.showModal({
				title: '隐私政策',
				content: '这里是隐私政策的内容...',
				showCancel: false
			})
		},
		
		// 查看服务条款
		viewTermsOfService() {
			uni.showModal({
				title: '服务条款',
				content: '这里是服务条款的内容...',
				showCancel: false
			})
		},
		
		// 跳转到注册页面
		navigateToRegister() {
			uni.navigateTo({
				url: '/subpkg/auth/register'
			})
		},
		
		// 切换同意条款状态
		toggleAgreement() {
			this.agreedToTerms = !this.agreedToTerms
			if (this.agreedToTerms) {
				this.errors.agreement = ''
			}
		}
	}
}
</script>

<style>
.login-container {
	min-height: 100vh;
	background: linear-gradient(135deg, #4A90E2 0%, #357ABD 100%);
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0 40rpx;
	box-sizing: border-box;
}

/* 顶部装饰 */
.header-decoration {
	position: absolute;
	top: 0;
	left: 0;
	right: 0;
	height: 300rpx;
	overflow: hidden;
}

.decoration-circle {
	position: absolute;
	border-radius: 50%;
	background: rgba(255, 255, 255, 0.1);
}

.circle1 {
	width: 200rpx;
	height: 200rpx;
	top: -100rpx;
	right: -50rpx;
}

.circle2 {
	width: 150rpx;
	height: 150rpx;
	top: 50rpx;
	left: -75rpx;
}

.circle3 {
	width: 100rpx;
	height: 100rpx;
	top: 150rpx;
	right: 100rpx;
}

/* Logo区域 */
.logo-section {
	margin-top: 50rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	z-index: 10;
}

.logo {
	width: 200rpx;
	height: 200rpx;
	margin-bottom: 10rpx;
}

.app-name {
	font-size: 48rpx;
	font-weight: bold;
	color: white;
	margin-bottom: 30rpx;
}

.app-desc {
	font-size: 28rpx;
	color: rgba(255, 255, 255, 0.8);
	margin-bottom:60rpx;
}

/* 手机号登录区域样式 */
.phone-login-section {
	width: 100%;
	margin-bottom: 40rpx;
}

.input-group {
	margin-bottom: 30rpx;
}

.phone-input {
	width: 100%;
	height: 100rpx;
	padding: 0 30rpx;
	border: 2rpx solid #e0e0e0;
	border-radius: 12rpx;
	background-color: #fff;
	font-size: 32rpx;
	color: #333;
	box-sizing: border-box;
}

.phone-input:focus {
	border-color: #007aff;
}

.error-tip {
	margin-top: 10rpx;
}

.error-text {
	font-size: 24rpx;
	color: #ff3b30;
}

.phone-login-btn {
	width: 100%;
	height: 100rpx;
	background-color: #007aff;
	color: #fff;
	border: none;
	border-radius: 12rpx;
	font-size: 32rpx;
	font-weight: 500;
	display: flex;
	align-items: center;
	justify-content: center;
}

.phone-login-btn:disabled {
	background-color: #c7c7cc;
	color: #fff;
}

.phone-login-btn::after {
	border: none;
}



/* 注册链接 */
.register-link {
	display: flex;
	justify-content: center;
	margin-top: 30rpx;
}

.link-text {
	font-size: 28rpx;
	color: #666;
}

.link-action {
	font-size: 28rpx;
	color: #007aff;
	margin-left: -120rpx;
}

/* 登录区域 */
.login-section {
	flex: 1;
	width: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
}

.login-card {
	background: white;
	border-radius: 30rpx;
	padding: 60rpx 40rpx;
	width: 100%;
	max-width: 600rpx;
	box-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.1);
	display: flex;
	flex-direction: column;
	align-items: center;
}

/* 登录表单样式 */
.login-form {
	width: 100%;
	margin: 30rpx 0;
}

.form-input {
	width: 100%;
	height: 90rpx;
	padding: 0 30rpx;
	border: 2rpx solid #e0e0e0;
	border-radius: 12rpx;
	background-color: #fff;
	font-size: 28rpx;
	color: #333;
	box-sizing: border-box;
	margin-bottom: 20rpx;
}

.form-input:focus {
	border-color: #007aff;
}

.login-btn {
	width: 100%;
	height: 90rpx;
	background-color: #007aff;
	color: #fff;
	border: none;
	border-radius: 12rpx;
	font-size: 32rpx;
	font-weight: 500;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-top: 20rpx;
}

.login-btn:disabled {
	background-color: #c7c7cc;
	color: #fff;
}

.login-title {
	font-size: 36rpx;
	font-weight: bold;
	color: #333;
	margin-bottom: 20rpx;
	text-align: center;
}

.login-subtitle {
	font-size: 28rpx;
	color: #666;
	text-align: center;
	line-height: 1.5;
	margin-bottom: 60rpx;
}



/* 游客模式 */
.guest-mode {
	padding: 20rpx;
}

.guest-text {
	color: #999;
	font-size: 28rpx;
	text-decoration: underline;
}

/* 底部信息 */
.footer-info {
	padding: 40rpx 0;
	display: flex;
	flex-direction: column;
	flex-wrap: wrap;
	justify-content: center;
	align-items: center;
	width: 100%;
}

.info-text {
	color: rgba(255, 255, 255, 0.7);
	font-size: 24rpx;
}

.link-text {
	color: rgba(255, 255, 255, 0.9);
	font-size: 24rpx;
	text-decoration: underline;
	margin: 0 5rpx;
}

.agreement-checkbox {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: center;
	margin-bottom: 10rpx;
}

.agreement-text {
	font-size: 26rpx;
	color: rgba(255, 255, 255, 0.8);
	margin: 0 4rpx;
}

.error-text {
	color: #ff4d4f;
	font-size: 24rpx;
	margin-top: 8rpx;
}

.agreement-error {
	text-align: center;
	margin-top: 10rpx;
}
</style>