package com.ecommerce.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品同步 ES 消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncMessage implements Serializable {

    private Long productId;
}