package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录
 */
@Data
@TableName("refund")
public class Refund {

    /** 处理中 */
    public static final int STATUS_PROCESSING = 0;
    /** 成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 失败 */
    public static final int STATUS_FAILED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    private String channel;

    private String reason;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime refundedAt;
}