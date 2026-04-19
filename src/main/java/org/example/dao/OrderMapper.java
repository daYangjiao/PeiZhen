package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.Order;
import org.example.model.request.OrderListQueryRequest;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderMapper {
    // 插入订单
    int insert(Order order);

    // 选择性更新订单
    int updateByPrimaryKeySelective(Order order);

    // 根据ID查询订单
    Order selectByPrimaryKey(Integer id);

    // 根据订单号查询订单（已存在，无需修改）
    Order selectByOrderNo(String orderNo);

    // ===== 新增：分页查询相关方法 =====
    
    /**
     * 统计用户订单数量
     */
    int countUserOrders(@Param("userId") Integer userId, @Param("query") OrderListQueryRequest queryRequest);

    /**
     * 分页查询用户订单
     */
    List<Order> findUserOrdersWithPagination(
        @Param("userId") Integer userId, 
        @Param("query") OrderListQueryRequest queryRequest,
        @Param("offset") int offset, 
        @Param("limit") int limit
    );

    /**
     * 统计所有订单数量
     */
    int countAllOrders(@Param("query") OrderListQueryRequest queryRequest);

    /**
     * 分页查询所有订单
     */
    List<Order> findAllOrdersWithPagination(
        @Param("query") OrderListQueryRequest queryRequest,
        @Param("offset") int offset, 
        @Param("limit") int limit
    );

    /**
     * 根据状态查询订单
     */
    List<Order> findOrdersByStatus(@Param("status") Integer status);

    /**
     * 将订单释放回接单大厅：状态改为待接单，清空陪诊师与二维码，记录取消原因与时间（供用户端展示）
     */
    int releaseOrderBackToHall(@Param("orderId") Integer orderId, @Param("reason") String reason, @Param("cancelTime") java.util.Date cancelTime);

    /**
     * 查询超过支付时限仍未支付的订单（用于系统自动取消）
     */
    List<Order> findExpiredUnpaidOrders(@Param("deadline") java.util.Date deadline);

    /**
     * 查询超过专属派单确认时限的订单
     */
    List<Order> findExpiredAssignedOrders(@Param("deadline") java.util.Date deadline);

    /**
     * 查询服务开始后仍未匹配成功的已支付订单
     */
    List<Order> findTimedOutUnmatchedOrders(@Param("now") java.util.Date now);

    /**
     * 将未匹配成功且已超时的订单关闭
     */
    int closeOrderAsTimeout(@Param("orderId") Integer orderId,
                            @Param("reason") String reason,
                            @Param("cancelTime") java.util.Date cancelTime,
                            @Param("refundAmount") BigDecimal refundAmount);

    /**
     * 统计陪诊师今日已完成服务次数（按 service_date）
     */
    Integer countTodayCompletedService(@Param("attendantId") Integer attendantId);

    /**
     * 统计陪诊师本月已完成服务次数（按 service_date）
     */
    Integer countMonthCompletedService(@Param("attendantId") Integer attendantId);

    /**
     * 统计陪诊师累计收入（已完成订单）
     */
    java.math.BigDecimal sumCompletedIncome(@Param("attendantId") Integer attendantId);

    /**
     * 统计陪诊师总订单数（已接单）
     */
    Integer countTotalOrdersByAttendant(@Param("attendantId") Integer attendantId);

    /**
     * 统计陪诊师已完成订单数
     */
    Integer countCompletedOrdersByAttendant(@Param("attendantId") Integer attendantId);
}
