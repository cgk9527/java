package com.ecommerce.mq;

import com.ecommerce.config.MqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息生产者(投递到延迟队列)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(Long orderId) {
        rabbitTemplate.convertAndSend(
                MqConfig.ORDER_DELAY_EXCHANGE,
                MqConfig.ORDER_DELAY_ROUTING_KEY,
                new OrderTimeoutMessage(orderId));
        log.info("发送订单超时延迟消息: orderId={}", orderId);
    }
}