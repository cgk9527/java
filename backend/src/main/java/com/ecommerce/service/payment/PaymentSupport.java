package com.ecommerce.service.payment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付通用支持: 校验订单、落支付流水、标记已支付
 */
@Service
@RequiredArgsConstructor
public class PaymentSupport {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;

    public Order getPendingOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Order.STATUS_PENDING_PAY) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        return order;
    }

    /**
     * 发起支付时创建待支付流水(幂等)
     */
    @Transactional(rollbackFor = Exception.class)
    public void createPendingPayment(Order order, String channel, int payType) {
        Payment payment = findByOrderId(order.getId());
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getPayAmount());
            payment.setCreatedAt(LocalDateTime.now());
        }
        payment.setPayType(payType);
        payment.setChannel(channel);
        payment.setStatus(0);
        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }
    }

    /**
     * 标记订单已支付(幂等)
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Order order, String channel, String tradeNo, int payType) {
        if (order.getStatus() != Order.STATUS_PENDING_PAY) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(Order.STATUS_PAID);
        order.setPaidAt(now);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        Payment payment = findByOrderId(order.getId());
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getPayAmount());
            payment.setPayType(payType);
            payment.setCreatedAt(now);
        }
        payment.setChannel(channel);
        payment.setTradeNo(tradeNo);
        payment.setStatus(1);
        payment.setPayTime(now);
        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }
    }

    /**
     * 根据订单号标记已支付(用于支付宝异步通知)
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPaidByOrderNo(String orderNo, String channel, String tradeNo, int payType) {
        Order order = orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order != null) {
            markPaid(order, channel, tradeNo, payType);
        }
    }

    private Payment findByOrderId(Long orderId) {
        return paymentMapper.selectOne(
                Wrappers.<Payment>lambdaQuery().eq(Payment::getOrderId, orderId));
    }
}