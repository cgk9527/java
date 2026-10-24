package com.ecommerce.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单库存变更消息(deduct=扣减 / restore=回补)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStockMessage implements Serializable {

    public static final String OPERATION_DEDUCT = "deduct";
    public static final String OPERATION_RESTORE = "restore";

    private String operation;
    private Long orderId;
    private Long userId;
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item implements Serializable {
        private Long skuId;
        private Long productId;
        private Integer quantity;
    }
}