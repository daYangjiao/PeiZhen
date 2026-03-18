# 用户端 UI 设计规范（mini-program）

## 1）导入标准

所有用户端页面和用户专用组件样式块统一引入：

```scss
@import '@/styles/user-ui.scss';
```

推荐样式标签：

```vue
<style lang="scss" scoped>
```

## 2）设计 Token

### 颜色

- `$user-color-primary`：`#66A6FF`（主色）
- `$user-color-primary-deep`：`#4F95F0`（主色深阶）
- `$user-color-bg`：`#F5F7FA`（页面灰底）
- `$user-color-surface`：`#FFFFFF`（卡片白底）
- `$user-color-text-main`：`#1F2937`（主文本）
- `$user-color-text-sub`：`#667085`（次文本）
- `$user-color-border`：`#E7EDF5`（边框/分割线）
- `$user-color-disabled`：`#C0C4CC`（禁用态）
- `$user-color-success`：`#52C41A`
- `$user-color-warning`：`#FAAD14`
- `$user-color-danger`：`#FF4D4F`

### 圆角/阴影

- `$user-radius-card`：`16rpx`
- `$user-radius-pill`：`999rpx`
- `$user-shadow-card`：`0 10rpx 24rpx rgba(31, 41, 55, 0.08)`
- `$user-shadow-primary`：`0 10rpx 24rpx rgba(102, 166, 255, 0.28)`

### 间距/尺寸

- `$user-gap-page`：`24rpx`
- `$user-gap-card`：`24rpx`
- `$user-gap-block`：`16rpx`
- `$user-btn-height`：`88rpx`

## 3）可复用 Mixin

- `@include user-page;`：页面级容器。
- `@include user-card($padding: 24rpx);`：白卡模块。
- `@include user-primary-btn;`：主操作按钮。
- `@include user-ghost-btn($height);`：幽灵按钮。
- `@include user-tag($active);`：筛选标签/状态标签。

## 4）页面结构规范

- 页面背景统一灰底 `#F5F7FA`。
- 内容信息优先装入白卡，卡片圆角统一 `16rpx`。
- 卡片间距建议 `16rpx ~ 24rpx`。
- 大段信息不直接堆在灰底上，避免视觉散乱。
- 搜索区、筛选区、工具区优先放在同一白色模块中，减少割裂感。

## 5）按钮与状态规范

- 主按钮：浅蓝渐变 + 白字 + 轻阴影。
- 次按钮/操作按钮：白底蓝边（幽灵按钮）。
- 状态色仅使用语义色：成功绿、警告橙、失败红。
- 禁用按钮：`$user-color-disabled` 背景，文本可读但弱化。

## 6）文本层级规范

- 页面主标题：`30rpx ~ 32rpx`，`700`。
- 模块标题：`28rpx ~ 30rpx`，`600/700`。
- 正文：`24rpx ~ 28rpx`。
- 说明与时间：`22rpx ~ 26rpx`，次文本色。

## 7）动效与反馈规范

- 进场动效：`slideUp`（0.35s~0.45s）。
- 卡片点击反馈：`scale(0.98)`。
- 按钮点击反馈：轻微缩放或透明度变化。
- 避免大幅位移、闪烁和高频动画。

## 8）验收清单

1. 页面是否引入 `user-ui.scss`。
2. 根容器是否使用统一灰底。
3. 白卡圆角、阴影、边距是否一致。
4. 主蓝是否统一为 `#66A6FF / #4F95F0`。
5. 是否存在重复信息展示块（金额/状态重复）。
6. 搜索、筛选、工具区是否模块化衔接。
7. 共用页是否按角色分支，确保用户/陪诊师都正常。
8. 是否去除未引用重复页面实现。
