package com.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {

    private Long id;
    private Long productId;
    private String skuCode;
    private String specs;
    private BigDecimal price;
    private Long stock;
    private Long sales;
}