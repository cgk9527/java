package com.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {

    private Long skuId;
    private Long productId;
    private String productName;
    private String productImage;
    private String specs;
    private BigDecimal price;
    private Integer quantity;
    /** 当前可用库存(用于前端展示/校验) */
    private Long stock;
}