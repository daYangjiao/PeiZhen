package org.example.test;

import org.example.model.response.CompleteOrderInfoResponse;

public class TestCompilation {
    public static void main(String[] args) {
        CompleteOrderInfoResponse response = new CompleteOrderInfoResponse();
        response.setOrderNo("TEST123");
        response.setPaymentStatus(1);
        System.out.println("测试成功: " + response.getOrderNo());
    }
}