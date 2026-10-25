package com.ecommerce.service.payment;

import com.ecommerce.dto.PaymentResultVO;

/**
 * 支付渠道处理器(按 channel 字符串注册)
 */
public interface PaymentChannelHandler {

    String channel();

    PaymentResultVO pay(Long userId, Long orderId);
}