package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.common.util.UserContext;
import com.ecommerce.dto.PaymentFlowVO;
import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.dto.RefundVO;
import com.ecommerce.service.payment.PaymentService;
import com.ecommerce.service.payment.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping("/orders/{id}/pay")
    public Result<PaymentResultVO> pay(@PathVariable Long id,
                                       @RequestParam(name = "channel", required = false, defaultValue = "mock") String channel) {
        return Result.success(paymentService.pay(UserContext.getUserId(), id, channel));
    }

    /**
     * 支付宝异步通知回调(无需登录, 已在拦截器放行)
     */
    @PostMapping("/pay/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) ->
                params.put(key, (values != null && values.length > 0) ? values[0] : ""));
        return paymentService.handleAlipayNotify(params) ? "success" : "failure";
    }

    /**
     * 微信异步通知回调(无需登录, 已在拦截器放行), 返回 XML 应答
     */
    @PostMapping(value = "/pay/notify/wechat", produces = "application/xml;charset=UTF-8")
    public String wechatNotify(HttpServletRequest request) throws IOException {
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return paymentService.handleWechatNotify(body);
    }

    @GetMapping("/payments")
    public Result<List<PaymentFlowVO>> payments() {
        return Result.success(paymentService.listFlows(UserContext.getUserId()));
    }

    @PostMapping("/orders/{id}/refund")
    public Result<RefundVO> refund(@PathVariable Long id,
                                   @RequestParam(name = "reason", required = false) String reason) {
        return Result.success(refundService.refund(UserContext.getUserId(), id, reason));
    }
}