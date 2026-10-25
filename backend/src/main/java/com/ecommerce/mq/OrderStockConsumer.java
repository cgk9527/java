package com.ecommerce.mq;

import com.ecommerce.config.MqConfig;
import com.ecommerce.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 订单库存消费者: 异步落库扣减/回补库存, 并触发 ES 同步
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockConsumer {

    private final SkuMapper skuMapper;
    private final ProductSyncProducer productSyncProducer;

    @RabbitListener(queues = MqConfig.ORDER_STOCK_QUEUE)
    public void onMessage(OrderStockMessage message) {
        log.info("消费订单库存消息: orderId={}, operation={}", message.getOrderId(), message.getOperation());
        boolean deduct = OrderStockMessage.OPERATION_DEDUCT.equals(message.getOperation());

        Set<Long> productIds = new LinkedHashSet<>();
        for (OrderStockMessage.Item item : message.getItems()) {
            int affected = deduct
                    ? skuMapper.deductStock(item.getSkuId(), item.getQuantity())
                    : skuMapper.restoreStock(item.getSkuId(), item.getQuantity());
            if (affected == 0) {
                log.warn("库存变更未生效, skuId={}, qty={}, operation={}", item.getSkuId(), item.getQuantity(), message.getOperation());
            }
            productIds.add(item.getProductId());
        }

        // 异步刷新 ES 中的销量/库存
        productIds.forEach(productSyncProducer::send);
    }
}