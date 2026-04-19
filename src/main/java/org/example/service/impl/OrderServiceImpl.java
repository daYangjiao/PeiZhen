package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.dao.ChatMessageMapper;
import org.example.dao.OrderMapper;
import org.example.dao.UserMapper;
import org.example.handler.ChatWebSocketHandler;
import org.example.handler.OrderWebSocketHandler;
import org.example.model.ChatMessage;
import org.example.model.Order;
import org.example.model.User;
import org.example.model.request.OrderListQueryRequest;
import org.example.model.response.OrderListResponse;
import org.example.model.response.PagedResponse;
import org.example.service.OrderService;
import org.example.util.OrderTimeoutCloseUtils;
import org.example.unity.ServiceFeeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private OrderWebSocketHandler webSocketHandler;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    @Transactional
    public int createOrder(Order order) {
        order.setCreateTime(new Date());
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(0);
        }
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus(0);
        }
        return orderMapper.insert(order);
    }

    @Override
    public Order getOrderById(Integer orderId) {
        return orderMapper.selectByPrimaryKey(orderId);
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return null;
        }
        return orderMapper.selectByOrderNo(orderNo.trim());
    }

    @Override
    public int updateOrder(Order order) {
        return orderMapper.updateByPrimaryKeySelective(order);
    }

    @Override
    @Transactional
    public String attendantAcceptOrder(Integer orderId, Integer attendantId) {
        log.info("开始处理陪诊师接单请求 - 订单ID: {}, 陪诊师ID: {}", orderId, attendantId);
        
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            log.warn("接单失败：订单不存在，订单ID: {}", orderId);
            return "订单不存在";
        }
        
        log.info("订单当前状态: {}, 订单号: {}", order.getOrderStatus(), order.getOrderNo());
        
        boolean publicWaitingOrder = order.getOrderStatus() != null && order.getOrderStatus() == 1;
        boolean assignedWaitingOrder = order.getOrderStatus() != null
                && order.getOrderStatus() == 8
                && order.getAttendantId() != null
                && order.getAttendantId().equals(attendantId);
        if (!publicWaitingOrder && !assignedWaitingOrder) {
            log.warn("接单失败：订单状态不允许接单，当前状态: {}", order.getOrderStatus());
            return "订单当前状态无法接单";
        }

        // 验证陪诊师ID是否有效
        // 注意：这里传入的 attendantId 应该是 user 表中的 id，而不是 attendant 表中的 id
        // 因为 AttendantController 中调用时传入的是 attendantInfo.id (即 user.id)
        User attendantUser = userMapper.findById(attendantId);
        if (attendantUser == null) {
            log.warn("接单失败：陪诊师用户不存在，用户ID: {}", attendantId);
            return "无效的陪诊师ID";
        }
        
        log.info("陪诊师用户信息 - ID: {}, Name: {}, UserType: {}", 
                attendantUser.getId(), attendantUser.getName(), attendantUser.getUserType());
        
        // 检查用户类型是否为陪诊师 (user_type = 1)
        // 临时放宽验证：允许 user_type = 0 或 1 的用户接单，便于测试
        if (attendantUser.getUserType() != 0 && attendantUser.getUserType() != 1) {
            log.warn("接单用户类型异常: ID={}, Type={}", attendantId, attendantUser.getUserType());
            return "用户类型不支持接单";
        }
        
        // 如果是普通用户类型，记录警告但允许接单（测试环境）
        if (attendantUser.getUserType() == 0) {
            log.warn("普通用户尝试接单，允许测试: ID={}, Name={}", attendantId, attendantUser.getName());
        }

        // 先检查订单是否已经被其他陪诊师接单
        if (publicWaitingOrder && order.getAttendantId() != null) {
            User existingAttendant = userMapper.findById(order.getAttendantId());
            if (existingAttendant != null) {
                log.warn("订单已被接单，接单人: {}", existingAttendant.getName());
                return "订单已被其他陪诊师接单";
            }
        }

        // 所有验证通过后才更新订单状态
        order.setAttendantId(attendantId);
        order.setAttendantName(attendantUser.getName());
        order.setOrderStatus(2); // 设置为已接单状态
        order.setAcceptTime(new Date());

        String qrCodeUrl = generateQrCodeUrl(orderId);
        order.setQrCodeUrl(qrCodeUrl);

        // 更新数据库
        int updateResult = orderMapper.updateByPrimaryKeySelective(order);
        if (updateResult <= 0) {
            log.error("更新订单状态失败，订单ID: {}", orderId);
            return "系统繁忙，请稍后重试";
        }

        log.info("订单状态更新成功 - 订单ID: {}, 新状态: {}, 陪诊师: {}", 
                orderId, order.getOrderStatus(), attendantUser.getName());

        // 只有在数据库更新成功后才发送通知
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);
        broadcastWaitingOrderUpdate(order);

        try {
            // 插入系统消息（serviceDate 为 String 如 "2026-02-22"，需解析后格式化）
            String dateStr = "";
            if (order.getServiceDate() != null && !order.getServiceDate().isEmpty()) {
try {
                dateStr = new SimpleDateFormat("MM月dd日").format(new SimpleDateFormat("yyyy-MM-dd").parse(order.getServiceDate().trim()));
            } catch (Exception parseEx) {
                    dateStr = order.getServiceDate();
                }
            }
            String msgContent = "您预约的(" + dateStr + ")" + (order.getServiceTimeSlot() != null ? order.getServiceTimeSlot() : "") + "有" + (order.getHospital() != null ? order.getHospital() : "") + "的就诊安排，陪诊师" + attendantUser.getName() + "已接单。请携带身份证、医保卡及相关检查报告。";
            sendSystemMessage(order.getUserId(), msgContent, order.getOrderId());
            
            log.info("已向用户 {} 发送系统消息", order.getUserId());
        } catch (Exception e) {
            log.error("发送系统消息失败，但不影响主流程", e);
        }

        try {
            // 陪诊师自动发送问候消息
            ChatMessage greetingMsg = new ChatMessage();
            greetingMsg.setSenderId(attendantId);
            greetingMsg.setReceiverId(order.getUserId());
            greetingMsg.setOrderId(order.getOrderId());
            greetingMsg.setContent("您好！我是陪诊师" + attendantUser.getName() + "，很高兴为您服务。我会尽快与您联系确认服务细节。");
            greetingMsg.setMsgType(1);
            greetingMsg.setIsRead(false);
            greetingMsg.setCreateTime(new Date());
            chatMessageMapper.insert(greetingMsg);

            // 通过 WebSocket 推送给用户
            chatWebSocketHandler.sendMessageToUser(order.getUserId(), greetingMsg);
            
            log.info("已向用户 {} 发送问候消息", order.getUserId());
        } catch (Exception e) {
            log.error("发送问候消息失败，但不影响主流程", e);
        }

        log.info("陪诊师 {} 接单成功并发送所有通知，订单号: {}", attendantUser.getName(), order.getOrderNo());
        return "接单成功";
    }

    @Override
    @Transactional
    public String rejectAssignedOrder(Integer orderId, Integer attendantId, String reason) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            return "订单不存在";
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != 8) {
            return "当前订单不是待确认的专属派单";
        }
        if (order.getAttendantId() == null || !order.getAttendantId().equals(attendantId)) {
            return "无权操作该专属订单";
        }

        String releaseReason = reason != null && !reason.trim().isEmpty()
                ? reason.trim()
                : "指定陪诊师暂未接单，订单已转入公共派单";
        Date now = new Date();
        int rows = orderMapper.releaseOrderBackToHall(orderId, releaseReason, now);
        if (rows <= 0) {
            return "系统繁忙，请稍后重试";
        }

        order.setOrderStatus(1);
        order.setAttendantId(null);
        order.setAttendantName(null);
        order.setAcceptTime(null);
        order.setQrCodeUrl(null);
        order.setCancelReason(releaseReason);
        order.setCancelTime(now);
        order.setCancelBy(1);

        notifyOrderParties(
                order,
                "您指定的陪诊师暂未接单，订单已进入公共派单大厅，我们会继续为您匹配服务。",
                null
        );
        publishOrderEvent(order, "ORDER_RELEASED_TO_HALL", releaseReason, null, true, false);
        broadcastWaitingOrderUpdate(order);
        return "订单已释放回接单大厅";
    }

    @Override
    @Transactional
    public String closeTimedOutUnmatchedOrder(Integer orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            return "订单不存在";
        }
        if (order.getPaymentStatus() == null || order.getPaymentStatus() != 1) {
            return "订单尚未支付";
        }
        if (order.getOrderStatus() == null || (order.getOrderStatus() != 1 && order.getOrderStatus() != 8)) {
            return "当前订单无需超时关闭";
        }

        Date appointmentStart = parseAppointmentStartTime(order.getServiceDate(), order.getServiceTimeSlot());
        Date now = new Date();
        if (appointmentStart != null && now.before(appointmentStart)) {
            return "服务时间未开始";
        }

        String timeoutMessage = OrderTimeoutCloseUtils.buildTimeoutCloseMessage(order);
        BigDecimal refundAmount = order.getOrderAmount() == null ? BigDecimal.ZERO : order.getOrderAmount();
        Integer previousAttendantId = order.getAttendantId();
        String previousAttendantName = order.getAttendantName();

        int rows = orderMapper.closeOrderAsTimeout(orderId, timeoutMessage, now, refundAmount);
        if (rows <= 0) {
            return "订单状态已更新";
        }

        order.setOrderStatus(7);
        order.setCancelReason(timeoutMessage);
        order.setCancelTime(now);
        order.setCancelBy(2);
        order.setPenaltyRate(BigDecimal.ZERO);
        order.setPenaltyAmount(BigDecimal.ZERO);
        order.setRefundAmount(refundAmount);
        order.setQrCodeUrl(null);
        order.setAcceptTime(null);
        order.setAttendantId(previousAttendantId);
        order.setAttendantName(previousAttendantName);

        String attendantMessage = previousAttendantId != null
                ? "订单 " + (order.getOrderNo() != null ? order.getOrderNo() : "") + " 已因服务开始前仍未完成接单而自动关闭。"
                : null;
        notifyOrderParties(order, timeoutMessage, attendantMessage);
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", timeoutMessage, null, true, previousAttendantId != null);

        order.setAttendantId(null);
        order.setAttendantName(null);
        broadcastWaitingOrderUpdate(order);
        return "订单已超时关闭";
    }

    private String generateQrCodeUrl(Integer orderId) {
        return "/order-qr/" + orderId + ".png";
    }

    @Override
    @Transactional
    public String startService(Integer orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() != 2) return "订单当前状态无法开始服务";

        order.setOrderStatus(3);
        order.setServiceStartTime(new Date());
        orderMapper.updateByPrimaryKeySelective(order);

        // 发送 WebSocket 消息
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);

        // 插入系统消息（用户）
        String msgContent = "您的订单No." + order.getOrderNo() + "服务已开始。陪诊师已到达指定位置，请准备就诊。";
        sendSystemMessage(order.getUserId(), msgContent, order.getOrderId());

        // 插入系统消息（陪诊师）
        sendSystemMessage(order.getAttendantId(), "您已开始为订单 " + order.getOrderNo() + " 提供服务，请按时完成服务。", order.getOrderId());

        return "服务开始成功";
    }

    @Override
    @Transactional
    public String endService(Integer orderId, BigDecimal actualDuration) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() == null || order.getOrderStatus() != 3) return "订单当前状态无法结束服务";

        // 记录结束时间与实际时长
        order.setServiceEndTime(new Date());
        order.setActualDuration(actualDuration);

        // 若预估时长为空，尝试从 consultationDuration 或 service_time_slot 推导
        if (order.getEstimatedDuration() == null) {
            BigDecimal estimated = null;
            if (order.getConsultationDuration() != null) {
                estimated = order.getConsultationDuration();
            } else if (order.getServiceTimeSlot() != null) {
                try {
                    String slot = order.getServiceTimeSlot();
                    String[] parts = slot.split("-");
                    if (parts.length == 2) {
                        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
                        Date start = fmt.parse(parts[0].trim());
                        Date end = fmt.parse(parts[1].trim());
                        long minutes = (end.getTime() - start.getTime()) / 60000;
                        if (minutes < 0) {
                            minutes += 24 * 60;
                        }
                        if (minutes > 0) {
                            estimated = BigDecimal.valueOf(minutes / 60.0).setScale(1, RoundingMode.HALF_UP);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析 service_time_slot 计算预估时长失败, slot={}", order.getServiceTimeSlot(), e);
                }
            }
            order.setEstimatedDuration(estimated);
        }

        // 计算基于实际时长的应收费用（不改变原 orderAmount，先存差额）
        BigDecimal balance = BigDecimal.ZERO;
        try {
            int serviceType = order.getClinicType() != null ? order.getClinicType() : 1;
            ServiceFeeCalculator.FeeCalculationResult feeResult =
                    ServiceFeeCalculator.calculateFee(serviceType, actualDuration.doubleValue(), false);
            BigDecimal expectedTotal = feeResult.getTotalFee();
            BigDecimal originalAmount = order.getOrderAmount() != null ? order.getOrderAmount() : BigDecimal.ZERO;
            balance = expectedTotal.subtract(originalAmount);
            // 四舍五入到两位
            balance = balance.setScale(2, RoundingMode.HALF_UP);
            order.setBalanceAmount(balance);
        } catch (Exception e) {
            log.error("按实际时长计算费用失败，orderId={}", orderId, e);
        }

        // 状态改为：4=待确认时长费用
        order.setOrderStatus(4);
        orderMapper.updateByPrimaryKeySelective(order);

        // 通知用户：服务已结束，请确认时长和费用
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);

        String msgContent = "您的陪诊服务(订单No." + order.getOrderNo() + ")已结束，请确认本次服务时长和费用（多退少补）。";
        sendSystemMessage(order.getUserId(), msgContent, order.getOrderId());
        sendSystemMessage(order.getAttendantId(), "您已结束订单 " + order.getOrderNo() + " 的服务，请提醒用户确认时长与费用。", order.getOrderId());

        return "服务结束成功，待用户确认时长费用";
    }

    @Override
    @Transactional
    public String updateServiceProgress(Integer orderId, Integer step) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() == null || order.getOrderStatus() != 3) {
            return "仅服务中状态可更新服务进度";
        }
        if (step == null || step < 1 || step > 4) {
            return "无效的服务进度步骤";
        }
        order.setServiceProgressStep(step);
        orderMapper.updateByPrimaryKeySelective(order);

        // 通过 WebSocket 通知用户和陪诊师，便于前端实时刷新
        publishOrderEvent(order, "SERVICE_PROGRESS_UPDATED", null, step, true, true);

        return "服务进度已更新";
    }

    @Override
    @Transactional
    public String userConfirmTimeAndFee(Integer orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() == null || order.getOrderStatus() != 4) {
            return "当前状态不允许确认时长费用";
        }

        BigDecimal originalAmount = order.getOrderAmount() != null ? order.getOrderAmount() : BigDecimal.ZERO;
        BigDecimal balance = order.getBalanceAmount() != null ? order.getBalanceAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = originalAmount.add(balance).setScale(2, RoundingMode.HALF_UP);

        // 更新订单金额与状态
        order.setOrderAmount(finalAmount);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            // 退款场景：记录退款金额（正数）
            order.setRefundAmount(balance.abs());
        }
        order.setOrderStatus(6);
        orderMapper.updateByPrimaryKeySelective(order);

        // 消息通知
        sendSystemMessage(order.getUserId(), "您已确认本次陪诊服务时长与费用，订单已完成。", order.getOrderId());
        sendSystemMessage(order.getAttendantId(), "用户已确认订单 " + order.getOrderNo() + " 的时长与费用，订单已完成。", order.getOrderId());

        // 通过 WebSocket 推送订单状态变更（方便前端实时刷新列表和详情）
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);

        return "确认成功，订单已完成";
    }

    @Override
    @Transactional
    public String userDisputeTimeAndFee(Integer orderId, BigDecimal userDuration, String reason) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() == null || order.getOrderStatus() != 4) {
            return "当前状态不允许发起申诉";
        }

        order.setTimeDisputeUserDuration(userDuration);
        order.setTimeDisputeReason(reason);
        order.setOrderStatus(5); // 时长费用有争议
        orderMapper.updateByPrimaryKeySelective(order);

        sendSystemMessage(order.getAttendantId(),
                "用户对订单 " + order.getOrderNo() + " 的服务时长与费用提出异议，请关注平台处理结果。");

        // WebSocket 推送争议状态，前端可实时更新为“时长费用有争议”
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);

        return "申诉已提交，等待平台处理";
    }

    @Override
    @Transactional
    public String attendantCancelOrder(Integer orderId, String reason, BigDecimal penaltyAmount,
                                       BigDecimal refundAmount, BigDecimal penaltyRate) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() == null || order.getOrderStatus() != 2) {
            return "仅待服务状态可取消订单";
        }
        if (reason == null || reason.trim().isEmpty()) return "请输入取消原因";

        Date appointmentStart = parseAppointmentStartTime(order.getServiceDate(), order.getServiceTimeSlot());
        Date now = new Date();
        boolean beforeStart = (appointmentStart != null && now.before(appointmentStart));

        if (beforeStart) {
            // 预约开始前：释放回接单大厅，不扣违约金，通知用户
            int rows = orderMapper.releaseOrderBackToHall(orderId, reason.trim(), now);
            if (rows <= 0) {
                log.error("释放订单回接单大厅失败，orderId={}", orderId);
                return "操作失败，请重试";
            }
            order.setOrderStatus(1);
            notifyOrderParties(
                    order,
                    "您的订单" + order.getOrderNo() + "因陪诊师取消已重新进入接单大厅，将为您重新匹配陪诊师。取消原因：" + reason.trim(),
                    "您已取消订单 " + order.getOrderNo() + "，订单已重新进入接单大厅。"
            );
            publishOrderEvent(order, "ORDER_RELEASED_BY_ATTENDANT", reason.trim(), null, true, true);
            broadcastWaitingOrderUpdate(order);
            return "订单已释放回接单大厅，将重新为您匹配合诊师";
        }

        // 预约开始后或无法解析时间：按已取消处理，扣违约金
        BigDecimal orderAmount = order.getOrderAmount() == null ? BigDecimal.ZERO : order.getOrderAmount();
        BigDecimal finalRate = penaltyRate != null ? penaltyRate : BigDecimal.ZERO;
        BigDecimal finalPenalty = penaltyAmount != null ? penaltyAmount : orderAmount.multiply(finalRate);
        BigDecimal finalRefund = refundAmount != null ? refundAmount : orderAmount;
        order.setOrderStatus(7);
        order.setCancelReason(reason.trim());
        order.setCancelTime(now);
        order.setCancelBy(1);
        order.setPenaltyRate(finalRate);
        order.setPenaltyAmount(finalPenalty);
        order.setRefundAmount(finalRefund);
        orderMapper.updateByPrimaryKeySelective(order);
        notifyOrderParties(
                order,
                "您的订单" + order.getOrderNo() + "已取消。取消原因：" + reason.trim(),
                "订单 " + order.getOrderNo() + " 已取消。取消原因：" + reason.trim()
        );
        publishOrderEvent(order, "ORDER_STATUS_CHANGED", null, null, true, true);
        return "订单已取消";
    }

    @Override
    public void notifyUserOrderCancelled(Order order) {
        String reason = order.getCancelReason() != null ? order.getCancelReason() : "订单已取消";
        notifyOrderParties(order,
                "您的订单" + (order.getOrderNo() != null ? order.getOrderNo() : "") + "已取消。取消原因：" + reason,
                null);
    }

    @Override
    public void notifyOrderParties(Order order, String userMessage, String attendantMessage) {
        if (order == null) return;
        if (order.getUserId() != null && userMessage != null && !userMessage.trim().isEmpty()) {
            sendSystemMessage(order.getUserId(), userMessage.trim(), order.getOrderId());
        }
        if (order.getAttendantId() != null && attendantMessage != null && !attendantMessage.trim().isEmpty()) {
            sendSystemMessage(order.getAttendantId(), attendantMessage.trim(), order.getOrderId());
        }
    }

    @Override
    public void publishOrderEvent(Order order, String eventType, String reason, Integer step,
                                  boolean notifyUser, boolean notifyAttendant) {
        if (order == null || eventType == null || eventType.trim().isEmpty()) return;
        String wsMsg = buildOrderEventPayload(order, eventType.trim(), reason, step);
        try {
            if (notifyUser && order.getUserId() != null) {
                webSocketHandler.sendMessageToUser(String.valueOf(order.getUserId()), wsMsg);
            }
            if (notifyAttendant && order.getAttendantId() != null) {
                webSocketHandler.sendMessageToUser(String.valueOf(order.getAttendantId()), wsMsg);
            }
        } catch (Exception e) {
            log.error("发送订单实时事件失败, orderId={}, type={}", order.getOrderId(), eventType, e);
        }
    }

    @Override
    public void broadcastWaitingOrderUpdate(Order order) {
        if (order == null) return;
        try {
            String wsMsg = buildOrderEventPayload(order, "WAITING_ORDER_UPDATED", null, null);
            webSocketHandler.broadcastMessage(wsMsg);
        } catch (Exception e) {
            log.error("广播待接单大厅事件失败, orderId={}", order.getOrderId(), e);
        }
    }

    /**
     * 解析预约开始时间：serviceDate 如 "2026-02-22"，serviceTimeSlot 如 "08:00-10:30" 或 "08:00"
     */
    private Date parseAppointmentStartTime(String serviceDate, String serviceTimeSlot) {
        if (serviceDate == null || serviceDate.trim().isEmpty()) return null;
        String timePart = "00:00";
        if (serviceTimeSlot != null && !serviceTimeSlot.trim().isEmpty()) {
            String slot = serviceTimeSlot.trim();
            int dash = slot.indexOf('-');
            String first = dash > 0 ? slot.substring(0, dash).trim() : slot;
            if (Pattern.compile("\\d{1,2}:\\d{2}").matcher(first).find()) {
                timePart = first.length() >= 5 ? first.substring(0, 5) : first;
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return sdf.parse(serviceDate.trim() + " " + timePart);
        } catch (Exception e) {
            log.warn("解析预约开始时间失败: serviceDate={}, serviceTimeSlot={}", serviceDate, serviceTimeSlot, e);
            return null;
        }
    }

    // 辅助方法：发送系统消息（写入 DB 并 WebSocket 推送，用户端可实时收到未读提示）
    private void sendSystemMessage(Integer receiverId, String content, Integer orderId) {
        try {
            ChatMessage sysMsg = new ChatMessage();
            sysMsg.setSenderId(0);
            sysMsg.setReceiverId(receiverId);
            sysMsg.setContent(content);
            sysMsg.setOrderId(orderId);
            sysMsg.setMsgType(1);
            sysMsg.setIsRead(false);
            sysMsg.setCreateTime(new Date());
            chatMessageMapper.insert(sysMsg);
            chatWebSocketHandler.sendMessageToUser(receiverId, sysMsg);
        } catch (Exception e) {
            log.error("发送系统消息失败", e);
        }
    }

    private void sendSystemMessage(Integer receiverId, String content) {
        try {
            ChatMessage sysMsg = new ChatMessage();
            sysMsg.setSenderId(0); // 0 代表系统
            sysMsg.setReceiverId(receiverId);
            sysMsg.setContent(content);
            sysMsg.setMsgType(1); // 文本
            sysMsg.setIsRead(false);
            sysMsg.setCreateTime(new Date());
            chatMessageMapper.insert(sysMsg);
            chatWebSocketHandler.sendMessageToUser(receiverId, sysMsg);
        } catch (Exception e) {
            log.error("发送系统消息失败", e);
        }
    }

    private String buildOrderEventPayload(Order order, String eventType, String reason, Integer step) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\":\"").append(escapeJson(eventType)).append("\"");
        if (order.getOrderId() != null) {
            builder.append(",\"orderId\":").append(order.getOrderId());
        }
        if (order.getOrderNo() != null && !order.getOrderNo().trim().isEmpty()) {
            builder.append(",\"orderNo\":\"").append(escapeJson(order.getOrderNo())).append("\"");
        }
        if (order.getOrderStatus() != null) {
            builder.append(",\"orderStatus\":").append(order.getOrderStatus());
        }
        if (step != null) {
            builder.append(",\"step\":").append(step);
        }
        if (reason != null && !reason.trim().isEmpty()) {
            builder.append(",\"reason\":\"").append(escapeJson(reason.trim())).append("\"");
        }
        builder.append("}");
        return builder.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    @Override
    public PagedResponse<OrderListResponse> getUserOrdersWithPagination(Integer userId, OrderListQueryRequest queryRequest) {
        int totalCount = orderMapper.countUserOrders(userId, queryRequest);
        int page = queryRequest.getPage() != null ? queryRequest.getPage() : 0;
        int size = queryRequest.getSize() != null ? queryRequest.getSize() : 10;
        int offset = page * size;

        List<Order> orders = orderMapper.findUserOrdersWithPagination(userId, queryRequest, offset, size);
        List<OrderListResponse> responses = convertToOrderListResponses(orders);

        return new PagedResponse<>(responses, totalCount, page, size);
    }

    @Override
    public PagedResponse<OrderListResponse> getAllOrdersWithPagination(OrderListQueryRequest queryRequest) {
        int totalCount = orderMapper.countAllOrders(queryRequest);
        int page = queryRequest.getPage() != null ? queryRequest.getPage() : 0;
        int size = queryRequest.getSize() != null ? queryRequest.getSize() : 10;
        int offset = page * size;

        List<Order> orders = orderMapper.findAllOrdersWithPagination(queryRequest, offset, size);
        List<OrderListResponse> responses = convertToOrderListResponses(orders);

        return new PagedResponse<>(responses, totalCount, page, size);
    }

    @Override
    public List<Order> findAllOrders() {
        return orderMapper.findAllOrdersWithPagination(new OrderListQueryRequest(), 0, 1000);
    }

    private List<OrderListResponse> convertToOrderListResponses(List<Order> orders) {
        List<OrderListResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            OrderListResponse res = new OrderListResponse();
            res.setOrderId(order.getOrderId());
            res.setOrderNo(order.getOrderNo());
            res.setHospital(order.getHospital());
            res.setPatientName(order.getPatientName());
            res.setPatientAge(order.getPatientAge());
            res.setPatientSex(order.getPatientSex());
            res.setServiceDate(order.getServiceDate());
            res.setServiceTimeSlot(order.getServiceTimeSlot());
            res.setOrderAmount(order.getOrderAmount());
            res.setOrderStatus(order.getOrderStatus());
            res.setOrderStatusDesc(OrderTimeoutCloseUtils.resolveOrderStatusText(order));
            res.setPaymentStatus(order.getPaymentStatus());
            res.setPaymentStatusDesc(order.getPaymentStatus() != null && order.getPaymentStatus() == 1 ? "已支付" : "待支付");
            res.setServiceTypeName(order.getServiceContent());
            res.setCreateTime(order.getCreateTime());
            res.setAcceptTime(order.getAcceptTime());
            res.setActualDuration(order.getActualDuration());
            res.setSpecialRequirements(order.getSpecialRequirements());
            res.setCustomRequirement(order.getCustomRequirement());

            if (order.getAttendantId() != null) {
                User attendantUser = userMapper.findById(order.getAttendantId());
                if (attendantUser != null) {
                    res.setAttendantName(attendantUser.getName());
                    res.setAttendantAvatar(attendantUser.getAvatar());
                }
            }
            responses.add(res);
        }
        return responses;
    }
}
