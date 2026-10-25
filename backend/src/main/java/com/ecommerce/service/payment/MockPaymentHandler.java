package com.ecommerce.service.payment;

import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模拟支付(本地演示, 直接标记为已支付)
 */
@Service
@RequiredArgsConstructor
public class MockPaymentHandler implements PaymentChannelHandler {

    private final PaymentSupport paymentSupport;

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultVO pay(Long userId, Long orderId) {
        Order order = paymentSupport.getPendingOrder(userId, orderId);
        paymentSupport.markPaid(order, "mock", null, 1);
        return PaymentResultVO.builder()
                .channel("mock")
                .orderId(orderId)
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .build();
    }
}