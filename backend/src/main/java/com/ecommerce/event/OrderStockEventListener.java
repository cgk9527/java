package com.ecommerce.event;

import com.ecommerce.mq.OrderStockProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单事件监听: 事务提交后投递 MQ, 保证消息与数据库一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStockEventListener {

    private final OrderStockProducer orderStockProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onOrderStockChanged(OrderStockChangedEvent event) {
        orderStockProducer.send(event.getMessage());
    }
}