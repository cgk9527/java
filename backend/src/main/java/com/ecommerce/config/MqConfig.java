package com.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 队列/交换机/绑定声明
 */
@Configuration
public class MqConfig {

    /** 订单扣减库存队列 */
    public static final String ORDER_STOCK_QUEUE = "ecommerce.order.stock.queue";
    public static final String ORDER_STOCK_EXCHANGE = "ecommerce.order.stock.exchange";
    public static final String ORDER_STOCK_ROUTING_KEY = "ecommerce.order.stock";

    /** 商品同步 ES 队列 */
    public static final String PRODUCT_SYNC_QUEUE = "ecommerce.product.sync.queue";
    public static final String PRODUCT_SYNC_EXCHANGE = "ecommerce.product.sync.exchange";
    public static final String PRODUCT_SYNC_ROUTING_KEY = "ecommerce.product.sync";

    /** 消息使用 JSON 序列化 */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue orderStockQueue() {
        return new Queue(ORDER_STOCK_QUEUE, true);
    }

    @Bean
    public DirectExchange orderStockExchange() {
        return new DirectExchange(ORDER_STOCK_EXCHANGE, true, false);
    }

    @Bean
    public Binding orderStockBinding() {
        return BindingBuilder.bind(orderStockQueue()).to(orderStockExchange()).with(ORDER_STOCK_ROUTING_KEY);
    }

    @Bean
    public Queue productSyncQueue() {
        return new Queue(PRODUCT_SYNC_QUEUE, true);
    }

    @Bean
    public DirectExchange productSyncExchange() {
        return new DirectExchange(PRODUCT_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Binding productSyncBinding() {
        return BindingBuilder.bind(productSyncQueue()).to(productSyncExchange()).with(PRODUCT_SYNC_ROUTING_KEY);
    }
}