package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.OrderEvaluation;

@Mapper
public interface OrderEvaluationMapper {

    OrderEvaluation selectByOrderId(@Param("orderId") Integer orderId);

    int insert(OrderEvaluation evaluation);

    int updateByOrderId(OrderEvaluation evaluation);

    /**
     * 统计陪诊师评价总数
     */
    Integer countByAttendantId(@Param("attendantId") Integer attendantId);

    /**
     * 统计陪诊师好评数（rating >= minRating）
     */
    Integer countGoodByAttendantId(@Param("attendantId") Integer attendantId, @Param("minRating") Integer minRating);
}
