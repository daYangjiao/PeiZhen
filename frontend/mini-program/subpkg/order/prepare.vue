<template>
	<view class="container">
		<scroll-view class="content" scroll-y>
			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/requirement.svg" mode="aspectFit"></image>
					<text>通用必备</text>
				</view>
				<view class="check-list">
					<view class="check-item" v-for="(item, idx) in generalItems" :key="`g${idx}`">
						<view class="bullet"></view>
						<text class="check-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="info-card">
				<view class="card-title">
					<image class="title-icon" src="/static/symptom-modern.svg" mode="aspectFit"></image>
					<text>订单专属·患者需求</text>
				</view>
				<view class="check-list">
					<view class="check-item" v-for="(item, idx) in orderItems" :key="`o${idx}`">
						<view class="bullet"></view>
						<text class="check-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="tips-card">
				<text class="tips-title">温馨提示</text>
				<text class="tips-text">请提前核对以上准备事项，与患者沟通就诊安排后，再按流程开始服务。</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			orderId: '',
			generalItems: [
				{ label: '工作证/资质证明' },
				{ label: '手机(扫码功能)' },
				{ label: '口罩/手套' },
				{ label: '签字笔/便签本' }
			],
			orderItems: []
		}
	},
	onLoad(options) {
		this.orderId = options.orderId || ''
		const symptom = decodeURIComponent(options.symptom || '')
		const other = decodeURIComponent(options.other || '')

		if (this.orderId) {
			uni.setStorageSync(`order_prepared_${this.orderId}`, '1')
		}

		const items = []
		if (symptom && symptom !== '无') {
			if (symptom.includes('青霉素') || symptom.toLowerCase().includes('过敏')) {
				items.push({ label: '对青霉素过敏' })
			}
			items.push({ label: symptom })
		}
		items.push({ label: '医院科室导航图' })
		items.push({ label: '取药袋/病历夹' })
		items.push({ label: '轮椅(根据需求)' })
		if (other && other !== '无') {
			items.push({ label: other })
		}

		const seen = new Set()
		this.orderItems = items.filter((item) => {
			if (!item.label || seen.has(item.label)) return false
			seen.add(item.label)
			return true
		})

		if (this.orderItems.length === 0) {
			this.orderItems = [
				{ label: '医院科室导航图' },
				{ label: '取药袋/病历夹' },
				{ label: '轮椅(根据需求)' }
			]
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/escort-ui.scss';

.container {
	min-height: 100vh;
	background: #f5f7fa;
	--text-main: #1f2937;
	--text-sub: #667085;
}

.content {
	min-height: 100vh;
	padding: 24rpx 0 40rpx;
}

.info-card,
.tips-card {
	background: $escort-color-surface;
	border-radius: $escort-radius-card;
	padding: 28rpx 24rpx;
	margin: 0 24rpx 20rpx;
	box-shadow: $escort-shadow-card;
	border: 1rpx solid #e7edf5;
}

.card-title {
	display: flex;
	align-items: center;
	margin-bottom: 18rpx;
	font-size: 30rpx;
	font-weight: 600;
	color: var(--text-main);
}

.title-icon {
	width: 34rpx;
	height: 34rpx;
	margin-right: 10rpx;
}

.check-list {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.check-item {
	display: flex;
	align-items: flex-start;
	padding: 10rpx 0;
}

.bullet {
	width: 14rpx;
	height: 14rpx;
	margin: 12rpx 16rpx 0 2rpx;
	border-radius: 50%;
	background: $escort-color-primary;
	flex-shrink: 0;
}

.check-label {
	font-size: 28rpx;
	color: var(--text-main);
	line-height: 1.6;
}

.tips-title {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: var(--text-main);
	margin-bottom: 12rpx;
}

.tips-text {
	display: block;
	font-size: 26rpx;
	line-height: 1.7;
	color: var(--text-sub);
}
</style>
