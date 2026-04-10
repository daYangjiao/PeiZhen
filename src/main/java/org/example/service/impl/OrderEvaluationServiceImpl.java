package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.OrderEvaluationMapper;
import org.example.model.OrderEvaluation;
import org.example.service.OrderEvaluationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEvaluationServiceImpl implements OrderEvaluationService {

    private final OrderEvaluationMapper evaluationMapper;

    @Override
    public OrderEvaluation getByOrderId(Integer orderId) {
        if (orderId == null) return null;
        return evaluationMapper.selectByOrderId(orderId);
    }

    @Override
    @Transactional
    public void saveOrUpdateEvaluation(OrderEvaluation evaluation) {
        if (evaluation == null || evaluation.getOrderId() == null) {
            throw new IllegalArgumentException("评价信息不完整");
        }
        OrderEvaluation existing = evaluationMapper.selectByOrderId(evaluation.getOrderId());
        if (existing == null) {
            evaluationMapper.insert(evaluation);
        } else {
            // 保留已有主键与orderId，更新其他字段
            evaluation.setId(existing.getId());
            evaluationMapper.updateByOrderId(evaluation);
        }
    }

    @Override
    @Transactional
    public void replyToEvaluation(Integer orderId, String replyContent) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        OrderEvaluation existing = evaluationMapper.selectByOrderId(orderId);
        if (existing == null) {
            throw new IllegalArgumentException("该订单暂无评价，无法回复");
        }
        OrderEvaluation update = new OrderEvaluation();
        update.setOrderId(orderId);
        update.setAttendantReply(replyContent != null ? replyContent.trim() : null);
        update.setReplyTime(new java.util.Date());
        evaluationMapper.updateByOrderId(update);
    }
}

