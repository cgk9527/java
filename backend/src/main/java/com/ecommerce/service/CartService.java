package com.ecommerce.service;

import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.CartItemVO;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务(基于 Redis Hash, 支持高并发)
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate redisTemplate;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final StockService stockService;

    @Value("${ecommerce.cart-key-prefix}")
    private String cartKeyPrefix;

    private String key(Long userId) {
        return cartKeyPrefix + userId;
    }

    public void add(Long userId, Long skuId, int quantity) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        long stock = stockService.getStock(skuId);
        Long newQty = redisTemplate.opsForHash().increment(key(userId), String.valueOf(skuId), quantity);
        if (newQty == null) {
            newQty = (long) quantity;
        }
        if (stock <= 0 || newQty > stock) {
            redisTemplate.opsForHash().put(key(userId), String.valueOf(skuId), String.valueOf(stock));
        }
    }

    public List<CartItemVO> list(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        List<CartItemVO> result = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            Long skuId = Long.parseLong(String.valueOf(entry.getKey()));
            int quantity = Integer.parseInt(String.valueOf(entry.getValue()));
            Sku sku = skuMapper.selectById(skuId);
            if (sku == null) {
                continue;
            }
            Product product = productMapper.selectById(sku.getProductId());
            if (product == null) {
                continue;
            }
            CartItemVO vo = new CartItemVO();
            vo.setSkuId(skuId);
            vo.setProductId(product.getId());
            vo.setProductName(product.getName());
            vo.setProductImage(product.getMainImage());
            vo.setSpecs(sku.getSpecs());
            vo.setPrice(sku.getPrice());
            vo.setQuantity(quantity);
            vo.setStock(stockService.getStock(skuId));
            result.add(vo);
        }
        return result;
    }

    public void update(Long userId, Long skuId, int quantity) {
        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(key(userId), String.valueOf(skuId));
            return;
        }
        long stock = stockService.getStock(skuId);
        int finalQty = (int) Math.min(quantity, stock);
        redisTemplate.opsForHash().put(key(userId), String.valueOf(skuId), String.valueOf(finalQty));
    }

    public void delete(Long userId, Long skuId) {
        redisTemplate.opsForHash().delete(key(userId), String.valueOf(skuId));
    }

    public void clear(Long userId) {
        redisTemplate.delete(key(userId));
    }

    public long count(Long userId) {
        Long size = redisTemplate.opsForHash().size(key(userId));
        return size == null ? 0 : size;
    }
}