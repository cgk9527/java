package com.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款信息视图
 */
@Data
public class RefundVO {

    private String refundNo;

    private BigDecimal amount;

    private String channel;

    private String reason;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime refundedAt;
}