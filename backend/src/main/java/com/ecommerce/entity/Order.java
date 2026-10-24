package com.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@TableName("orders")
public class Order {

    /** 待支付 */
    public static final int STATUS_PENDING_PAY = 0;
    /** 已支付 */
    public static final int STATUS_PAID = 1;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 2;
    /** 已关闭 */
    public static final int STATUS_CLOSED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer status;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime updatedAt;
}