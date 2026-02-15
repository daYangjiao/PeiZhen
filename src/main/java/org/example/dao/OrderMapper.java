package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.Order;
import org.example.model.request.OrderListQueryRequest;

import java.util.List;

@Mapper
public interface OrderMapper {
    // 插入订单
    int insert(Order order);

    // 根据用户ID查询订单
    List<Order> findOrdersByUserId(Integer userId);

    // 查询所有订单
    List<Order> findAllOrders();

    // 删除订单
    int deleteOrder(Integer orderId);

    // 插入预约订单
    int insertAppointmentOrder(Order order);

    // 更新订单评价
    int updateOrderEvaluation(Order order);

    // 更新陪诊师星级
    int updateAttendantStarRating(Order order);

    // 获取所有就诊类型
    List<Integer> getAllClinicTypes();

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
}