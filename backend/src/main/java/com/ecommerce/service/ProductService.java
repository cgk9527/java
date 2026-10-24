package com.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.PageResult;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.ProductVO;
import com.ecommerce.dto.SkuVO;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.es.ProductDocument;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SkuMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 商品服务(详情 Redis 缓存, 搜索 ES)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final SearchService searchService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ecommerce.product-cache-key}")
    private String productCacheKey;

    public PageResult<ProductDocument> search(String keyword, Long categoryId, int page, int size, String sort) {
        return searchService.search(keyword, categoryId, page, size, sort);
    }

    public List<ProductDocument> hot(int limit) {
        return searchService.hot(limit);
    }

    public ProductVO getDetail(Long id) {
        String key = productCacheKey + id;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ProductVO.class);
            } catch (Exception e) {
                log.warn("反序列化商品缓存失败, id={}", id, e);
            }
        }

        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        ProductVO vo = toVO(product, skuMapper.selectList(
                Wrappers.<Sku>lambdaQuery().eq(Sku::getProductId, id).eq(Sku::getStatus, 1)));

        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("写入商品缓存失败, id={}", id, e);
        }
        return vo;
    }

    public ProductVO toVO(Product product, List<Sku> skus) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setMainImage(product.getMainImage());
        vo.setSubImages(product.getSubImages());
        vo.setDetail(product.getDetail());
        vo.setPrice(product.getPrice());
        vo.setSales(product.getSales());
        vo.setStatus(product.getStatus());
        vo.setSkus(skus.stream().map(sku -> {
            SkuVO s = new SkuVO();
            s.setId(sku.getId());
            s.setProductId(sku.getProductId());
            s.setSkuCode(sku.getSkuCode());
            s.setSpecs(sku.getSpecs());
            s.setPrice(sku.getPrice());
            s.setStock(sku.getStock());
            s.setSales(sku.getSales());
            return s;
        }).toList());
        return vo;
    }
}