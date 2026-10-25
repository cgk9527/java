package com.ecommerce.service.payment;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.Refund;
import org.springframework.stereotype.Service;

/**
 * 微信退款占位实现(需商户 SSL 证书, 当前未接入)
 */
@Service
public class WechatRefundHandler implements RefundChannelHandler {

    @Override
    public String channel() {
        return "wechat";
    }

    @Override
    public void refund(Order order, Payment payment, Refund refund) {
        throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "微信退款需配置商户证书, 当前暂未接入");
    }
}