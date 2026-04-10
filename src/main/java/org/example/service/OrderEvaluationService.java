package org.example.service;

import org.example.model.OrderEvaluation;

public interface OrderEvaluationService {

    /**
     * 根据订单ID查询评价
     */
    OrderEvaluation getByOrderId(Integer orderId);

    /**
     * 创建或更新订单评价
     */
    void saveOrUpdateEvaluation(OrderEvaluation evaluation);

    /**
     * 陪诊师回复评价
     * @param orderId 订单ID
     * @param replyContent 回复内容
     */
    void replyToEvaluation(Integer orderId, String replyContent);
}

