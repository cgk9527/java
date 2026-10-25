package com.ecommerce.service.payment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.RefundVO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.Refund;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.PaymentMapper;
import com.ecommerce.mapper.RefundMapper;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款服务(仅已支付订单可退款, 成功后退款并回补库存)
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private final List<RefundChannelHandler> handlers;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final OrderService orderService;

    @Transactional(rollbackFor = Exception.class)
    public RefundVO refund(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Order.STATUS_PAID) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }

        Payment payment = paymentMapper.selectOne(
                Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, orderId));
        String channel = (payment != null && payment.getChannel() != null) ? payment.getChannel() : "mock";

        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setAmount(order.getPayAmount());
        refund.setChannel(channel);
        refund.setReason(reason);
        refund.setStatus(Refund.STATUS_PROCESSING);
        refund.setCreatedAt(LocalDateTime.now());
        refundMapper.insert(refund);

        RefundChannelHandler handler = handlers.stream()
                .filter(h -> h.channel().equals(channel))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.PARAM_ERROR.getCode(), "不支持的退款渠道: " + channel));

        try {
            handler.refund(order, payment, refund);
        } catch (BizException e) {
            refund.setStatus(Refund.STATUS_FAILED);
            refundMapper.updateById(refund);
            throw e;
        }

        refund.setStatus(Refund.STATUS_SUCCESS);
        refund.setRefundedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        order.setStatus(Order.STATUS_REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 回补库存(Redis + MQ 异步落库)
        orderService.restoreStock(orderId);

        return toVO(refund);
    }

    private RefundVO toVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setRefundNo(refund.getRefundNo());
        vo.setAmount(refund.getAmount());
        vo.setChannel(refund.getChannel());
        vo.setReason(refund.getReason());
        vo.setStatus(refund.getStatus());
        vo.setCreatedAt(refund.getCreatedAt());
        vo.setRefundedAt(refund.getRefundedAt());
        return vo;
    }

    private String generateRefundNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "R" + timestamp + random;
    }
}