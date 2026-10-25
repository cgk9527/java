package com.ecommerce.service.payment;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.util.AlipaySignUtil;
import com.ecommerce.common.util.WechatSignUtil;
import com.ecommerce.dto.PaymentResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 支付服务门面: 按渠道分发到对应的处理器, 并处理各渠道异步通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final List<PaymentChannelHandler> handlers;
    private final AlipayProperties alipayProperties;
    private final WechatProperties wechatProperties;
    private final PaymentSupport paymentSupport;

    public PaymentResultVO pay(Long userId, Long orderId, String channel) {
        PaymentChannelHandler handler = handlers.stream()
                .filter(h -> h.channel().equals(channel))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.PARAM_ERROR.getCode(), "不支持的支付方式: " + channel));
        return handler.pay(userId, orderId);
    }

    /**
     * 支付宝异步通知(电脑网站与扫码共用)
     */
    public boolean handleAlipayNotify(Map<String, String> params) {
        boolean verified = AlipaySignUtil.verify(params, alipayProperties.getAlipayPublicKey(), alipayProperties.getCharset());
        if (!verified) {
            log.warn("支付宝异步通知验签失败");
            return false;
        }
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            paymentSupport.markPaidByOrderNo(params.get("out_trade_no"), "alipay", params.get("trade_no"), 2);
        }
        return true;
    }

    /**
     * 微信异步通知(XML), 返回要给微信的 XML 应答
     */
    public String handleWechatNotify(String xmlBody) {
        Map<String, String> params = WechatSignUtil.parseXml(xmlBody);
        boolean tradeSuccess = "SUCCESS".equals(params.get("return_code"))
                && "SUCCESS".equals(params.get("result_code"));
        if (!tradeSuccess || !WechatSignUtil.verify(params, wechatProperties.getApiKey())) {
            log.warn("微信异步通知验签失败");
            return WechatSignUtil.buildXml(Map.of("return_code", "FAIL", "return_msg", "签名校验失败"));
        }
        paymentSupport.markPaidByOrderNo(params.get("out_trade_no"), "wechat", params.get("transaction_id"), 3);
        return WechatSignUtil.buildXml(Map.of("return_code", "SUCCESS", "return_msg", "OK"));
    }
}