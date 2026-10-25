package com.ecommerce.service.payment;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.Refund;
import org.springframework.stereotype.Service;

/**
 * 模拟退款(本地演示, 即时成功)
 */
@Service
public class MockRefundHandler implements RefundChannelHandler {

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public void refund(Order order, Payment payment, Refund refund) {
        // 模拟渠道: 无需真实调用, 交由 RefundService 标记成功
    }
}