package com.ecommerce.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.es.ProductDocument;
import com.ecommerce.es.ProductEsRepository;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SkuMapper;
import com.ecommerce.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动初始化: 预热库存到 Redis, 商品索引到 ES
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final StockService stockService;
    private final ProductEsRepository productEsRepository;

    @Override
    public void run(String... args) {
        try {
            List<Sku> skus = skuMapper.selectList(null);
            skus.forEach(sku -> stockService.initStock(sku.getId()));
            log.info("库存已预热到 Redis, 数量={}", skus.size());
        } catch (Exception e) {
            log.warn("库存预热失败(请确认 Redis 与数据库可用): {}", e.getMessage());
        }

        try {
            List<Product> products = productMapper.selectList(
                    Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1));
            products.forEach(p -> productEsRepository.save(ProductDocument.from(p)));
            log.info("商品已索引到 ES, 数量={}", products.size());
        } catch (Exception e) {
            log.warn("商品索引失败(请确认 ES 可用): {}", e.getMessage());
        }
    }
}