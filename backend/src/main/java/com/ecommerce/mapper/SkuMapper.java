package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 原子扣减库存(数据库层面, 幂等由 MQ 消费者保证)
     */
    @Update("UPDATE sku SET stock = stock - #{qty}, sales = sales + #{qty}, " +
            "version = version + 1, updated_at = now() " +
            "WHERE id = #{skuId} AND stock >= #{qty}")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /**
     * 回补库存(订单取消场景)
     */
    @Update("UPDATE sku SET stock = stock + #{qty}, sales = GREATEST(sales - #{qty}, 0), " +
            "version = version + 1, updated_at = now() WHERE id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("qty") int qty);
}