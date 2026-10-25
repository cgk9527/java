package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.common.util.UserContext;
import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{id}/pay")
    public Result<PaymentResultVO> pay(@PathVariable Long id) {
        return Result.success(paymentService.pay(UserContext.getUserId(), id));
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
}