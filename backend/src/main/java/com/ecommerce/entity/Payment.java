package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private Integer payType;

    /** 支付渠道: mock / alipay */
    private String channel;

    /** 第三方交易流水号(如支付宝 trade_no) */
    private String tradeNo;

    private BigDecimal amount;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime createdAt;
}