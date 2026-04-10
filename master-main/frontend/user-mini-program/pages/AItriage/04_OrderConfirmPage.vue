<template>
  <view class="order-confirm-page">
    <!-- 导航栏 -->
    <navigator url="/pages/index/index" class="back-btn">
      <!-- <text class="iconfont icon-back"> 返回</text> -->
    </navigator>

    <!-- 进度条 -->
    <view class="step-bar">
      <view class="step-item completed">
        <text class="step-dot">✓</text>
        <text class="step-text">选择服务</text>
      </view>
      <view class="progress-line green"></view>
      <view class="step-item completed">
        <text class="step-dot">✓</text>
        <text class="step-text">AI陪诊师匹配</text>
      </view>
      <view class="progress-line green"></view>
      <view class="step-item completed">
        <text class="step-dot">✓</text>
        <text class="step-text">选择陪诊师</text>
      </view>
      <view class="progress-line green"></view>
      <view class="step-item active">
        <text class="step-dot"></text>
        <text class="step-text">确认订单</text>
      </view>
    </view>

    <!-- 加载指示器 -->
    <view v-if="isLoading" class="loading-container">
      <text>加载中...</text>
    </view>

    <!-- 订单信息 -->
    <view v-else class="card order-info">
      <view class="title">
        <text class="iconfont icon-info"> 订单信息</text>
      </view>
      <view class="info-item">
        <text class="label"><text class="iconfont icon-hospital"> 就诊医院</text></text>
        <text class="value">{{ orderData.hospital || '未知' }}</text>
      </view>
      <!-- 合并就诊日期和时间 -->
      <view class="info-item">
        <text class="label"><text class="iconfont icon-calendar"> 就诊时间</text></text>
        <text class="value">{{ formatDate(orderData.serviceDate) }} {{ orderData.serviceTime || '未知' }}</text>
      </view>
      <view class="info-item">
        <text class="label"><text class="iconfont icon-user"> 就诊人</text></text>
        <text class="value">{{ orderData.patientName || '未知' }}</text>
      </view>
      <!-- 显示后端返回的 symptoms 和 otherRequirement -->
      <view class="info-item">
        <text class="label"><text class="iconfont icon-note"> 症状描述</text></text>
        <text class="value">{{ getSymptomDescription() }}</text>
      </view>
      <!-- 新增：其他需求 -->
      <view class="info-item">
        <text class="label"><text class="iconfont icon-note"> 其他需求</text></text>
        <text class="value">{{ orderData.otherRequirement || '无' }}</text>
      </view>

    </view>


    <!-- 费用明细 -->
    <view v-if="!isLoading" class="card fee-detail">
      <view class="title">
        <text class="iconfont icon-list"> 费用明细</text>
        <!-- 新增：右上角的小字“查看计费规则”按钮 -->
        <button class="rule-link-button" @click="showBillingRules = true">查看计费规则</button>
      </view>
      <view class="fee-item">
        陪诊服务费用
        <text class="price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
      <view class="fee-item">优惠券
        <text class="discount">-¥0.00</text>
      </view>
      <view class="total">
        总计
        <text class="total-price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
    </view>

    <!-- 支付方式 -->
    <view v-if="!isLoading" class="card payment-method">
      <view class="title">
        <text class="iconfont icon-pay"> 选择支付方式</text>
      </view>
      <radio-group @change="onPaymentChange" :value="payMethod">
        <view class="payment-option">
          <radio value="wechat" :checked="payMethod === 'wechat'" />
          <view class="pay-icon">
            <text class="iconfont icon-wechat"> 微信支付</text>
          </view>
        </view>
        <view class="payment-option">
          <radio value="alipay" :checked="payMethod === 'alipay'" />
          <view class="pay-icon">
            <text class="iconfont icon-alipay"> 支付宝支付</text>
          </view>
        </view>
        <view class="payment-option">
          <radio value="unionpay" :checked="payMethod === 'unionpay'" />
          <view class="pay-icon">
            <text class="iconfont icon-unionpay"> 银联支付</text>
          </view>
        </view>
      </radio-group>
    </view>

    <!-- 底部支付栏 -->
    <view v-if="!isLoading" class="footer">
      <view class="real-price">实付款：
        <text class="price">¥{{ formatAmount(orderData.totalPrice) }}</text>
      </view>
      <button class="confirm-btn" @click="confirmPay">确认支付</button>
    </view>

    <!-- 计费规则模态框 -->
    <view v-if="showBillingRules" class="modal-overlay" @click="showBillingRules = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">计费规则</text>
          <text class="close-btn" @click="showBillingRules = false">×</text>
        </view>
        <scroll-view class="modal-body" scroll-y="true">
          <text class="rule-text">
            陪诊服务费用定价与计算规则说明<br/>
            一、定价原则<br/>
            本次陪诊服务定价遵循成本导向+市场适配原则：以陪诊师服务时长、人力成本、交通成本为核心核算基础，结合本地医疗陪诊行业的市场均价，同时兼顾不同服务场景的特殊性（如急诊的加急调配、上门服务的里程成本），制定差异化定价方案，确保费用透明、性价比合理。<br/><br/>

            二、分类型费用计算规则<br/>
            （一）普通陪诊（基础核心服务）<br/>
            1. 服务定位：医院内基础陪诊（挂号、缴费、取药、就诊引导等），覆盖日常就医高频需求；<br/>
            2. 优化后定价规则：<br/>
            &nbsp;&nbsp;a. 起步价： 50 元（ 对应 2 小时最低起约时长，不足 2 小时按 2 小时计费，与原规则一致）；<br/>
            &nbsp;&nbsp;b. 延长费：由 50 元 / 小时调整为30 元 / 小时（不足 1 小时按 1 小时计），降低长时长服务用户成本；<br/>
            &nbsp;&nbsp;c. 市场对比优势：调整后长时长服务性价比突出，如 6 小时陪诊费用 = 50+30×4=170 元，较本地同类平台（180-220 元）低 5%-23%；<br/>
            &nbsp;&nbsp;d. 新增多次卡套餐（参考天鹅到家会员体系逻辑）：<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;| 套餐类型 | 包含服务次数 | 套餐价格 | 单次折合价 | 额外权益 | 适用人群 |<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;| --- | --- | --- | --- | --- | --- |<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;| 基础体验卡 | 3 次 | 199 元 | 66.3 元 | 免费病历整理 1 次、改期 1 次 | 偶尔就医、首次尝试陪诊用户 |<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;| 高频复查卡 | 10 次 | 599 元 | 59.9 元 | 免费病历整理 3 次、优先匹配陪诊师 | 慢性病复查、定期产检用户 |<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;| 年度尊享卡 | 20 次 | 1099 元 | 54.9 元 | 免费病历整理 5 次、家属远程同步服务 | 长期就医、老年独居用户 |<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;套餐规则：多次卡有效期 1 年，支持转赠，未使用次数可申请退款（按套餐价折算剩余价值）。<br/><br/>

            （二）术后护理（专业照护服务）<br/>
            1. 服务定位：术后康复照护（伤口观察、用药提醒、生活协助等），需专业技能支撑；<br/>
            2. 新增昼夜差异化定价：<br/>
            &nbsp;&nbsp;| 计费模式 | 时段划分 | 定价标准 | 约束规则 |<br/>
            &nbsp;&nbsp;| --- | --- | --- | --- |<br/>
            &nbsp;&nbsp;| 按天计费 | 白天（08:00-20:00） | 180 元 / 天 | 不足 1 天按 1 天计，含基础护理用品 |<br/>
            &nbsp;&nbsp;| 按天计费 | 夜间（20:00-08:00） | 240 元 / 天 | 含夜间起夜照料（≤3 次）、应急响应 |<br/>
            &nbsp;&nbsp;| 按小时计费 | 白天（08:00-20:00） | 45 元 / 小时 | 不足 1 小时按 1 小时计，2 小时起约 |<br/>
            &nbsp;&nbsp;| 按小时计费 | 夜间（20:00-08:00） | 60 元 / 小时 | 夜间最低起约 4 小时，含应急处理 |<br/>
            3. 新增多次卡套餐：<br/>
            &nbsp;&nbsp;| 套餐类型 | 套餐有效期 | 包含服务内容 | 套餐价格（元） | 折合单价 | 比单次预约节省金额 | 额外权益 | 适配场景 |<br/>
            &nbsp;&nbsp;| --- | --- | --- | --- | --- | --- | --- | --- |<br/>
            &nbsp;&nbsp;| 术后体验卡 | 90 天（3 个月） | 10 小时白天护理（可拆分） | 419 | 41.9 元 / 小时 | 31 元（约 7%） | 免费上门评估 1 次、护理指导 1 次 | 轻度术后恢复、短期复查护理 |<br/>
            &nbsp;&nbsp;| 康复进阶卡 | 180 天（6 个月） | 30 小时白天护理（可拆分） | 1259 | 41.97 元 / 小时 | 191 元（约 13%） | 免费上门评估 1 次、康复方案 1 份 | 中度术后康复、定期护理需求 |<br/>
            &nbsp;&nbsp;| 全天照护卡 | 180 天（6 个月） | 5 天白天全天照护（8 小时 / 天） | 1699 | 42.48 元 / 小时 | 161 元（约 9%） | 免费护理用品 1 套、改期 2 次 | 重度术后集中照护、无人照料场景 |<br/>
            &nbsp;&nbsp;| 昼夜组合卡 | 180 天（6 个月） | 15 小时白天 + 5 小时夜间护理 | 1199 | 白天 39.9 元 / 小时、夜间 60 元 / 小时 | 151 元（约 11%） | 免费护理用品 1 套、紧急优先响应 | 术后昼夜交替照护、应急需求 |<br/>
            &nbsp;&nbsp;| 年度尊享卡 | 365 天（1 年） | 80 小时白天护理（可拆分） | 3199 | 39.99 元 / 小时 | 801 元（约 20%） | 免费上门评估 2 次、专属客服对接 | 长期居家术后康复、慢性病术后护理 |<br/>
            4. 核心说明：<br/>
            &nbsp;&nbsp;a. 定价适配性：按小时计费的白天单价（45 元 / 小时）、夜间单价（60 元 / 小时），与普通陪诊 “超时 30 元 / 小时” 形成合理梯度（术后护理含专业技能，溢价 50%-100%，符合市场认知），无明显价格断层；<br/>
            &nbsp;&nbsp;b. 套餐性价比：多次卡折合单价统一在 39.99-42.48 元 / 小时区间，既保持 “多买多省” 优势（最高省 20%），又与普通陪诊（30 元 / 小时）形成合理专业溢价；<br/>
            &nbsp;&nbsp;c. 市场贴合度：夜间溢价 30%-40%，低于一线城市 50% 的溢价水平，兼顾性价比与服务成本，同时支持服务拆分、转赠，提升用户使用灵活性。<br/><br/>

            （三）急诊陪同（紧急响应服务）<br/>
            1. 服务定位：突发疾病急诊就医协助，需快速调配资源；<br/>
            2. 新增昼夜差异化定价：<br/>
            &nbsp;&nbsp;| 费用构成 | 时段划分 | 定价标准 | 说明 |<br/>
            &nbsp;&nbsp;| --- | --- | --- | --- |<br/>
            &nbsp;&nbsp;| 加急费 | 白天（08:00-20:00） | 100 元（固定） | 覆盖紧急调配成本 |<br/>
            &nbsp;&nbsp;| | 夜间（20:00-08:00） | 150 元（固定） | 夜间人力调度难度增加 |<br/>
            &nbsp;&nbsp;| 时长费 | 全天通用 | 同普通陪诊（2 小时起步 50 元，延长 30 元 / 小时） | 无起约时长限制，按实际服务时长计 |<br/>
            3. 示例：夜间急诊陪诊 3 小时，费用 = 150（夜间加急费）+50（2 小时起步）+30（1 小时延长）=230 元；<br/>
            4. 规则说明：急诊服务支持 “2 小时内紧急响应”，夜间服务需提前 1 小时预约，未按时抵达可减免 20% 加急费。<br/><br/>

            （四）上门陪诊（上门服务场景）<br/>
            1. 服务定位：上门问诊协助、取药送药、居家护理指导，覆盖行动不便用户；<br/>
            2. 定价规则（维持原逻辑，补充套餐关联）：<br/>
            &nbsp;&nbsp;a. 里程费：3 公里内免费，超 3 公里后 5 元 / 公里（不足 1 公里按 1 公里计）；<br/>
            &nbsp;&nbsp;b. 时长费：同普通陪诊（2 小时起步 50 元，延长 30 元 / 小时）；<br/>
            &nbsp;&nbsp;c. 套餐适配：普通陪诊多次卡可直接用于上门陪诊，仅需额外支付里程费。<br/>
            3. 费用管理核心补充说明昼夜时段界定：所有服务的昼夜划分以预约服务开始时间为准，跨时段服务按实际时长拆分计费（如 19:00-21:00 服务，1 小时按白天价、1 小时按夜间价）；<br/>
            4. 套餐权益叠加：多次卡用户可同时享受平台优惠活动（如节日满减），但不可与其他套餐叠加使用；<br/>
            5. 动态调整机制：夜间服务定价、多次卡套餐内容可根据用户需求反馈、市场成本变化，通过后台配置灵活调整；<br/>
            6. 透明化展示：订单页实时显示 “基础费 + 时段溢价 + 里程费 - 套餐抵扣” 明细，多次卡用户同步展示剩余次数 / 时长，无隐藏收费。<br/><br/>

            （五）服务进行中取消订单规则<br/>
            1. 核心前提说明：<br/>
            &nbsp;&nbsp;a. 定金定义：所有术后护理订单支付时，需缴纳订单总金额的 20% 作为定金（最低 50 元，最高 200 元），剩余费用服务开始前结清；<br/>
            &nbsp;&nbsp;b. 取消权限：用户可在服务开始前及服务进行中发起取消申请，平台结合 “取消时间阶梯” 与 “取消原因” 双维度判定定金扣除比例；<br/>
            &nbsp;&nbsp;c. 定金用途：覆盖陪诊师交通成本、服务准备成本及资源占用损失，未扣除部分将与剩余服务费一同原路退还。<br/><br/>

            2. 取消时间阶梯与定金扣除基础规则：<br/>
            &nbsp;&nbsp;| 取消时间节点 | 定金扣除比例 | 补充说明 |<br/>
            &nbsp;&nbsp;| --- | --- | --- |<br/>
            &nbsp;&nbsp;| 服务开始前 2 小时以上取消 | 0%（全额退定金） | 支持免费取消，方便用户灵活调整行程，无任何费用损失 |<br/>
            &nbsp;&nbsp;| 服务开始前 1-2 小时内取消 | 20%（退 80% 定金） | 陪诊师已启动服务准备（如路线规划、工具准备），扣除部分成本补偿 |<br/>
            &nbsp;&nbsp;| 服务开始前 30 分钟 - 1 小时内取消 | 30%（退 70% 定金） | 陪诊师可能已出发前往服务地点，扣费弥补行程调整与资源占用损失 |<br/>
            &nbsp;&nbsp;| 服务开始前 30 分钟内取消 | 50%（退 50% 定金） | 临近服务启动，对服务安排影响显著，平衡平台与陪诊师损失 |<br/>
            &nbsp;&nbsp;| 服务开始后取消 | 100%（不退定金） | 陪诊师已提供实质服务，定金全额扣除，剩余服务费按实际未服务时长折算退还 |<br/><br/>

            3. 取消原因分级调整规则（叠加时间阶梯规则）：<br/>
            &nbsp;&nbsp;| 取消原因分类 | 具体取消原因 | 对定金扣除比例的调整方式 | 补充说明 |<br/>
            &nbsp;&nbsp;| --- | --- | --- | --- |<br/>
            &nbsp;&nbsp;| 不可抗因素（用户方） | 1. 突发疾病 / 急诊需紧急就医<br/>2. 直系亲属突发状况需陪同<br/>3. 自然灾害等不可预见情况（需提供证明） | 在时间阶梯比例基础上减免 100%（即全额退定金） | 需上传相关证明（如急诊单、灾害预警），平台 1 小时内审核通过后执行 |<br/>
            &nbsp;&nbsp;| 合理调整（用户方） | 1. 服务时间与个人行程冲突（非主观故意）<br/>2. 对服务内容理解偏差（平台未明确说明）<br/>3. 临时更换就医方案 / 医院 | 按时间阶梯比例执行，无额外调整 | 无需额外证明，平台直接根据原因与时间节点判定 |<br/>
            &nbsp;&nbsp;| 主观违约（用户方） | 1. 单纯不想继续服务<br/>2. 找到其他更便宜的术后护理服务<br/>3. 无正当理由临时取消 | 按时间阶梯比例执行，服务开始后取消额外扣除 10% 服务费 | 定金按规则扣除，用于补偿陪诊师已产生的全部成本 |<br/>
            &nbsp;&nbsp;| 服务违规（平台 / 陪诊师方） | 1. 陪诊师迟到超 30 分钟 未说明<br/>2. 陪诊师服务不专业（未按约定提供护理、态度恶劣）<br/>3. 陪诊师资质不符、临时更换未告知 | 全额退定金 + 双倍定金赔付 | 需上传证据（照片、聊天记录），审核通过后退还全部费用，额外补偿订单总金额 10% 的优惠券 |<br/>
            &nbsp;&nbsp;| 客观条件变更（非双方原因） | 1. 医院临时停诊 / 调整就诊流程<br/>2. 服务地点突发管控无法抵达<br/>3. 其他非双方可控的客观因素 | 在时间阶梯比例基础上减免 100%（即全额退定金） | 可选择全额退款或免费改期，改期无额外费用 |<br/><br/>

            4. 特殊人群与场景优待规则：<br/>
            &nbsp;&nbsp;a. 新用户优待：首次使用平台下单的新用户，在服务开始前 1 小时内取消订单，定金扣除比例减半（如原扣 30%，现扣 15%），降低新用户尝试服务的顾虑；<br/>
            &nbsp;&nbsp;b. 多次卡用户优待：多次卡用户取消订单时，仅按对应规则扣除单次服务的定金，剩余服务次数正常保留，不影响套餐整体有效性；<br/>
            &nbsp;&nbsp;c. 长期套餐用户保障：年度尊享卡、全天照护卡用户，每年可享受 1 次 “服务开始前 30 分钟内免费取消” 权益，需提前 24 小时申请激活。<br/><br/>

            5. 服务取消操作流程：<br/>
            &nbsp;&nbsp;a. 发起申请：用户进入 “订单详情页”，点击底部对应按钮（服务前显示 “取消预约”，服务中显示 “紧急取消”），选择具体取消原因，按要求上传证明材料（如需）；<br/>
            &nbsp;&nbsp;b. 规则告知与二次确认：<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;i. 点击取消按钮后，系统弹出确认弹窗，明确展示 “取消时间节点 + 对应扣除比例 + 最终退款金额”，例如 “您距离服务开始仅剩 25 分钟，取消将扣除 50% 定金，预计退款 XX 元，是否继续？”；<br/>
            &nbsp;&nbsp;&nbsp;&nbsp;ii. 弹窗同时标注规则查询入口，方便用户即时核对详细条款；<br/>
            &nbsp;&nbsp;c. 平台审核：客服 30 分钟内完成审核（需核验证明的场景 1 小时内），审核结果通过 APP 弹窗 + 短信通知用户；<br/>
            &nbsp;&nbsp;d. 费用结算：审核通过后，1-3 个工作日内完成退款（平台托管账户优先结算），退款金额 =（总服务费 - 已服务时长费用）+（定金 - 扣除定金）；<br/>
            &nbsp;&nbsp;e. 服务终止：确认取消后，陪诊师停止后续服务（已开始的护理步骤需完成），并提交服务记录备案。<br/><br/>

            6. 规则透明化展示机制：<br/>
            &nbsp;&nbsp;a. 页面标注：在预约确认页、订单详情页的取消按钮旁，用灰色小字清晰标注核心规则，如 “取消规则：2 小时前免费，1-2 小时扣 20% 定金，30 分钟内扣 50% 定金”；<br/>
            &nbsp;&nbsp;b. 规则查询入口：在平台 “帮助中心”“服务协议” 板块设置专门页面，详细展示完整取消规则、阶梯比例及特殊场景说明，用户可随时查阅；<br/>
            &nbsp;&nbsp;c. 订单通知同步：下单成功后，通过短信 / APP 推送订单确认信息，附带取消规则简要提示，确保用户提前知晓权益与责任。<br/><br/>

            7. 争议处理机制：<br/>
            &nbsp;&nbsp;若用户对取消结果、定金扣除比例有异议，可在审核结果出具后 24 小时内申请平台仲裁，仲裁结果以陪诊师服务记录、用户提交的证据及本规则为准，仲裁期间不影响退款流程推进。<br/>
          </text>
        </scroll-view>
        <view class="modal-footer">
          <button class="close-modal-btn" @click="showBillingRules = false">关闭</button>
        </view>
      </view>
    </view>

    <!-- 支付弹窗 -->
    <view v-if="showPaymentModal" class="payment-modal-overlay" @click="showPaymentModal = false">
      <view class="payment-modal-content" @click.stop>
        <view class="payment-modal-header">
          <text class="payment-modal-title">支付确认</text>
        </view>
        <view class="payment-modal-body">
          <text class="payment-modal-text">请确认您的支付状态：</text>
        </view>
        <view class="payment-modal-footer">
          <button class="payment-modal-btn success-btn" @click="handlePaymentResult(true)">已支付</button>
          <button class="payment-modal-btn fail-btn" @click="handlePaymentResult(false)">未支付</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { get, post, config } from '@/utils/api.js';

const orderData = ref({});
const showInvoice = ref(false);
const payMethod = ref('wechat'); // 默认支付方式
const isLoading = ref(true);
const showBillingRules = ref(false); // 控制模态框显示
const showPaymentModal = ref(false); // 控制支付弹窗显示

// 页面加载时获取 orderNo
const orderNo = ref('');

onLoad((options) => {
  console.log('【OrderConfirmPage】页面加载参数:', options);
  if (options && options.orderNo) {
    orderNo.value = options.orderNo;
    console.log('【OrderConfirmPage】订单号:', orderNo.value);
    fetchOrderDetail(orderNo.value);
  } else {
    console.error('【OrderConfirmPage】缺少订单号参数');
    uni.showToast({
      title: '订单号错误',
      icon: 'none'
    });
    uni.redirectTo({
      url: '/pages/index/index'
    });
  }
});

/**
 * 调用后端接口获取订单详情
 * @param {string} orderNo - 订单号
 */
const fetchOrderDetail = async (orderNo) => {
  isLoading.value = true;
  try {
    console.log('【OrderConfirmPage】正在调用后端接口获取订单详情:', orderNo);
    const response = await get(`/ai/guide/orders/${orderNo}/complete-info`);

    console.log('【OrderConfirmPage】后端返回的订单数据:', response);

    if (response && response.code === 200 && response.data) {
      orderData.value = response.data;
    } else {
      console.warn('【OrderConfirmPage】后端返回数据为空或格式不正确:', response);
      uni.showToast({
        title: '获取订单信息失败',
        icon: 'none'
      });
    }
  } catch (error) {
    console.error('【OrderConfirmPage】获取订单详情失败:', error);
    uni.showToast({
      title: '网络错误，获取订单信息失败',
      icon: 'none'
    });
  } finally {
    isLoading.value = false;
  }
};

/**
 * 处理支付方式变更
 */
const onPaymentChange = (e) => {
  payMethod.value = e.detail.value;
};

/**
 * 确认支付 (触发弹窗)
 */
const confirmPay = () => {
  showPaymentModal.value = true;
};

/**
 * 处理支付结果
 */
const handlePaymentResult = async (isPaid) => {
  showPaymentModal.value = false;

  const paymentStatus = isPaid ? 1 : 0;

  try {
    // 无论成功或失败，都先通知后端
    const response = await post('/ai/guide/payments/status', {
      orderNo: orderNo.value,
      paymentStatus: paymentStatus
    });

    console.log('【OrderConfirmPage】支付状态更新响应:', response);

    if (isPaid) {
      uni.showToast({ title: '支付成功，跳转中...', icon: 'none' });
      setTimeout(() => {
        uni.navigateTo({
          url: `/pages/AItriage/05_PaymentSuccessPage?orderNo=${encodeURIComponent(orderNo.value)}`
        });
      }, 1000);
    } else {
      uni.showToast({ title: '支付未完成', icon: 'none' });
      setTimeout(() => {
        uni.navigateTo({
          url: `/pages/AItriage/PaymentFailedPage?orderNo=${encodeURIComponent(orderNo.value)}`
        });
      }, 1000);
    }
  } catch (error) {
    console.error('【调用支付状态接口失败】', error);
    // 即使接口报错，也根据用户点击的结果进行跳转
    if (isPaid) {
        uni.navigateTo({
          url: `/pages/AItriage/05_PaymentSuccessPage?orderNo=${encodeURIComponent(orderNo.value)}`
        });
    } else {
        uni.navigateTo({
          url: `/pages/AItriage/PaymentFailedPage?orderNo=${encodeURIComponent(orderNo.value)}`
        });
    }
  }
};


/**
 * 格式化金额 (元)
 */
const formatAmount = (amount) => {
  const num = Number(amount);
  if (isNaN(num)) {
    return '0.00';
  }
  return num.toFixed(2);
};


/**
 * 获取症状描述文本
 */
const getSymptomDescription = () => {
  const { symptoms } = orderData.value;
  if (!symptoms) {
    return '未提供症状信息';
  }
  if (Array.isArray(symptoms)) {
    const validSymptoms = symptoms.filter(s => s && s.trim() && s !== '无' && s !== 'null');
    return validSymptoms.length > 0 ? validSymptoms.join(', ') : '未提供症状信息';
  }
  return symptoms;
};

/**
 * 格式化日期
 */
const formatDate = (dateStr) => {
  if (!dateStr || typeof dateStr !== 'string') return '未知';
  return dateStr; // 后端已返回格式化好的日期
};

</script>

<style scoped>
/* 保持原有样式不变 */
.order-confirm-page {
  background-color: #f5f5f5;
  padding: 40rpx;
  font-size: 28rpx;
  min-height: 100vh;
}

.loading-container {
  text-align: center;
  padding: 40rpx;
  color: #999;
}

.back-btn {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
  color: #333;
  font-size: 32rpx;
}

.step-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40px;
  padding: 0 20rpx;
  position: relative;
  align-items: center;
}

.step-dot {
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  border-radius: 50%;
  color: white;
  font-size: 24rpx;
  text-align: center;
  margin-bottom: 10rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
}

.step-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 28rpx;
  color: #333;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 10%;
  margin: 0 10px -25px 10px;
  text-align: center;
  font-size: 28rpx;
}

.step-item.completed .step-dot {
  background-color: #4caf50;
  color: white;
}

.step-item.active .step-dot {
  background-color: #007aff;
  color: white;
}

.step-item.completed .step-text {
  color: #4caf50;
}

.step-item.active .step-text {
  color: #007aff;
}

.progress-line {
  width: 60rpx;
  height: 2rpx;
  background-color: #ddd;
  margin: 8px 10rpx;
  flex-shrink: 0;
}

.progress-line.green {
  background-color: #4caf50;
}

.card {
  background-color: white;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 40px;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.title {
  font-weight: bold;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title .iconfont {
  margin-right: 10rpx;
  color: #007aff;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  font-size: 24rpx;
  color: #666;
  font-weight: 800;
}

.value {
  color: #333;
  font-weight: 500;
  font-size: 24rpx;
  text-align: right;
  max-width: 60%;
  word-break: break-all;
}

.fee-item {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #eee;
  font-size: 24rpx;
}

.price {
  color: #333;
  font-weight: 500;
}

.discount {
  color: #007aff;
  font-weight: 500;
}

.total {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  font-weight: bold;
  color: #007aff;
}

.total-price {
  font-size: 36rpx;
}

.payment-option {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #eee;
}

.pay-icon {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 24rpx;
}

.pay-icon .iconfont {
  margin-right: 10rpx;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx;
  background-color: white;
  border-radius: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  margin-top: 20rpx;
  position: sticky;
  bottom: 0;
  z-index: 10;
}

.real-price {
  font-size: 28rpx;
  color: #333;
}

.price {
  color: #007aff;
  font-weight: bold;
  font-size: 32rpx;
}

.confirm-btn {
  width: 200rpx;
  height: 60rpx;
  background-color: #007aff;
  color: white;
  border-radius: 30rpx;
  font-size: 28rpx;
  line-height: 60rpx;
  margin-right: -5px;
  border: none;
}

.rule-link-button {
  display: inline-block;
  margin-left: auto;
  padding: 0;
  border: none;
  background: none;
  color: #007aff;
  font-size: 24rpx;
  text-decoration: underline;
  cursor: pointer;
  line-height: 1;
  margin-top: -10rpx;
  margin-right: 10rpx;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20rpx;
}

.modal-content {
  background-color: white;
  border-radius: 16rpx;
  width: 90%;
  max-width: 600rpx;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid #eee;
  background-color: #f8f9fa;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.close-btn {
  font-size: 40rpx;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0 10rpx;
}

.modal-body {
  flex: 1;
  padding: 20rpx;
  overflow-y: auto;
  font-size: 24rpx;
  line-height: 1.5;
  color: #333;
}

.rule-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.modal-footer {
  padding: 20rpx;
  border-top: 1rpx solid #eee;
  display: flex;
  justify-content: center;
}

.close-modal-btn {
  padding: 10rpx 30rpx;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 8rpx;
  font-size: 28rpx;
  cursor: pointer;
}

.payment-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20rpx;
}

.payment-modal-content {
  background-color: white;
  border-radius: 16rpx;
  width: 90%;
  max-width: 500rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.payment-modal-header {
  padding: 20rpx;
  border-bottom: 1rpx solid #eee;
  background-color: #f8f9fa;
  text-align: center;
}

.payment-modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.payment-modal-body {
  padding: 40rpx 20rpx;
  text-align: center;
}

.payment-modal-text {
  font-size: 28rpx;
  color: #333;
}

.payment-modal-footer {
  display: flex;
  justify-content: space-around;
  padding: 20rpx;
  border-top: 1rpx solid #eee;
}

.payment-modal-btn {
  padding: 16rpx 30rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  cursor: pointer;
  border: none;
  flex: 1;
  margin: 0 10rpx;
}

.success-btn {
  background-color: #4caf50;
  color: white;
}

.fail-btn {
  background-color: #ff5252;
  color: white;
}
</style>