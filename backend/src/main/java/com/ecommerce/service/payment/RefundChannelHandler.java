package com.ecommerce.service.payment;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.Refund;

/**
 * 退款渠道处理器(按 channel 注册)
 */
public interface RefundChannelHandler {

    String channel();

    /**
     * 执行渠道退款, 失败时抛出业务异常
     *
     * @return true=同步退款成功; false=异步处理中(等待退款结果回执)
     */
    boolean refund(Order order, Payment payment, Refund refund);
}