package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付结果(拉起支付所需的返回值)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultVO {

    /** 支付渠道: mock / alipay */
    private String channel;

    private Long orderId;

    private String orderNo;

    /** 支付宝返回的支付表单 HTML(仅 alipay 渠道) */
    private String body;

    /** 订单当前状态 */
    private Integer status;
}