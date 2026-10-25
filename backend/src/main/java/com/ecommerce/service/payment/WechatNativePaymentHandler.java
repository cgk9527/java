package com.ecommerce.service.payment;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.util.HttpUtil;
import com.ecommerce.common.util.WechatSignUtil;
import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 微信 Native 扫码支付(统一下单接口, 返回 code_url 二维码内容)
 */
@Service
@RequiredArgsConstructor
public class WechatNativePaymentHandler implements PaymentChannelHandler {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final PaymentSupport paymentSupport;
    private final WechatProperties props;

    @Override
    public String channel() {
        return "wechat";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultVO pay(Long userId, Long orderId) {
        Order order = paymentSupport.getPendingOrder(userId, orderId);
        paymentSupport.createPendingPayment(order, "wechat", 3);
        String codeUrl = unifiedOrder(order);
        return PaymentResultVO.builder()
                .channel("wechat")
                .orderId(orderId)
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .qrCode(codeUrl)
                .build();
    }

    private String unifiedOrder(Order order) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", props.getAppId());
        params.put("mch_id", props.getMchId());
        params.put("nonce_str", nonceStr());
        params.put("body", "电商平台订单");
        params.put("out_trade_no", order.getOrderNo());
        params.put("total_fee", totalFee(order.getPayAmount()));
        params.put("spbill_create_ip", props.getSpbillCreateIp());
        params.put("notify_url", props.getNotifyUrl());
        params.put("trade_type", "NATIVE");
        params.put("sign_type", "MD5");

        params.put("sign", WechatSignUtil.sign(params, props.getApiKey()));
        String xml = WechatSignUtil.buildXml(params);
        String response = HttpUtil.post(props.getUnifiedOrderUrl(), xml, "text/xml;charset=utf-8");

        Map<String, String> resp = WechatSignUtil.parseXml(response);
        boolean success = "SUCCESS".equals(resp.get("return_code")) && "SUCCESS".equals(resp.get("result_code"));
        if (!success) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(),
                    "微信下单失败: " + resp.get("return_msg") + resp.get("err_code_des"));
        }
        return resp.get("code_url");
    }

    private String totalFee(BigDecimal amount) {
        return String.valueOf(amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue());
    }

    private String nonceStr() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}