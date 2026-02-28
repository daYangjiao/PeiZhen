package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.OrderEvaluation;

@Mapper
public interface OrderEvaluationMapper {

    OrderEvaluation selectByOrderId(@Param("orderId") Integer orderId);

    int insert(OrderEvaluation evaluation);

    int updateByOrderId(OrderEvaluation evaluation);
}

