package com.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
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

    /** 订单超时关闭(延迟队列 -> 死信队列) */
    public static final String ORDER_DELAY_QUEUE = "ecommerce.order.timeout.delay.queue";
    public static final String ORDER_DELAY_EXCHANGE = "ecommerce.order.timeout.delay.exchange";
    public static final String ORDER_DELAY_ROUTING_KEY = "ecommerce.order.timeout.delay";
    public static final String ORDER_TIMEOUT_QUEUE = "ecommerce.order.timeout.queue";
    public static final String ORDER_TIMEOUT_EXCHANGE = "ecommerce.order.timeout.exchange";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "ecommerce.order.timeout";

    @Value("${ecommerce.order.timeout-ms:1800000}")
    private long orderTimeoutMs;

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

    /**
     * 延迟队列: 消息 TTL 到期后转发到死信交换机, 由超时队列消费
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .withArgument("x-message-ttl", orderTimeoutMs)
                .withArgument("x-dead-letter-exchange", ORDER_TIMEOUT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue(ORDER_TIMEOUT_QUEUE, true);
    }

    @Bean
    public DirectExchange orderTimeoutExchange() {
        return new DirectExchange(ORDER_TIMEOUT_EXCHANGE, true, false);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderTimeoutExchange()).with(ORDER_TIMEOUT_ROUTING_KEY);
    }
}