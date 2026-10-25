package com.ecommerce.service;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.entity.Sku;
import com.ecommerce.mapper.SkuMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 库存服务(基于 Redis + Lua 原子扣减, 支撑高并发)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StringRedisTemplate redisTemplate;
    private final SkuMapper skuMapper;

    @Value("${ecommerce.stock-key-prefix}")
    private String stockKeyPrefix;

    private DefaultRedisScript<Long> deductScript;

    @PostConstruct
    public void init() {
        deductScript = new DefaultRedisScript<>();
        deductScript.setResultType(Long.class);
        deductScript.setLocation(new ClassPathResource("lua/deduct_stock.lua"));
    }

    /**
     * 原子扣减库存, 返回剩余库存; 扣减失败抛出业务异常
     */
    public long deduct(Long skuId, long quantity) {
        String key = stockKeyPrefix + skuId;
        Long result = executeDeduct(key, quantity);
        long r = result == null ? -1L : result;
        if (r == -1L) {
            // key 不存在(冷启动) -> 从 DB 加载后重试一次
            initStock(skuId);
            result = executeDeduct(key, quantity);
            r = result == null ? -1L : result;
        }
        if (r < 0) {
            log.warn("库存扣减失败, skuId={}, qty={}, result={}", skuId, quantity, r);
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        return r;
    }

    private Long executeDeduct(String key, long quantity) {
        return redisTemplate.execute(deductScript, List.of(key), String.valueOf(quantity));
    }

    /**
     * 回补库存(订单取消/失败补偿)
     */
    public void restore(Long skuId, long quantity) {
        String key = stockKeyPrefix + skuId;
        Long current = redisTemplate.opsForValue().increment(key, quantity);
        if (current == null) {
            initStock(skuId);
            redisTemplate.opsForValue().increment(key, quantity);
        }
    }

    /**
     * 获取当前可用库存(Redis 缺失时回源 DB)
     */
    public long getStock(Long skuId) {
        String key = stockKeyPrefix + skuId;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            return Long.parseLong(value);
        }
        initStock(skuId);
        value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    /**
     * 从 DB 加载 SKU 库存到 Redis
     */
    public void initStock(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        redisTemplate.opsForValue().set(stockKeyPrefix + skuId, String.valueOf(sku.getStock()));
    }
}