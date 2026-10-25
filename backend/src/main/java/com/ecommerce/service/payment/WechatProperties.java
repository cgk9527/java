package com.ecommerce.service.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置
 */
@Getter
@Component
public class WechatProperties {

    @Value("${ecommerce.payment.wechat.app-id}")
    private String appId;

    @Value("${ecommerce.payment.wechat.mch-id}")
    private String mchId;

    @Value("${ecommerce.payment.wechat.api-key}")
    private String apiKey;

    @Value("${ecommerce.payment.wechat.notify-url}")
    private String notifyUrl;

    @Value("${ecommerce.payment.wechat.unifiedorder-url:https://api.mch.weixin.qq.com/pay/unifiedorder}")
    private String unifiedOrderUrl;

    @Value("${ecommerce.payment.wechat.spbill-create-ip:127.0.0.1}")
    private String spbillCreateIp;
}