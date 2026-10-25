package com.ecommerce.mq;

import com.ecommerce.config.MqConfig;
import com.ecommerce.entity.Product;
import com.ecommerce.es.ProductDocument;
import com.ecommerce.es.ProductEsRepository;
import com.ecommerce.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 商品同步 ES 消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSyncConsumer {

    private final ProductMapper productMapper;
    private final ProductEsRepository productEsRepository;

    @RabbitListener(queues = MqConfig.PRODUCT_SYNC_QUEUE)
    public void onMessage(ProductSyncMessage message) {
        Product product = productMapper.selectById(message.getProductId());
        if (product == null) {
            log.warn("同步 ES 失败, 商品不存在: productId={}", message.getProductId());
            return;
        }
        productEsRepository.save(ProductDocument.from(product));
        log.info("商品已同步到 ES: productId={}", product.getId());
    }
}