package com.ecommerce.event;

import com.ecommerce.mq.OrderStockMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 订单库存变更事件(在事务提交后发送 MQ)
 */
@Getter
public class OrderStockChangedEvent extends ApplicationEvent {

    private final OrderStockMessage message;

    public OrderStockChangedEvent(Object source, OrderStockMessage message) {
        super(source);
        this.message = message;
    }
}