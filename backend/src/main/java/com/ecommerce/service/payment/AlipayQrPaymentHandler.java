package com.ecommerce.service.payment;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.util.AlipaySignUtil;
import com.ecommerce.common.util.HttpUtil;
import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.entity.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝扫码支付(alipay.trade.precreate 当面付预下单, 返回二维码内容)
 */
@Service
@RequiredArgsConstructor
public class AlipayQrPaymentHandler implements PaymentChannelHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaymentSupport paymentSupport;
    private final AlipayProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public String channel() {
        return "alipay_qr";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultVO pay(Long userId, Long orderId) {
        Order order = paymentSupport.getPendingOrder(userId, orderId);
        paymentSupport.createPendingPayment(order, "alipay", 2);
        String qrCode = precreate(order);
        return PaymentResultVO.builder()
                .channel("alipay_qr")
                .orderId(orderId)
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .qrCode(qrCode)
                .build();
    }

    private String precreate(Order order) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", props.getAppId());
        params.put("method", "alipay.trade.precreate");
        params.put("format", "json");
        params.put("charset", props.getCharset());
        params.put("sign_type", props.getSignType());
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        params.put("version", "1.0");
        params.put("notify_url", props.getNotifyUrl());
        params.put("biz_content", bizContent(order));

        String sign = AlipaySignUtil.sign(params, props.getPrivateKey(), props.getCharset());
        String formBody = toFormBody(params, sign, props.getCharset());
        String response = HttpUtil.post(props.getGateway(), formBody,
                "application/x-www-form-urlencoded;charset=" + props.getCharset());

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode resp = root.path("alipay_trade_precreate_response");
            if (!"10000".equals(resp.path("code").asText())) {
                throw new BizException(ResultCode.SYSTEM_ERROR.getCode(),
                        "支付宝下单失败: " + resp.path("sub_msg").asText());
            }
            return resp.path("qr_code").asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR);
        }
    }

    private String bizContent(Order order) {
        String totalAmount = order.getPayAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return "{\"out_trade_no\":\"" + order.getOrderNo() + "\","
                + "\"total_amount\":\"" + totalAmount + "\","
                + "\"subject\":\"电商平台订单-" + order.getOrderNo() + "\"}";
    }

    private String toFormBody(Map<String, String> params, String sign, String charset) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(urlEncode(entry.getKey(), charset))
                    .append('=')
                    .append(urlEncode(entry.getValue(), charset))
                    .append('&');
        }
        sb.append("sign=").append(urlEncode(sign, charset));
        return sb.toString();
    }

    private String urlEncode(String value, String charset) {
        return URLEncoder.encode(value, charset == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(charset));
    }
}