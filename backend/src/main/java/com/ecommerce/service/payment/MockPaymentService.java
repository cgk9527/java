package com.ecommerce.service.payment;

import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 模拟支付(本地演示, 直接标记为已支付)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ecommerce.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentService implements PaymentService {

    private final PaymentSupport paymentSupport;

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

    @Override
    public boolean handleAlipayNotify(Map<String, String> params) {
        return false;
    }
}