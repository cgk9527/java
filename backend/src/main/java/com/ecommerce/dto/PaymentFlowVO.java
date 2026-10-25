package com.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水视图(含退款信息)
 */
@Data
public class PaymentFlowVO {

    private Long paymentId;

    private Long orderId;

    private String orderNo;

    private BigDecimal amount;

    /** 支付渠道: mock / alipay / alipay_qr / wechat */
    private String channel;

    /** 支付状态: 0-待支付 1-成功 2-失败 */
    private Integer status;

    private String tradeNo;

    private LocalDateTime payTime;

    private LocalDateTime createdAt;

    private RefundVO refund;
}