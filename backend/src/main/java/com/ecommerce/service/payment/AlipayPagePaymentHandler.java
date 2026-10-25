package com.ecommerce.service.payment;

import com.ecommerce.common.util.AlipaySignUtil;
import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝电脑网站支付(alipay.trade.page.pay, 返回自动提交表单)
 */
@Service
@RequiredArgsConstructor
public class AlipayPagePaymentHandler implements PaymentChannelHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaymentSupport paymentSupport;
    private final AlipayProperties props;

    @Override
    public String channel() {
        return "alipay";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResultVO pay(Long userId, Long orderId) {
        Order order = paymentSupport.getPendingOrder(userId, orderId);
        paymentSupport.createPendingPayment(order, "alipay", 2);
        String form = buildPagePayForm(order);
        return PaymentResultVO.builder()
                .channel("alipay")
                .orderId(orderId)
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .body(form)
                .build();
    }

    private String buildPagePayForm(Order order) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", props.getAppId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "json");
        params.put("charset", props.getCharset());
        params.put("sign_type", props.getSignType());
        params.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        params.put("version", "1.0");
        params.put("notify_url", props.getNotifyUrl());
        params.put("return_url", props.getReturnUrl());
        params.put("biz_content", bizContent(order, true));

        String sign = AlipaySignUtil.sign(params, props.getPrivateKey(), props.getCharset());

        StringBuilder form = new StringBuilder();
        form.append("<form name=\"punchout_form\" method=\"post\" action=\"")
                .append(props.getGateway())
                .append("?charset=").append(props.getCharset())
                .append("\">\n");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            form.append("<input type=\"hidden\" name=\"").append(entry.getKey())
                    .append("\" value=\"").append(escapeHtml(entry.getValue())).append("\" />\n");
        }
        form.append("<input type=\"hidden\" name=\"sign\" value=\"")
                .append(escapeHtml(sign)).append("\" />\n");
        form.append("<script>document.forms[0].submit();</script>\n");
        form.append("</form>");
        return form.toString();
    }

    private String bizContent(Order order, boolean withProductCode) {
        String totalAmount = order.getPayAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"out_trade_no\":\"").append(order.getOrderNo()).append("\",");
        sb.append("\"total_amount\":\"").append(totalAmount).append("\",");
        sb.append("\"subject\":\"电商平台订单-").append(order.getOrderNo()).append("\"");
        if (withProductCode) {
            sb.append(",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"");
        }
        sb.append('}');
        return sb.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}