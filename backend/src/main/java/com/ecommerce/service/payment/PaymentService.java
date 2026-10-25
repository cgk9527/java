package com.ecommerce.service.payment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.util.AlipaySignUtil;
import com.ecommerce.common.util.WechatSignUtil;
import com.ecommerce.dto.PaymentFlowVO;
import com.ecommerce.dto.PaymentResultVO;
import com.ecommerce.dto.RefundVO;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.Refund;
import com.ecommerce.mapper.PaymentMapper;
import com.ecommerce.mapper.RefundMapper;
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
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final RefundService refundService;

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
        // 退款结果回执(通知含 refund_fee 字段)
        String refundFee = params.get("refund_fee");
        if (refundFee != null && !refundFee.isEmpty()) {
            refundService.handleAsyncRefundResult(params.get("out_trade_no"), true);
            return true;
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

    /**
     * 查询当前用户支付流水(含退款信息)
     */
    public List<PaymentFlowVO> listFlows(Long userId) {
        List<Payment> payments = paymentMapper.selectList(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getUserId, userId)
                .orderByDesc(Payment::getCreatedAt));
        return payments.stream().map(payment -> {
            PaymentFlowVO vo = new PaymentFlowVO();
            vo.setPaymentId(payment.getId());
            vo.setOrderId(payment.getOrderId());
            vo.setOrderNo(payment.getOrderNo());
            vo.setAmount(payment.getAmount());
            vo.setChannel(payment.getChannel());
            vo.setStatus(payment.getStatus());
            vo.setTradeNo(payment.getTradeNo());
            vo.setPayTime(payment.getPayTime());
            vo.setCreatedAt(payment.getCreatedAt());

            Refund refund = refundMapper.selectOne(
                    Wrappers.<Refund>lambdaQuery().eq(Refund::getOrderId, payment.getOrderId()));
            if (refund != null) {
                RefundVO rv = new RefundVO();
                rv.setRefundNo(refund.getRefundNo());
                rv.setAmount(refund.getAmount());
                rv.setChannel(refund.getChannel());
                rv.setReason(refund.getReason());
                rv.setStatus(refund.getStatus());
                rv.setCreatedAt(refund.getCreatedAt());
                rv.setRefundedAt(refund.getRefundedAt());
                vo.setRefund(rv);
            }
            return vo;
        }).toList();
    }
}