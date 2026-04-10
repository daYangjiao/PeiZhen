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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        
        if (order.getOrderStatus() != 1) {
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
        if (order.getAttendantId() != null) {
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
        try {
            // 发送 WebSocket 消息给用户
            String orderAcceptedMessage = String.format("{\"type\":\"ORDER_ACCEPTED\",\"orderId\":%d,\"attendantName\":\"%s\"}",
                                          orderId, attendantUser.getName());
            webSocketHandler.sendMessageToUser(String.valueOf(order.getUserId()), orderAcceptedMessage);
            
            log.info("已向用户 {} 发送接单通知", order.getUserId());
        } catch (Exception e) {
            log.error("发送订单状态 WebSocket 消息失败，但不影响主流程", e);
            // WebSocket发送失败不应该影响主流程，继续执行
        }

        try {
            // 插入系统消息
            // 格式：您明天(06月15日)上午10:00有XX医院的就诊安排...
            SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日");
            String dateStr = "";
            if (order.getServiceDate() != null) {
                dateStr = sdf.format(order.getServiceDate());
            }
            String msgContent = "您预约的(" + dateStr + ")" + order.getServiceTimeSlot() + "有" + order.getHospital() + "的就诊安排，陪诊师" + attendantUser.getName() + "已接单。请携带身份证、医保卡及相关检查报告。";
            sendSystemMessage(order.getUserId(), msgContent);
            
            log.info("已向用户 {} 发送系统消息", order.getUserId());
        } catch (Exception e) {
            log.error("发送系统消息失败，但不影响主流程", e);
        }

        try {
            // 陪诊师自动发送问候消息
            ChatMessage greetingMsg = new ChatMessage();
            greetingMsg.setSenderId(attendantId);
            greetingMsg.setReceiverId(order.getUserId());
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

    private String generateQrCodeUrl(Integer orderId) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=SERVICE_CONFIRM_" + orderId;
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
        try {
            String message = String.format("{\"type\":\"SERVICE_STARTED\",\"orderId\":%d}", orderId);
            webSocketHandler.sendMessageToUser(String.valueOf(order.getUserId()), message);
        } catch (Exception e) {
            log.error("发送服务开始 WebSocket 消息失败", e);
        }

        // 插入系统消息（用户）
        String msgContent = "您的订单No." + order.getOrderNo() + "服务已开始。陪诊师已到达指定位置，请准备就诊。";
        sendSystemMessage(order.getUserId(), msgContent);

        // 插入系统消息（陪诊师）
        sendSystemMessage(order.getAttendantId(), "您已开始为订单 " + order.getOrderNo() + " 提供服务，请按时完成服务。");

        return "服务开始成功";
    }

    @Override
    @Transactional
    public String endService(Integer orderId, BigDecimal actualDuration) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null) return "订单不存在";
        if (order.getOrderStatus() != 3) return "订单当前状态无法结束服务";

        order.setOrderStatus(6);
        order.setServiceEndTime(new Date());
        order.setActualDuration(actualDuration);
        orderMapper.updateByPrimaryKeySelective(order);

        // 发送 WebSocket 消息
        try {
            String message = String.format("{\"type\":\"SERVICE_COMPLETED\",\"orderId\":%d}", orderId);
            webSocketHandler.sendMessageToUser(String.valueOf(order.getUserId()), message);
        } catch (Exception e) {
            log.error("发送服务结束 WebSocket 消息失败", e);
        }

        // 插入系统消息（用户）
        // 格式：您的陪诊服务(订单No.****8765)已完成，感谢您使用我们的服务!请对本次服务进行评价...
        String msgContent = "您的陪诊服务(订单No." + order.getOrderNo() + ")已完成，感谢您使用我们的服务!请对本次服务进行评价，帮助我们持续改进服务质量。";
        sendSystemMessage(order.getUserId(), msgContent);

        // 插入系统消息（陪诊师）
        sendSystemMessage(order.getAttendantId(), "您已完成订单 " + order.getOrderNo() + " 的服务，请提醒用户确认服务时长并评价。");

        return "服务结束成功";
    }

    // 辅助方法：发送系统消息
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
        } catch (Exception e) {
            log.error("发送系统消息失败", e);
        }
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
            res.setPaymentStatus(order.getPaymentStatus());
            res.setServiceTypeName(order.getServiceContent());
            res.setCreateTime(order.getCreateTime());

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