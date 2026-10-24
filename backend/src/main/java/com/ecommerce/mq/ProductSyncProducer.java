package com.ecommerce.mq;

import com.ecommerce.config.MqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 商品同步 ES 消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(Long productId) {
        rabbitTemplate.convertAndSend(
                MqConfig.PRODUCT_SYNC_EXCHANGE,
                MqConfig.PRODUCT_SYNC_ROUTING_KEY,
                new ProductSyncMessage(productId));
    }
}