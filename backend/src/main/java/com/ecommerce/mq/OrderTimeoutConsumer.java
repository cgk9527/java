package com.ecommerce.mq;

import com.ecommerce.config.MqConfig;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单超时消费者: TTL 到期后判定并自动关闭未支付订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = MqConfig.ORDER_TIMEOUT_QUEUE)
    public void onMessage(OrderTimeoutMessage message) {
        log.info("处理订单超时消息: orderId={}", message.getOrderId());
        orderService.autoCancel(message.getOrderId());
    }
}