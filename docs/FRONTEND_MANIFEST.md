# Frontend Manifest

更新时间: 2026-04-25

本清单覆盖两个前端：uni-app 小程序端和 Vue 管理端。小程序页面来源于 `frontend/mini-program/pages.json`；管理端页面来源于 `frontend/admin/src/router/index.js`，接口封装来源于 `frontend/admin/src/utils/admin-api.js`。

## 摘要

- 小程序页面数量: 46
- 管理端路由页面数量: 11
- 小程序主包: `frontend/mini-program/pages/**`
- 小程序分包: `frontend/mini-program/subpkg/**`
- 管理端路由基座: `/admin/`

## 小程序端页面

| 页面路径 | 标题 | 核心交互逻辑 | 主要接口/动作 |
| --- | --- | --- | --- |
| `pages/auth/login` | 登录 | 用户/陪诊师登录入口，支持账号密码和微信登录分流，登录后按角色进入对应 Tab。 | `/api/users/login` |
| `pages/role-user/order` | 我的订单 | 用户订单列表，查看订单状态、进入详情、处理待支付/服务中/评价等流程。 | `/api/orders/{...}`<br>`/api/orders/user-orders` |
| `pages/role-user/message` | 消息中心 | 用户消息中心，加载聊天联系人和系统消息，跳转聊天或系统消息详情。 | `/api/chat/contacts` |
| `pages/role-user/profile` | 个人中心 | 用户个人中心，展示账户信息，进入资料编辑、订单、消息、设置等入口。 | 无直接接口调用/通过封装模块调用 |
| `pages/ai-triage/01-appointment-selection` | 预约类型选择 | 预约类型选择，承接首页/AI 分流，进入普通预约或 AI 导诊。 | 无直接接口调用/通过封装模块调用 |
| `pages/role-escort/hall` | 陪诊接单大厅 | 陪诊师接单大厅，先检查资质门禁；未通过、驳回、封禁、过期或资料不完整时不请求待接订单，待审核显示审核中状态。 | `/attendant/profile/{...}`<br>`/attendant/orders/{...}`<br>`/attendant/orders/{...}/accept?attendantId={...}`<br>`/attendant/orders/waiting` |
| `pages/role-escort/order` | 陪诊师订单 | 陪诊师订单列表，按状态查看历史和服务订单；审核失败进入时弹窗提示但不阻断历史订单。 | `/attendant/profile/{...}`<br>`/attendant/orders` |
| `pages/role-escort/message` | 消息中心 | 陪诊师消息中心，加载联系人和系统消息；审核失败进入时弹窗提示。 | `/attendant/profile/{...}`<br>`/api/chat/contacts` |
| `pages/role-escort/profile` | 我的 | 陪诊师个人中心，展示资料、收入、资质门禁状态和服务统计入口。 | `/attendant/profile/{...}` |
| `pages/role-user/home` | 陪诊服务 | 用户首页，推荐陪诊师、服务入口、AI 助手/AI 导诊入口和陪诊师详情跳转。 | 无直接接口调用/通过封装模块调用 |
| `pages/public/index` | 愈安陪诊 | 公开落地页/角色入口，按登录状态和角色引导进入用户端或陪诊师端。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/chat/chat` | 聊天 | 用户侧聊天页，加载历史消息、发送文本/图片/语音并同步已读。 | `/api/chat/history?targetUserId={...}&page=1&pageSize={...}`<br>`/api/chat/history?targetUserId={...}&page={...}&pageSize={...}`<br>`/api/chat/read?senderId={...}`<br>`/api/chat/send`<br>`/api/common/upload` |
| `subpkg/chat/chat-escort` | 聊天 | 陪诊师侧聊天页，加载历史消息、发送文本/图片/语音并同步已读。 | `/api/chat/history?targetUserId={...}&page=1&pageSize={...}`<br>`/api/chat/history?targetUserId={...}&page={...}&pageSize={...}`<br>`/api/chat/read?senderId={...}`<br>`/api/chat/send`<br>`/api/common/upload` |
| `subpkg/system-message/system-message` | 系统消息 | 系统消息列表，读取消息、按消息动作跳转订单详情/评价/接单处理。 | `/ai/guide/orders/{...}/complete-info`<br>`/api/chat/system/{...}`<br>`/attendant/orders/{...}`<br>`/api/orders/{...}`<br>`/api/chat/system`<br>`/api/chat/read?senderId=0` |
| `subpkg/system-message/escort-detail` | 系统消息详情 | 陪诊师系统消息详情，查看派单/订单相关消息并跳转订单。 | `/attendant/orders/{...}`<br>`/api/chat/system/{...}` |
| `subpkg/order/order-detail` | 订单详情 | 用户订单详情，查看订单、支付尾款、取消、申诉、确认时长费用、补差额支付、查看二维码/联系陪诊师。 | `/api/orders/{...}/cancel?reason={...}`<br>`/api/orders/{...}/dispute-time-fee{...}`<br>`/api/orders/{...}/confirm-time-fee`<br>`/api/orders/{...}/pay-balance`<br>`/api/orders/{...}/evaluation`<br>`/ai/guide/orders/{...}/complete-info` |
| `subpkg/order/escort-detail` | 订单详情（陪诊师端） | 陪诊师订单详情，接单后执行开始服务、结束服务、取消、扫码核销、评价查看等。 | `/attendant/orders/{...}/evaluation`<br>`/attendant/orders/{...}/evaluation/reply`<br>`/attendant/orders/{...}/service-progress?step={...}`<br>`/attendant/orders/{...}`<br>`/attendant/orders/{...}/scan-qr?qrCodeContent={...}`<br>`/attendant/orders/{...}/cancel` |
| `subpkg/order/prepare` | 服务前准备清单 | 服务前准备清单，展示陪诊服务前注意事项。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/order/submit-time-fee` | 提交时长与费用 | 陪诊师提交实际服务时长和费用，驱动用户确认或争议流程。 | `/attendant/orders/{...}`<br>`/attendant/orders/{...}/end?actualDuration={...}` |
| `subpkg/evaluate/evaluate` | 订单评价 | 用户订单评价，加载订单和既有评价，提交星级、标签和文字内容。 | `/ai/guide/orders/{...}/complete-info`<br>`/api/orders/{...}/evaluation` |
| `subpkg/auth/escort-register` | 陪诊师入驻 | 陪诊师入驻注册，填写基础信息、擅长领域、医院和简介后提交。 | `/api/users/register` |
| `subpkg/auth/user-register` | 用户注册 | 用户注册，填写手机号、姓名、密码等基础信息后提交。 | `/api/users/register` |
| `subpkg/auth/wechat-bind` | 绑定手机号 | 微信登录后绑定手机号/姓名/密码，完成账号合并并进入角色首页。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/appointment-flow/01-appointment-selection` | 预约类型选择 | 普通预约流程入口，选择服务类型并进入预约表单。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/appointment-flow/02-appointment-form` | 服务预约 | 普通预约表单，填写患者/医院/时间/需求，创建预约、匹配陪诊师并生成订单。 | `/ai/guide/attendants/match?appointmentNo={...}`<br>`/ai/guide/appointments`<br>`/ai/guide/orders`<br>`/api/users/current` |
| `subpkg/appointment-flow/04-order-confirm-page` | 确认订单 | 订单确认与模拟支付，加载完整订单信息并提交支付状态。 | `/ai/guide/orders/{...}/complete-info`<br>`/ai/guide/payments/status` |
| `subpkg/appointment-flow/05-payment-success-page` | 支付成功 | 支付成功页，展示订单摘要并引导查看订单详情。 | `/ai/guide/orders/{...}/complete-info` |
| `subpkg/appointment-flow/06-payment-failed-page` | 支付失败 | 支付失败页，提示重试或返回订单确认。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/ai/ai-ask` | AI助手 | AI 医疗问答助手，恢复最近会话、发送问题、轮询回答/思考过程。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/ai-appointment/01-ai-appointment` | AI导诊 | AI 导诊对话页，结构化采集需求、追问补全、确认时间并启动匹配。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/ai-appointment/02-ai-match-result` | AI推荐结果 | AI 推荐结果页，展示匹配陪诊师，选择陪诊师并创建订单。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/attendant-detail` | 陪诊师主页 | 陪诊师主页，展示公开资料/评分/资质，引导指定 TA 预约或普通预约。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/edit-profile` | 编辑个人资料 | 用户编辑个人资料和头像。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/avatar-crop` | 裁剪头像 | 头像裁剪工具页，裁剪后返回上传流程。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/id-card-crop` | 身份证框选 | 身份证正反面上传前的自定义框选页，按身份证比例裁切并输出清晰审核图。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/edit-escort` | 编辑资料 | 陪诊师编辑个人资料、擅长领域、医院和简介。 | `/attendant/profile/{...}` |
| `subpkg/profile/withdraw-center` | 提现中心 | 提现中心入口，跳转钱包明细的提现 Tab。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/wallet-detail` | 钱包明细 | 陪诊师钱包明细，统计收入、提现记录并保留提现申请入口。 | `/attendant/orders`<br>`/attendant/withdraw/records`<br>`/attendant/withdraw/apply` |
| `subpkg/profile/qualification` | 资质管理 | 陪诊师资质管理，查看审核进度、材料完整度、证件有效期、驳回原因和最近审核记录，并提交审核。 | `/attendant/profile/{...}`<br>`/attendant/qualification/{...}/submit` |
| `subpkg/profile/qualification-upload` | 上传资质 | 上传/更新身份证、执业证、健康证和证件有效期；身份证正反面先进入自定义框选页，图片上传后同时保存原图和后端生成的扫描预览图。 | `/attendant/qualification/{...}`<br>`/api/common/upload-image` |
| `subpkg/profile/reviews` | 我的评价 | 陪诊师评价列表，查看订单评价并回复用户评价。 | `/attendant/orders`<br>`/attendant/orders/` |
| `subpkg/profile/service-stats` | 服务统计 | 陪诊师服务统计，汇总已完成订单、收入和评分。 | `/attendant/orders` |
| `subpkg/profile/platform-rules` | 平台规则 | 平台规则说明页。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/service-center` | 客服中心 | 客服中心入口，跳转消息/联系支持。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/help` | 帮助中心 | 帮助中心常见问题。 | 无直接接口调用/通过封装模块调用 |
| `subpkg/profile/settings` | 设置 | 设置页，通知开关、缓存清理和退出登录。 | 无直接接口调用/通过封装模块调用 |

## 管理端页面

| 路由 | 组件 | 权限 | 核心交互逻辑 | 主要接口 |
| --- | --- | --- | --- | --- |
| `/login` | `LoginPage.vue` | 公开/游客 | 管理员登录，保存 admin token 并按 redirect 跳转。 | `POST /api/admin/auth/login` |
| `/dashboard` | `DashboardPage.vue` | 管理员 JWT | 首页概览，查看核心经营数据、待审核/争议/今日订单快捷入口；超级管理员可见最近操作日志摘要。 | `GET /api/admin/dashboard/overview` |
| `/workbench` | `WorkbenchPage.vue` | 管理员 JWT | 处理工作台，统一流水线处理订单争议和陪诊师入驻审核；系统自动分配和续期任务、实时刷新队列、完成后进入下一条。陪诊师审核默认展示扫描预览图，放大查看展示用户上传原图。 | `GET /api/admin/workbench/summary`<br>`GET /api/admin/workbench/tasks`<br>`POST /api/admin/workbench/tasks/{type}/{targetId}/claim`<br>`POST /api/admin/workbench/tasks/{type}/{targetId}/complete`<br>`DELETE /api/admin/workbench/tasks/{type}/{targetId}/claim`<br>`GET /api/admin/orders/{id}`<br>`GET /api/admin/attendants/{id}` |
| `/users` | `UsersPage.vue` | 管理员 JWT | 用户筛选、分页、档案抽屉查看、启用/禁用用户。列表点击或查看资料直接打开右侧工作抽屉展示完整资料、资质与最近订单。 | `GET /api/admin/users`<br>`GET /api/admin/users/{id}`<br>`PATCH /api/admin/users/{id}/status` |
| `/attendants` | `AttendantsPage.vue` | 管理员 JWT | 陪诊师审核工作台，展示材料完整度、有效期、最近审核记录；资质缩略图默认用扫描预览图，点开看原图。材料缺失或过期时禁用通过。超级管理员可看到审核人字段。 | `GET /api/admin/attendants`<br>`GET /api/admin/attendants/{id}`<br>`GET /api/admin/attendants/{id}/qualification-logs`<br>`PATCH /api/admin/attendants/{id}/qualification-review`<br>`PATCH /api/admin/attendants/{id}/status` |
| `/attendants/:id` | `AttendantDetailPage.vue` | 管理员 JWT | 陪诊师详情、历史订单、资质有效期、审核记录、禁用/恢复；资质缩略图默认用扫描预览图，点开看原图。超级管理员可看到审核人字段。 | `GET /api/admin/attendants/{id}`<br>`GET /api/admin/attendants/{id}/qualification-logs`<br>`PATCH /api/admin/attendants/{id}/qualification-review`<br>`PATCH /api/admin/attendants/{id}/status` |
| `/orders` | `OrdersPage.vue` | 管理员 JWT | 订单筛选、详情抽屉查看、取消订单、争议处理。列表点击或查看记录直接打开右侧工作抽屉展示完整资料与处理操作，并同步 `selectedId` 查询参数。 | `GET /api/admin/orders`<br>`GET /api/admin/orders/{id}`<br>`PATCH /api/admin/orders/{id}/cancel`<br>`PATCH /api/admin/orders/{id}/dispute-resolution` |
| `/orders/:id` | `OrderDetailPage.vue` | 管理员 JWT | 订单详情、取消订单、时长费用争议处理。 | `GET /api/admin/orders/{id}`<br>`PATCH /api/admin/orders/{id}/cancel`<br>`PATCH /api/admin/orders/{id}/dispute-resolution` |
| `/system` | `SystemPage.vue` | 超级管理员 JWT | 管理员账号管理，创建管理员/超级管理员、启用/停用/删除账号。普通管理员不显示该入口，手动访问会静默回首页。 | `GET /api/admin/admin-users`<br>`POST /api/admin/admin-users`<br>`PATCH /api/admin/admin-users/{id}/status`<br>`DELETE /api/admin/admin-users/{id}` |
| `/logs` | `LogsPage.vue` | 超级管理员 JWT | 操作日志工作台，按模块、动作、管理员角色、关键词和时间范围分页查询后台处理记录。普通管理员不显示该入口，手动访问会静默回首页。 | `GET /api/admin/operation-logs` |

## 导航和鉴权

- 小程序使用原生 Tab/分包跳转，登录态由 `stores/session.js`、`stores/user.js` 和请求封装维护。
- 管理端使用 Vue Router，`meta.requiresAuth` 路由必须存在管理端 token；401 响应会清理会话并跳转 `/admin/login`。
- 管理端侧栏入口在 `frontend/admin/src/components/AppShell.vue` 中维护。
- 管理端浏览器标题为“愈安伴后台管理”，登录页、侧栏和 favicon 统一使用 `frontend/admin/public/brand-logo.png`。
- 管理端活动弹窗统一使用 `BaseDialog.vue` 和 `styles.css` 中的 `.dialog-*` 自定义样式，不使用浏览器原生确认框；列表详情使用 `BaseDrawer.vue` 和 `.drawer-*` 自定义工作抽屉，避免详情堆到页面底部；筛选、分页和管理员账号类型选择使用 `BaseSelect.vue` 自定义下拉，日期/时间筛选使用 `BaseDateInput.vue` 自定义日历浮层，避免浏览器原生下拉和日期弹窗样式。

## 维护规则

- 修改 `pages.json`、新增/删除小程序页面、改变核心跳转或接口时，必须同步更新“小程序端页面”。
- 修改 `frontend/admin/src/router/index.js`、`admin-api.js` 或管理端页面核心操作时，必须同步更新“管理端页面”。
