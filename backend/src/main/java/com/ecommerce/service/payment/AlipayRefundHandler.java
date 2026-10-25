package com.ecommerce.service.payment;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.util.AlipaySignUtil;
import com.ecommerce.common.util.HttpUtil;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.Refund;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝退款(alipay.trade.refund)
 */
@Service
@RequiredArgsConstructor
public class AlipayRefundHandler implements RefundChannelHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlipayProperties props;
    private final ObjectMapper objectMapper;

    @Override
    public String channel() {
        return "alipay";
    }

    @Override
    public boolean refund(Order order, Payment payment, Refund refund) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", props.getAppId());
        params.put("method", "alipay.trade.refund");
        params.put("format", "json");
        params.put("charset", props.getCharset());
        params.put("sign_type", props.getSignType());
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        params.put("version", "1.0");
        params.put("biz_content", bizContent(order, refund));

        String sign = AlipaySignUtil.sign(params, props.getPrivateKey(), props.getCharset());
        String response = HttpUtil.post(props.getGateway(),
                toFormBody(params, sign, props.getCharset()),
                "application/x-www-form-urlencoded;charset=" + props.getCharset());

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode resp = root.path("alipay_trade_refund_response");
            if (!"10000".equals(resp.path("code").asText())) {
                String subMsg = resp.path("sub_msg").asText();
                throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "支付宝退款失败: " + subMsg);
            }
            // fund_change: Y=退款已到账(同步成功), N=退款处理中(等待异步回执)
            return "Y".equals(resp.path("fund_change").asText());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.SYSTEM_ERROR);
        }
    }

    private String bizContent(Order order, Refund refund) {
        String refundAmount = refund.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return "{\"out_trade_no\":\"" + order.getOrderNo() + "\","
                + "\"refund_amount\":\"" + refundAmount + "\","
                + "\"out_request_no\":\"" + refund.getRefundNo() + "\"}";
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