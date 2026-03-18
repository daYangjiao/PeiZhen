# 陪诊师端 UI 设计规范（mini-program）

## 1）导入规范

在每个陪诊师端页面/组件的样式块顶部统一引入：

```scss
@import '@/styles/escort-ui.scss';
```

推荐样式标签：

```vue
<style lang="scss" scoped>
```

## 2）核心 Token

### 颜色体系

- `$escort-color-primary`：`#66a6ff`（主色）
- `$escort-color-primary-deep`：`#4f95f0`（主色深阶）
- `$escort-color-bg`：`#f5f7fa`（页面背景）
- `$escort-color-surface`：`#ffffff`（卡片背景）
- `$escort-color-text-main`：`#1f2937`（主文本）
- `$escort-color-text-sub`：`#667085`（次文本）
- `$escort-color-border`：`#e7edf5`（边框/分割线）
- `$escort-color-success`：`#52c41a`（成功）
- `$escort-color-warning`：`#faad14`（警告）
- `$escort-color-danger`：`#ff4d4f`（危险）

### 圆角与阴影

- `$escort-radius-card`：`16rpx`
- `$escort-radius-pill`：`999rpx`
- `$escort-shadow-card`：`0 10rpx 24rpx rgba(31, 41, 55, 0.08)`
- `$escort-shadow-primary`：`0 10rpx 24rpx rgba(102, 166, 255, 0.28)`

### 间距与尺寸

- `$escort-gap-page`：`24rpx`
- `$escort-gap-card`：`24rpx`
- `$escort-gap-block`：`16rpx`
- `$escort-btn-height`：`88rpx`

## 3）Mixin 使用规则

- `@include escort-page;`：页面根容器统一灰底。
- `@include escort-card($padding: 24rpx);`：统一卡片样式。
- `@include escort-primary-btn;`：统一主操作按钮。

## 4）布局规则

- 页面根容器必须使用 `@include escort-page`。
- 页面左右内边距默认 `24rpx`。
- 卡片默认：`16rpx` 圆角、`24rpx` 内边距、统一阴影。
- 卡片间距建议 `16rpx ~ 24rpx`。
- 主按钮默认使用渐变主色，禁止页面自行定义另一套主按钮风格。

## 5）文字层级规则

- 页面/模块标题：`30rpx ~ 32rpx`，字重 `700`，主文本色。
- 正文信息：`24rpx ~ 28rpx`，主文本色。
- 辅助说明：`22rpx ~ 26rpx`，次文本色。
- 风险与错误提示：使用 `$escort-color-danger`。

## 6）状态与标签规则

- 成功状态：`$escort-color-success`
- 待处理/警告：`$escort-color-warning`
- 失败/异常：`$escort-color-danger`
- 激活标签/按钮：主色渐变 + 白字

## 7）交互动效规则

- 卡片点击反馈：

```scss
:active {
  transform: scale(0.98);
}
```

- 主按钮点击反馈可使用轻微缩放或亮度变化。
- 入场动效建议 `slideUp`，时长 `0.35s ~ 0.45s`，避免过度动画。

## 8）新页面复用检查清单

1. 是否使用 `lang="scss"` 并引入 `escort-ui.scss`。
2. 根容器是否统一灰底（`@include escort-page`）。
3. 页面边距是否统一为 `24rpx`。
4. 卡片是否统一圆角 `16rpx` 与阴影。
5. 主操作按钮是否复用统一主按钮风格。
6. 标题/正文/说明文字层级是否一致。
7. 状态色是否仅来自系统 Token。
8. 是否避免重复信息区块（尤其金额、状态信息）。
