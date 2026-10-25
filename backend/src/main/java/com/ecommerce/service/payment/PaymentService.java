package com.ecommerce.service.payment;

import com.ecommerce.dto.PaymentResultVO;

import java.util.Map;

/**
 * 支付服务抽象
 */
public interface PaymentService {

    /**
     * 拉起支付
     */
    PaymentResultVO pay(Long userId, Long orderId);

    /**
     * 处理支付宝异步通知, 返回是否验签通过
     */
    boolean handleAlipayNotify(Map<String, String> params);
}