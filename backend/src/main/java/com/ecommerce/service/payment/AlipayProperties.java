package com.ecommerce.service.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付配置
 */
@Getter
@Component
public class AlipayProperties {

    @Value("${ecommerce.payment.alipay.gateway}")
    private String gateway;

    @Value("${ecommerce.payment.alipay.app-id}")
    private String appId;

    @Value("${ecommerce.payment.alipay.private-key}")
    private String privateKey;

    @Value("${ecommerce.payment.alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${ecommerce.payment.alipay.notify-url}")
    private String notifyUrl;

    @Value("${ecommerce.payment.alipay.return-url}")
    private String returnUrl;

    @Value("${ecommerce.payment.alipay.sign-type:RSA2}")
    private String signType;

    @Value("${ecommerce.payment.alipay.charset:utf-8}")
    private String charset;
}