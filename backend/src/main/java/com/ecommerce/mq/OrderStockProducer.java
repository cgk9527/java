package com.ecommerce.mq;

import com.ecommerce.config.MqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单库存消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(OrderStockMessage message) {
        rabbitTemplate.convertAndSend(
                MqConfig.ORDER_STOCK_EXCHANGE,
                MqConfig.ORDER_STOCK_ROUTING_KEY,
                message);
        log.info("发送订单库存消息: orderId={}, operation={}", message.getOrderId(), message.getOperation());
    }
}