<template>
	<view class="evaluate-page">
		<!-- 头部 -->
		<!-- <view class="header">
			<text class="back" @click="goBack">← 返回订单详情</text>
			<text class="title">服务评价</text>
		</view> -->

		<!-- 医生信息 -->
		<view class="doctor-info">
			<image :src="doctor.avatar" class="avatar" mode="aspectFill"></image>
			<text class="name">{{ doctor.name }}</text>
			<text class="service">{{ doctor.service }}</text>
			<text class="time">{{ doctor.time }}</text>
			<view class="rating">
				<uni-icons type="star-filled" color="#FFB300" size="24" v-for="i in 5" :key="i" />
			</view>
		</view>

		<!-- 整体服务评分 -->
		<view class="section">
			<text class="section-title">整体服务评分</text>
			<view class="stars">
				<uni-icons 
					type="star-filled" 
					color="#149DE4" 
					size="40" 
					v-for="i in 5" 
					:key="i"
					@click="setRating(i)"
					:class="{ 'active': rating >= i }"
				/>
			</view>
			<text class="tip">请为本次陪诊服务打分（必填）</text>
		</view>

		<!-- 评价内容 -->
		<view class="section">
			<text class="section-title">评价内容</text>
			<view class="textarea-wrapper">
				<textarea
					v-model="content"
					:maxlength="200"
					placeholder="请输入您的评价内容..."
					class="textarea"
				></textarea>
				<text class="char-count">{{ content.length }}/200</text>
			</view>
		</view>

		<!-- 服务亮点 -->
		<view class="section">
			<text class="section-title">服务亮点</text>
			<view class="tags">
				<view
					v-for="tag in tags"
					:key="tag"
					class="tag"
					:class="{ 'selected': selectedTags.includes(tag) }"
					@click="toggleTag(tag)"
				>
					{{ tag }}
				</view>
			</view>
		</view>

		<!-- 底部按钮 -->
		<view class="footer">
			<button class="cancel-btn" @click="cancel">取消评价</button>
			<button class="submit-btn" @click="submit">提交评价</button>
		</view>
	</view>
	<view class="evaluate-page">
		<!-- 加载中 -->
		<view v-if="loading" class="loading">加载中...</view>
	
		<!-- 错误提示 -->
		<view v-else-if="error" class="error">{{ error }}</view>
	
		<!-- 正常显示 -->
		<view v-else class="content">
			<text>服务名称：{{ orderData.serviceName }}</text>
			<text>医院：{{ orderData.hospital }}</text>
			<text>时间：{{ orderData.serviceTime }}</text>
			<text>陪诊师：{{ orderData.provider }}</text>
	
			<!-- 星级评分组件 -->
			<view class="rating">
				<text>满意度：</text>
				<image src="/static/star.png" class="star" />
				<text>{{ orderData.rating }}星</text>
			</view>
	
			<!-- 评论输入框 -->
			<textarea placeholder="请输入您的评价..." v-model="orderData.comment" />
	
			<button @click="submit">提交评价</button>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const orderId = ref('')
const loading = ref(true)
const error = ref('')
const orderData = ref(null)

onLoad((options) => {
	orderId.value = options.orderId || ''
	console.log('【evaluate】收到 orderId:', orderId.value)

	if (!orderId.value) {
		error.value = '无效订单ID'
		loading.value = false
		return
	}

	// 模拟加载（替代真实请求）
	loadOrderData()
})

const loadOrderData = async () => {
	loading.value = true
	error.value = ''

	try {
		// 👇 用 mock 数据代替真实请求
		await new Promise(resolve => setTimeout(resolve, 800))

		const mockData = {
			id: orderId.value,
			serviceName: '专业陪诊服务',
			hospital: '北京协和医院',
			serviceTime: '2023-08-15 09:00-12:00',
			provider: '张医生',
			rating: 5,
			comment: ''
		}

		orderData.value = mockData
	} catch (err) {
		error.value = '加载失败，请稍后重试'
		console.error(err)
	} finally {
		loading.value = false
	}
}
</script>

<style scoped>
.evaluate-page {
	padding: 20rpx;
}
.loading, .error {
	text-align: center;
	margin: 100rpx auto;
	color: #666;
}
.content {
	background: #fff;
	border-radius: 12rpx;
	padding: 20rpx;
}
.star {
	width: 30rpx;
	height: 30rpx;
}

.tip {
	font-size: 24rpx;
	color: #999;
	margin-top: 16rpx;
}

.textarea-wrapper {
	position: relative;
}

.textarea {
	width: 100%;
	height: 120rpx;
	font-size: 26rpx;
	line-height: 1.5;
	padding: 20rpx;
	border: 1rpx solid #ddd;
	border-radius: 12rpx;
}

.char-count {
	font-size: 22rpx;
	color: #999;
	text-align: right;
	margin-top: 8rpx;
}

.tags {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.tag {
	padding: 8rpx 16rpx;
	border: 1rpx solid #149DE4;
	border-radius: 20rpx;
	font-size: 24rpx;
	color: #149DE4;
}

.tag.selected {
	background-color: #149DE4;
	color: white;
}

.footer {
	display: flex;
	gap: 20rpx;
}

.cancel-btn {
	flex: 1;
	background: #fff;
	color: #149DE4;
	border: 1rpx solid #149DE4;
	border-radius: 12rpx;
	font-size: 28rpx;
}

.submit-btn {
	flex: 1;
	background: #149DE4;
	color: white;
	border-radius: 12rpx;
	font-size: 28rpx;
}
</style>